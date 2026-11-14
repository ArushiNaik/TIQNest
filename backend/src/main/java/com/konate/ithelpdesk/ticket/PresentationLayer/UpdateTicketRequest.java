package com.konate.ithelpdesk.ticket.PresentationLayer;

import com.konate.ithelpdesk.ticket.DataTier.TicketPriority;
import com.konate.ithelpdesk.ticket.DataTier.TicketStatus;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        TicketStatus status,
        TicketPriority priority,
        @Size(max = 100) String assignee,
        @Size(max = 100) String actor
) {}
