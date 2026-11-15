package com.konate.ithelpdesk.triage.BusinessLayer;

import com.konate.ithelpdesk.config.CodedApiException;
import com.konate.ithelpdesk.triage.DataLayer.AdminReviewRepository;
import com.konate.ithelpdesk.triage.DataLayer.AdminReviewRequest;
import com.konate.ithelpdesk.triage.DataLayer.AdminReviewStatus;
import com.konate.ithelpdesk.triage.PresentationLayer.AdminReviewCreateRequest;
import com.konate.ithelpdesk.triage.PresentationLayer.AdminReviewResponse;
import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.config.AccessControl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AdminReviewService {

    static final int MAX_OPEN_PER_USER = 3;

    private final AdminReviewRepository repository;

    public AdminReviewService(AdminReviewRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AdminReviewResponse create(AdminReviewCreateRequest req, User user) {
        AccessControl.requireUser(user);
        if (!req.confirmed()) {
            throw new CodedApiException(HttpStatus.BAD_REQUEST, "CONFIRMATION_REQUIRED",
                    "Please confirm that you want an IT administrator to review this request.");
        }
        String text = AiScopeClassifier.clean(req.text(), 1000);
        if (text == null) {
            throw new CodedApiException(HttpStatus.BAD_REQUEST, "EMPTY_REVIEW", "There is nothing to review.");
        }
        if (repository.existsByRequesterEmailIgnoreCaseAndSubmittedTextIgnoreCaseAndCreatedAtAfter(
                user.getEmail(), text, Instant.now().minus(Duration.ofHours(24)))) {
            throw new CodedApiException(HttpStatus.CONFLICT, "DUPLICATE_REVIEW",
                    "You already asked IT to review this request today.");
        }
        if (repository.countByRequesterEmailIgnoreCaseAndStatus(user.getEmail(), AdminReviewStatus.OPEN)
                >= MAX_OPEN_PER_USER) {
            throw new CodedApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REVIEWS",
                    "You already have " + MAX_OPEN_PER_USER + " review requests waiting. Please wait for IT to respond.");
        }
        String classification = AiScopeClassifier.parseClassification(req.classification()) == null
                ? null : req.classification().trim().toUpperCase(Locale.ROOT);
        return toResponse(repository.save(AdminReviewRequest.builder()
                .requesterEmail(user.getEmail())
                .submittedText(text)
                .classification(classification)
                .build()));
    }

    @Transactional(readOnly = true)
    public List<AdminReviewResponse> list(User user) {
        AccessControl.requireUser(user);
        List<AdminReviewRequest> rows = AccessControl.isAdmin(user)
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByRequesterEmailIgnoreCaseOrderByCreatedAtDesc(user.getEmail());
        return rows.stream().map(AdminReviewService::toResponse).toList();
    }

    @Transactional
    public AdminReviewResponse close(Long id, User user) {
        AccessControl.requireAdmin(user);
        AdminReviewRequest review = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review request " + id + " not found"));
        if (review.getStatus() != AdminReviewStatus.CLOSED) {
            review.setStatus(AdminReviewStatus.CLOSED);
            review.setClosedAt(Instant.now());
            review.setClosedBy(user.getEmail());
        }
        return toResponse(review);
    }

    private static AdminReviewResponse toResponse(AdminReviewRequest r) {
        return new AdminReviewResponse(r.getId(), r.getRequesterEmail(), r.getSubmittedText(), r.getClassification(),
                r.getStatus(), r.getCreatedAt(), r.getClosedAt(), r.getClosedBy());
    }
}
