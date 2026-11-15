package com.konate.ithelpdesk.ticket.PresentationLayer;

import com.konate.ithelpdesk.ticket.BusinessLayer.TicketService;
import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.config.AccessControl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request,
                                 @RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        return ticketService.create(request, user);
    }

    @GetMapping
    public List<TicketResponse> findAll(@RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        return ticketService.findAllFor(user);
    }

    @GetMapping("/queue")
    public List<TicketResponse> queue(@RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        AccessControl.requireAdmin(user);
        return ticketService.findQueue();
    }

    @GetMapping("/{id}")
    public TicketResponse findById(@PathVariable Long id,
                                   @RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        return ticketService.findByIdFor(id, user);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request,
                                 @RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        return ticketService.update(id, request, user);
    }
}
