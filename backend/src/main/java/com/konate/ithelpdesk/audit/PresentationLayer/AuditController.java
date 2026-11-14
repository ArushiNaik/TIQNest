package com.konate.ithelpdesk.audit.PresentationLayer;

import com.konate.ithelpdesk.audit.BusinessLayer.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/{id}/history")
    public List<AuditEventResponse> history(@PathVariable Long id) {
        return auditService.history(id);
    }
}
