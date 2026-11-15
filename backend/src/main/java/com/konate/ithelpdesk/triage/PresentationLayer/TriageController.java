package com.konate.ithelpdesk.triage.PresentationLayer;

import com.konate.ithelpdesk.triage.BusinessLayer.AdminReviewService;
import com.konate.ithelpdesk.triage.BusinessLayer.TriageService;
import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.config.AccessControl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** TiqVision triage API. Every route requires a signed-in user (AuthInterceptor). */
@RestController
@RequestMapping("/api/triage")
@CrossOrigin(origins = "http://localhost:5173")
public class TriageController {

    private final TriageService triageService;
    private final AdminReviewService adminReviewService;

    public TriageController(TriageService triageService, AdminReviewService adminReviewService) {
        this.triageService = triageService;
        this.adminReviewService = adminReviewService;
    }

    @GetMapping("/status")
    public Map<String, Boolean> status(@RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        AccessControl.requireUser(user);
        return Map.of("aiConfigured", triageService.aiConfigured(),
                "demoFixturesEnabled", triageService.demoFixturesEnabled());
    }

    @PostMapping("/classify")
    public ClassifyResponse classify(@Valid @RequestBody ClassifyRequest request,
                                     @RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        return triageService.classify(request.description(), user);
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TriageAnalysisResponse analyze(@RequestPart(name = "files", required = false) List<MultipartFile> files,
                                          @RequestParam(name = "description", required = false) String description,
                                          @RequestParam(name = "demo", required = false) String demo,
                                          @RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        return triageService.analyze(files, description, demo, user);
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminReviewResponse requestReview(@Valid @RequestBody AdminReviewCreateRequest request,
                                             @RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        return adminReviewService.create(request, user);
    }

    @GetMapping("/reviews")
    public List<AdminReviewResponse> reviews(@RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        return adminReviewService.list(user);
    }

    @PatchMapping("/reviews/{id}/close")
    public AdminReviewResponse closeReview(@PathVariable Long id,
                                           @RequestAttribute(name = AccessControl.CURRENT_USER, required = false) User user) {
        return adminReviewService.close(id, user);
    }
}
