package com.konate.ithelpdesk.incident.BusinessLayer;

import com.konate.ithelpdesk.incident.DataLayer.Incident;
import com.konate.ithelpdesk.incident.DataLayer.IncidentRepository;
import com.konate.ithelpdesk.incident.DataLayer.IncidentStatus;
import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import com.konate.ithelpdesk.ticket.DataTier.TicketStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutageRadarService {

    private final TicketRepository ticketRepository;
    private final IncidentRepository incidentRepository;

    @Value("${outage-radar.threshold:3}")
    private int threshold;

    @Value("${outage-radar.window-minutes:5}")
    private long windowMinutes;

    @Transactional
    public void scan(Ticket ticket) {

        var existing = incidentRepository.findFirstByCategoryAndAffectedSystemAndStatus(
                ticket.getCategory(), ticket.getAffectedSystem(), IncidentStatus.ACTIVE);

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
                .findByCategoryAndAffectedSystemAndCreatedAtAfterOrderByCreatedAtAsc(
                        ticket.getCategory(), ticket.getAffectedSystem(), since)
                .stream()
                .filter(t -> t.getIncident() == null)
                .toList();

        if (cluster.size() < threshold) {
            log.debug("Radar: {} {} -> {}/{} similar tickets, no incident yet",
                    ticket.getCategory(), ticket.getAffectedSystem(), cluster.size(), threshold);
            return;
        }

        Incident incident = incidentRepository.save(Incident.builder()
                .title("Possible outage: " + ticket.getAffectedSystem()
                        + " (" + ticket.getCategory() + ")")
                .category(ticket.getCategory())
                .affectedSystem(ticket.getAffectedSystem())
                .ticketCount(cluster.size())
                .build());

        cluster.forEach(t -> link(t, incident));

        log.warn("Radar: OUTAGE DETECTED -> incident {} '{}' from {} tickets",
                incident.getId(), incident.getTitle(), cluster.size());
    }

    private void link(Ticket t, Incident incident) {
        t.setIncident(incident);
        t.setStatus(TicketStatus.LINKED_TO_INCIDENT);
    }
}