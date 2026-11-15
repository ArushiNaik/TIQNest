package com.konate.ithelpdesk.triage.PresentationLayer;

import com.konate.ithelpdesk.triage.DataLayer.AdminReviewStatus;

import java.time.Instant;

public record AdminReviewResponse(
        Long id,
        String requesterEmail,
        String submittedText,
        String classification,
        AdminReviewStatus status,
        Instant createdAt,
        Instant closedAt,
        String closedBy
) {
}
