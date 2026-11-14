package com.konate.ithelpdesk.sla;

import com.konate.ithelpdesk.ticket.PresentationLayer.TicketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
    public SlaSummaryResponse summary() {
        return slaService.summary();
    }

    @GetMapping("/alerts")
    public List<TicketResponse> alerts() {
        return slaService.alerts();
    }
}
