package com.konate.ithelpdesk.audit.PresentationLayer;

import com.konate.ithelpdesk.audit.BusinessLayer.AuditService;
import com.konate.ithelpdesk.ticket.BusinessLayer.TicketService;
import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.config.AccessControl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuditController {

    private final AuditService auditService;
    private final TicketService ticketService;

    @GetMapping("/{id}/history")
    public List<AuditEventResponse> history(@PathVariable Long id,
                                            @RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        ticketService.findByIdFor(id, user); // 401/403/404: only the requester or an admin
        return auditService.history(id);
    }
}
