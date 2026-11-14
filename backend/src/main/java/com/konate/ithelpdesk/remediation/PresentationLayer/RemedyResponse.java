package com.konate.ithelpdesk.remediation.PresentationLayer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.konate.ithelpdesk.remediation.BusinessLayer.IssueType;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemedyActionType;
import com.konate.ithelpdesk.remediation.BusinessLayer.SafetyLevel;

import java.util.List;

/** An approved remedy. {@code matchReason} and {@code confidence} are only set in recommendations. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RemedyResponse(
        String remedyId,
        String title,
        String explanation,
        IssueType issueType,
        String supportedPlatform,
        SafetyLevel safetyLevel,
        RemedyActionType actionType,
        String deepLink,
        String command,
        List<RemedyActionResponse> actions,
        List<String> steps,
        String expectedResult,
        List<String> cautions,
        boolean userConfirmationRequired,
        String matchReason,
        Integer confidence
) {}
