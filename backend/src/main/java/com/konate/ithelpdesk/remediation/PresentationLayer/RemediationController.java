package com.konate.ithelpdesk.remediation.PresentationLayer;

import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationService;
import com.konate.ithelpdesk.user.DataLayer.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/remediations")
@CrossOrigin(origins = "http://localhost:5173")
public class RemediationController {

    private final RemediationService remediationService;

    public RemediationController(RemediationService remediationService) {
        this.remediationService = remediationService;
    }

    @PostMapping("/recommend")
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request,
                                            @RequestAttribute(name = "currentUser", required = false) User currentUser) {
        return remediationService.recommend(request, currentUser);
    }

    @GetMapping("/catalog")
    public List<RemedyResponse> catalog() {
        return remediationService.catalog();
    }

    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public RemediationAttemptResponse recordAttempt(
            @Valid @RequestBody RemediationAttemptRequest request,
            @RequestAttribute(name = "currentUser", required = false) User currentUser) {
        return remediationService.recordAttempt(request, currentUser);
    }

    @GetMapping("/attempts")
    public List<RemediationAttemptResponse> history(
            @RequestParam(required = false) Long ticketId,
            @RequestAttribute(name = "currentUser", required = false) User currentUser) {
        if (ticketId != null && ticketId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketId must be a positive number");
        }
        return remediationService.history(ticketId, currentUser);
    }
}
