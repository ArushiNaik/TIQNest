package com.konate.ithelpdesk.triage.PresentationLayer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.konate.ithelpdesk.remediation.BusinessLayer.IssueType;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import com.konate.ithelpdesk.ticket.DataTier.TicketPriority;
import com.konate.ithelpdesk.triage.BusinessLayer.NextStep;
import com.konate.ithelpdesk.triage.BusinessLayer.ScopeClassification;
import com.konate.ithelpdesk.triage.BusinessLayer.ScopeSource;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassifyResponse(
        ScopeClassification classification,
        boolean isITRelated,
        double confidence,
        String reason,
        String clarifyingQuestion,
        IssueType issueType,
        TicketCategory category,
        ScopeSource source,
        NextStep recommendedNextStep,
        TicketPriority suggestedPriority,
        List<String> suggestedRemedyIds
) {
}
