package com.konate.ithelpdesk.triage.PresentationLayer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code confirmed} must be true: the employee explicitly asked for a human review. */
public record AdminReviewCreateRequest(
        @NotBlank @Size(max = 1000) String text,
        @Size(max = 32) String classification,
        boolean confirmed
) {
}
