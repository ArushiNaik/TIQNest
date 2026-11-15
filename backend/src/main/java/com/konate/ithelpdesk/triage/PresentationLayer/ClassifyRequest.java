package com.konate.ithelpdesk.triage.PresentationLayer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassifyRequest(@NotBlank @Size(max = 2000) String description) {
}
