package com.konate.ithelpdesk.ticket.PresentationLayer;

import com.konate.ithelpdesk.sla.SlaStatus;
import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import com.konate.ithelpdesk.ticket.DataTier.TicketPriority;
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
        TicketPriority priority,
        String assignee,
        Instant createdAt,
        Instant slaDeadline,
        SlaStatus slaStatus,
        Instant resolvedAt,
        Long incidentId
) {}