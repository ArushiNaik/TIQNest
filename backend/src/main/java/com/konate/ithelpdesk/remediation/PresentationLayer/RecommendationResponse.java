package com.konate.ithelpdesk.remediation.PresentationLayer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.konate.ithelpdesk.remediation.BusinessLayer.IssueType;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationResponse(
        Long ticketId,
        String ticketTitle,
        IssueType detectedIssue,
        boolean approvedRemedyAvailable,
        String message,
        List<RemedyResponse> recommendations,
        String scopeClassification,
        String clarifyingQuestion
) {
    public RecommendationResponse(Long ticketId, String ticketTitle, IssueType detectedIssue,
                                  boolean approvedRemedyAvailable, String message, List<RemedyResponse> recommendations) {
        this(ticketId, ticketTitle, detectedIssue, approvedRemedyAvailable, message, recommendations, null, null);
    }
}
