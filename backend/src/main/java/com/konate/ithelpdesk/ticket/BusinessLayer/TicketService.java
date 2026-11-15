package com.konate.ithelpdesk.ticket.BusinessLayer;

import com.konate.ithelpdesk.audit.BusinessLayer.AuditService;
import com.konate.ithelpdesk.audit.DataLayer.AuditAction;
import com.konate.ithelpdesk.config.NonITIssueException;
import com.konate.ithelpdesk.incident.BusinessLayer.OutageRadarService;
import com.konate.ithelpdesk.sla.SlaPolicy;
import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketPriority;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import com.konate.ithelpdesk.ticket.DataTier.TicketStatus;
import com.konate.ithelpdesk.ticket.Mapper.TicketMapper;
import com.konate.ithelpdesk.ticket.PresentationLayer.CreateTicketRequest;
import com.konate.ithelpdesk.ticket.PresentationLayer.TicketResponse;
import com.konate.ithelpdesk.ticket.PresentationLayer.UpdateTicketRequest;
import com.konate.ithelpdesk.triage.BusinessLayer.AiScopeClassifier;
import com.konate.ithelpdesk.triage.BusinessLayer.ScopeClassification;
import com.konate.ithelpdesk.triage.BusinessLayer.ScopeResult;
import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.config.AccessControl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final OutageRadarService outageRadarService;
    private final SlaPolicy slaPolicy;
    private final AuditService auditService;
    private final AiScopeClassifier scopeClassifier;

    @Value("${outage-radar.window-minutes:5}")
    private long windowMinutes;

    /**
     * Creates a ticket for the signed-in user. Employees always file tickets as themselves and
     * pass the IT-scope gate (non-IT submissions are rejected and never reach the outage radar).
     * Admins may file on behalf of someone else (used by the outage simulator).
     */
    @Transactional
    public TicketResponse create(CreateTicketRequest req, User currentUser) {
        AccessControl.requireUser(currentUser);
        boolean admin = AccessControl.isAdmin(currentUser);
        String requester = admin && req.requesterEmail() != null && !req.requesterEmail().isBlank()
                ? req.requesterEmail().trim() : currentUser.getEmail();

        TicketPriority priority = req.priority();
        if (!admin) {
            ScopeResult scope = scopeClassifier.classifyForGate(
                    req.title() + " " + (req.description() == null ? "" : req.description()));
            if (scope.classification() == ScopeClassification.NON_IT) throw new NonITIssueException();
            if (scope.classification() == ScopeClassification.SECURITY_RISK
                    && (priority == null || priority.ordinal() < TicketPriority.HIGH.ordinal())) {
                priority = TicketPriority.HIGH;
            }
        }
        return create(new CreateTicketRequest(req.title(), req.description(), req.category(),
                req.affectedSystem(), requester, priority));
    }

    /** Internal creation path (already authorized). */
    @Transactional
    public TicketResponse create(CreateTicketRequest req) {
        Ticket ticket = ticketMapper.toEntity(req);
        String system = req.affectedSystem().trim().toUpperCase();
        Instant since = Instant.now().minus(Duration.ofMinutes(windowMinutes));

        if (ticketRepository.existsByRequesterEmailIgnoreCaseAndCategoryAndAffectedSystemAndCreatedAtAfter(
                req.requesterEmail(), req.category(), system, since)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already reported a " + req.category() + " problem with " + system
                            + " in the last " + windowMinutes + " minutes. We're on it.");
        }

        if (ticket.getPriority() == null) ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setSlaDeadline(slaPolicy.deadlineFor(ticket.getPriority(), Instant.now()));
        Ticket saved = ticketRepository.save(ticket);

        auditService.record(saved.getId(), AuditAction.CREATED, null, saved.getStatus(),
                saved.getRequesterEmail(),
                "Ticket created with priority " + saved.getPriority()
                        + ", SLA deadline " + saved.getSlaDeadline());

        outageRadarService.scan(saved);
        return ticketMapper.toResponse(saved);
    }

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest req, User currentUser) {
        AccessControl.requireAdmin(currentUser);
        Ticket ticket = getTicket(id);
        String actor = currentUser.getEmail();

        if (req.status() != null && req.status() != ticket.getStatus()) {
            if (req.status() == TicketStatus.LINKED_TO_INCIDENT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "LINKED_TO_INCIDENT is set automatically by the outage radar");
            }
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(req.status());
            if (req.status() == TicketStatus.RESOLVED || req.status() == TicketStatus.CLOSED) {
                if (ticket.getResolvedAt() == null) ticket.setResolvedAt(Instant.now());
            } else {
                ticket.setResolvedAt(null);
            }
            auditService.record(id, AuditAction.STATUS_CHANGED, oldStatus, req.status(), actor,
                    "Status changed from " + oldStatus + " to " + req.status());
        }

        if (req.priority() != null && req.priority() != ticket.getPriority()) {
            TicketPriority oldPriority = ticket.getPriority();
            ticket.setPriority(req.priority());
            ticket.setSlaDeadline(slaPolicy.deadlineFor(req.priority(), ticket.getCreatedAt()));
            auditService.record(id, AuditAction.PRIORITY_CHANGED, oldPriority, req.priority(), actor,
                    "Priority changed from " + oldPriority + " to " + req.priority()
                            + ", new SLA deadline " + ticket.getSlaDeadline());
        }

        if (req.assignee() != null) {
            String newAssignee = req.assignee().isBlank() ? null : req.assignee().trim();
            if (!Objects.equals(newAssignee, ticket.getAssignee())) {
                String oldAssignee = ticket.getAssignee();
                ticket.setAssignee(newAssignee);
                auditService.record(id, AuditAction.ASSIGNED, oldAssignee, newAssignee, actor,
                        newAssignee == null ? "Unassigned" : "Assigned to " + newAssignee);
            }
        }

        return ticketMapper.toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> findAll() {
        return ticketMapper.toResponseList(ticketRepository.findAllByOrderByCreatedAtDesc());
    }

    /** Admins see every ticket; employees only their own. */
    @Transactional(readOnly = true)
    public List<TicketResponse> findAllFor(User currentUser) {
        AccessControl.requireUser(currentUser);
        if (AccessControl.isAdmin(currentUser)) return findAll();
        return ticketMapper.toResponseList(
                ticketRepository.findByRequesterEmailIgnoreCaseOrderByCreatedAtDesc(currentUser.getEmail()));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> findQueue() {
        return ticketMapper.toResponseList(ticketRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)));
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        return ticketMapper.toResponse(getTicket(id));
    }

    /** 404 if the ticket does not exist, 403 if it belongs to someone else (admins see all). */
    @Transactional(readOnly = true)
    public TicketResponse findByIdFor(Long id, User currentUser) {
        AccessControl.requireUser(currentUser);
        Ticket ticket = getTicket(id);
        AccessControl.requireOwnerOrAdmin(currentUser, ticket.getRequesterEmail());
        return ticketMapper.toResponse(ticket);
    }

    private Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ticket " + id + " not found"));
    }
}
