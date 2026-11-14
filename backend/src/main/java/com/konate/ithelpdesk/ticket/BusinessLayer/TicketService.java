package com.konate.ithelpdesk.ticket.BusinessLayer;

import com.konate.ithelpdesk.incident.BusinessLayer.OutageRadarService;
import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import com.konate.ithelpdesk.ticket.DataTier.TicketStatus;
import com.konate.ithelpdesk.ticket.PresentationLayer.CreateTicketRequest;
import com.konate.ithelpdesk.ticket.PresentationLayer.TicketResponse;
import com.konate.ithelpdesk.ticket.Mapper.TicketMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final OutageRadarService outageRadarService;

    @Value("${outage-radar.window-minutes:5}")
    private long windowMinutes;

    @Transactional
    public TicketResponse create(CreateTicketRequest req) {
        Ticket ticket = ticketMapper.toEntity(req);
        // same normalization as @PrePersist, so the duplicate check compares like with like
        String system = req.affectedSystem().trim().toUpperCase();
        Instant since = Instant.now().minus(Duration.ofMinutes(windowMinutes));

        // Anti-flood 1: same person, same problem, recently -> refuse
        if (ticketRepository.existsByRequesterEmailIgnoreCaseAndCategoryAndAffectedSystemAndCreatedAtAfter(
                req.requesterEmail(), req.category(), system, since)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already reported a " + req.category() + " problem with " + system
                            + " in the last " + windowMinutes + " minutes. We're on it.");
        }

        Ticket saved = ticketRepository.save(ticket);
        outageRadarService.scan(saved);
        return ticketMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> findAll() {
        return ticketMapper.toResponseList(ticketRepository.findAllByOrderByCreatedAtDesc());
    }

    // Anti-flood 2: the agent queue hides tickets that belong to an outage
    @Transactional(readOnly = true)
    public List<TicketResponse> findQueue() {
        return ticketMapper.toResponseList(ticketRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)));
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        return ticketRepository.findById(id)
                .map(ticketMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ticket " + id + " not found"));
    }
}