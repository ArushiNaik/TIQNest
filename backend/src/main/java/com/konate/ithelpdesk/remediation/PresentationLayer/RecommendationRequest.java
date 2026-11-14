package com.konate.ithelpdesk.remediation.PresentationLayer;

import com.konate.ithelpdesk.remediation.BusinessLayer.IssueType;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RecommendationRequest(
        @Positive Long ticketId,
        IssueType issueType,
        TicketCategory category,
        @Size(max = 100) String affectedSystem,
        @Size(max = 2000) String description
) {}
