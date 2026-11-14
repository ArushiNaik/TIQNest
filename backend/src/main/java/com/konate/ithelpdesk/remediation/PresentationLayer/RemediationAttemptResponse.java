package com.konate.ithelpdesk.remediation.PresentationLayer;

import com.konate.ithelpdesk.remediation.DataLayer.RemediationOutcome;

import java.time.Instant;

public record RemediationAttemptResponse(
        Long id,
        String remedyId,
        String remedyTitle,
        Long ticketId,
        RemediationOutcome outcome,
        String feedback,
        String reportedBy,
        Instant createdAt
) {}
