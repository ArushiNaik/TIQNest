package com.konate.ithelpdesk.incident.PresentationLayer;

import com.konate.ithelpdesk.incident.BusinessLayer.IncidentService;
import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.config.AccessControl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    /** Banner data for every signed-in user: system-level information only, no ticket details. */
    @GetMapping("/active")
    public List<IncidentResponse> active() {
        return incidentService.findActive();
    }

    @GetMapping
    public List<IncidentResponse> all(@RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        AccessControl.requireAdmin(user);
        return incidentService.findAll();
    }

    @PatchMapping("/{id}/resolve")
    public IncidentResponse resolve(@PathVariable Long id,
                                    @RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        AccessControl.requireAdmin(user);
        return incidentService.resolve(id, user.getEmail());
    }
}