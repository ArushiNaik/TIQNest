package com.konate.ithelpdesk.ticket.BusinessLayer;

import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import com.konate.ithelpdesk.ticket.PresentationLayer.CreateTicketRequest;
import com.konate.ithelpdesk.ticket.PresentationLayer.TicketResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    @Transactional
    public TicketResponse create(CreateTicketRequest req) {
        Ticket ticket = Ticket.builder()
                .title(req.title())
                .description(req.description())
                .category(req.category())
                .affectedSystem(req.affectedSystem())
                .requesterEmail(req.requesterEmail())
                .build();
        Ticket saved = ticketRepository.save(ticket);
        return TicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> findAll() {
        return ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        return ticketRepository.findById(id)
                .map(TicketResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ticket " + id + " not found"));
    }
}