package com.konate.ithelpdesk.incident.BusinessLayer;

import com.konate.ithelpdesk.audit.BusinessLayer.AuditService;
import com.konate.ithelpdesk.audit.DataLayer.AuditAction;
import com.konate.ithelpdesk.incident.DataLayer.Incident;
import com.konate.ithelpdesk.incident.DataLayer.IncidentRepository;
import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import com.konate.ithelpdesk.ticket.DataTier.TicketStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TiqRadar: groups reports from distinct employees about the same system into one incident.
 * <ul>
 *   <li>Similarity = same category + same normalized affected system (see {@link AffectedSystemNormalizer}).</li>
 *   <li>Threshold counts distinct requesters inside the time window, not raw tickets.</li>
 *   <li>Generic systems ("laptop", "unknown") are never grouped.</li>
 *   <li>Scans for the same outage key run one at a time until the surrounding transaction ends, and
 *       the unique {@code active_key} column rejects a second active incident at the database level.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutageRadarService {

    private final TicketRepository ticketRepository;
    private final IncidentRepository incidentRepository;
    private final AuditService auditService;
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Value("${outage-radar.threshold:3}")
    private int threshold;

    @Value("${outage-radar.window-minutes:5}")
    private long windowMinutes;

    public static String radarKey(TicketCategory category, String affectedSystem) {
        return category + "|" + AffectedSystemNormalizer.key(affectedSystem);
    }

    @Transactional
    public void scan(Ticket ticket) {
        if (ticket.getCategory() == null || AffectedSystemNormalizer.isGeneric(ticket.getAffectedSystem())) {
            log.debug("Radar: ticket {} has a generic system, not grouped", ticket.getId());
            return;
        }
        String key = radarKey(ticket.getCategory(), ticket.getAffectedSystem());
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        boolean releaseLater = registerUnlockAfterTransaction(lock);
        try {
            scanLocked(ticket, key);
        } finally {
            if (!releaseLater) lock.unlock();
        }
    }

    private void scanLocked(Ticket ticket, String key) {
        var existing = incidentRepository.findByActiveKey(key);
        if (existing.isPresent()) {
            Incident incident = existing.get();
            link(ticket, incident);
            incident.setTicketCount(incident.getTicketCount() + 1);
            incident.setLastReportAt(Instant.now());
            log.info("Radar: ticket {} joined incident {} ({} tickets)",
                    ticket.getId(), incident.getId(), incident.getTicketCount());
            return;
        }

        Instant since = Instant.now().minus(Duration.ofMinutes(windowMinutes));
        List<Ticket> cluster = ticketRepository
                .findByCategoryAndCreatedAtAfterOrderByCreatedAtAsc(ticket.getCategory(), since)
                .stream()
                .filter(t -> t.getIncident() == null)
                .filter(t -> key.equals(radarKey(t.getCategory(), t.getAffectedSystem())))
                .toList();
        long distinctReporters = cluster.stream()
                .map(t -> t.getRequesterEmail().toLowerCase(Locale.ROOT))
                .distinct()
                .count();

        if (distinctReporters < threshold) {
            log.debug("Radar: {} -> {}/{} distinct reporters, no incident yet", key, distinctReporters, threshold);
            return;
        }

        Incident incident = incidentRepository.save(Incident.builder()
                .title("Possible outage: " + ticket.getAffectedSystem() + " (" + ticket.getCategory() + ")")
                .category(ticket.getCategory())
                .affectedSystem(ticket.getAffectedSystem())
                .ticketCount(cluster.size())
                .activeKey(key)
                .build());

        cluster.forEach(t -> link(t, incident));

        log.warn("Radar: OUTAGE DETECTED -> incident {} '{}' from {} reporters ({} tickets)",
                incident.getId(), incident.getTitle(), distinctReporters, cluster.size());
    }

    /** Keeps the lock until commit/rollback so the next scan sees this transaction's tickets. */
    private static boolean registerUnlockAfterTransaction(ReentrantLock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return false;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                lock.unlock();
            }
        });
        return true;
    }

    private void link(Ticket t, Incident incident) {
        TicketStatus oldStatus = t.getStatus();
        t.setIncident(incident);
        t.setStatus(TicketStatus.LINKED_TO_INCIDENT);
        auditService.record(t.getId(), AuditAction.LINKED_TO_INCIDENT, oldStatus,
                TicketStatus.LINKED_TO_INCIDENT, AuditService.SYSTEM,
                "Grouped into incident #" + incident.getId() + ": " + incident.getTitle());
    }
}
