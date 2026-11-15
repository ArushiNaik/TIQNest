package com.konate.ithelpdesk.triage.BusinessLayer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.konate.ithelpdesk.remediation.BusinessLayer.IssueType;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScopeResult(
        ScopeClassification classification,
        boolean isITRelated,
        double confidence,
        String reason,
        String clarifyingQuestion,
        IssueType issueType,
        TicketCategory category,
        ScopeSource source
) {
    public static ScopeResult of(ScopeClassification c, double confidence, String reason, String question,
                                 IssueType issueType, TicketCategory category, ScopeSource source) {
        return new ScopeResult(c, c.isItRelated(), confidence, reason, question, issueType, category, source);
    }

    public ScopeResult withSource(ScopeSource newSource) {
        return new ScopeResult(classification, isITRelated, confidence, reason, clarifyingQuestion,
                issueType, category, newSource);
    }
}
