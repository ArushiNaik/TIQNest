package com.konate.ithelpdesk.ticket.PresentationLayer;

import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import com.konate.ithelpdesk.ticket.DataTier.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketCategory category,
        String affectedSystem,
        String requesterEmail,
        TicketStatus status,
        Instant createdAt
) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(t.getId(), t.getTitle(), t.getDescription(),
                t.getCategory(), t.getAffectedSystem(), t.getRequesterEmail(),
                t.getStatus(), t.getCreatedAt());
    }
}