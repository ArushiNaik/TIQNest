package com.konate.ithelpdesk.incident.PresentationLayer;

import com.konate.ithelpdesk.incident.BusinessLayer.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping("/active")
    public List<IncidentResponse> active() {
        return incidentService.findActive();
    }

    @GetMapping
    public List<IncidentResponse> all() {
        return incidentService.findAll();
    }

    @PatchMapping("/{id}/resolve")
    public IncidentResponse resolve(@PathVariable Long id) {
        return incidentService.resolve(id);
    }
}