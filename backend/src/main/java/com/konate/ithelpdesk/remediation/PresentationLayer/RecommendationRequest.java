package com.konate.ithelpdesk.remediation.PresentationLayer;

import com.konate.ithelpdesk.remediation.BusinessLayer.IssueType;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Either point at an existing ticket ({@code ticketId}) or describe the problem manually.
 * At least one field must be provided. There is deliberately no field for a command.
 */
public record RecommendationRequest(
        @Positive Long ticketId,
        IssueType issueType,
        TicketCategory category,
        @Size(max = 100) String affectedSystem,
        @Size(max = 2000) String description
) {}
