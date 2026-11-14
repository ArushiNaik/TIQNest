package com.konate.ithelpdesk.audit.BusinessLayer;

import com.konate.ithelpdesk.audit.DataLayer.AuditAction;
import com.konate.ithelpdesk.audit.DataLayer.AuditEvent;
import com.konate.ithelpdesk.audit.DataLayer.AuditEventRepository;
import com.konate.ithelpdesk.audit.Mapper.AuditEventMapper;
import com.konate.ithelpdesk.audit.PresentationLayer.AuditEventResponse;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    public static final String SYSTEM = "system";

    private final AuditEventRepository auditEventRepository;
    private final TicketRepository ticketRepository;
    private final AuditEventMapper auditEventMapper;

    @Transactional
    public void record(Long ticketId, AuditAction action, Object oldValue, Object newValue,
                       String actor, String message) {
        auditEventRepository.save(AuditEvent.builder()
                .ticketId(ticketId)
                .action(action)
                .oldValue(oldValue == null ? null : oldValue.toString())
                .newValue(newValue == null ? null : newValue.toString())
                .actor(actor == null || actor.isBlank() ? SYSTEM : actor)
                .message(message)
                .build());
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> history(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket " + ticketId + " not found");
        }
        return auditEventMapper.toResponseList(
                auditEventRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId));
    }
}
