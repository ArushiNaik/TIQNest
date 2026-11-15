package com.konate.ithelpdesk.triage.PresentationLayer;

import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import com.konate.ithelpdesk.ticket.DataTier.TicketPriority;
import com.konate.ithelpdesk.triage.BusinessLayer.NextStep;
import com.konate.ithelpdesk.triage.BusinessLayer.ScopeClassification;

import java.util.List;

/**
 * A proposed ticket built from validated AI output. Nothing is persisted: the employee reviews and
 * edits it, then submits it through POST /api/tickets, which re-validates every field.
 * {@code confidence} is the model's own estimate, not a calibrated probability.
 */
public record TriageAnalysisResponse(
        ScopeClassification classification,
        boolean isITRelated,
        double confidence,
        String confidenceLabel,
        String reason,
        String title,
        String description,
        TicketCategory category,
        String affectedSystem,
        String visibleErrorCode,
        String possibleCause,
        List<String> evidence,
        List<String> suggestedRemedyIds,
        NextStep recommendedNextStep,
        boolean requiresUserReview,
        TicketPriority suggestedPriority,
        String clarifyingQuestion,
        boolean promptInjectionDetected,
        String source,
        boolean demo,
        int imageCount
) {
}
