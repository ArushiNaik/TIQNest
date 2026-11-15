package com.konate.ithelpdesk.sla;

import com.konate.ithelpdesk.ticket.PresentationLayer.TicketResponse;
import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.config.AccessControl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sla")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class SlaController {

    private final SlaService slaService;

    @GetMapping("/summary")
    public SlaSummaryResponse summary(@RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        AccessControl.requireAdmin(user);
        return slaService.summary();
    }

    @GetMapping("/alerts")
    public List<TicketResponse> alerts(@RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        AccessControl.requireAdmin(user);
        return slaService.alerts();
    }
}
