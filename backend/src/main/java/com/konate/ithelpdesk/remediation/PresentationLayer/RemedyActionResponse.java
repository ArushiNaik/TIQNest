package com.konate.ithelpdesk.remediation.PresentationLayer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemedyActionType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RemedyActionResponse(
        String id,
        RemedyActionType type,
        String label,
        /** ms-settings deep link or command text, copied verbatim from the approved catalog. */
        String value,
        String shell,
        String explanation,
        String effects
) {}
