package com.konate.ithelpdesk.remediation.PresentationLayer;

import com.konate.ithelpdesk.remediation.DataLayer.RemediationOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** The user's own report of how a remedy went. */
public record RemediationAttemptRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[a-z0-9-]+$", message = "must be an approved remedy ID")
        String remedyId,
        @Positive Long ticketId,
        @NotNull RemediationOutcome outcome,
        @Size(max = 1000) String feedback
) {}
