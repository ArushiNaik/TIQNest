package com.konate.ithelpdesk.remediation.PresentationLayer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.konate.ithelpdesk.remediation.BusinessLayer.IssueType;

import java.util.List;

/** Recommendations, best first. When nothing approved matches, the only entry is the escalation remedy. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationResponse(
        Long ticketId,
        String ticketTitle,
        IssueType detectedIssue,
        boolean approvedRemedyAvailable,
        String message,
        List<RemedyResponse> recommendations
) {}
