package com.konate.ithelpdesk.incident.PresentationLayer;

import com.konate.ithelpdesk.incident.DataLayer.IncidentStatus;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;

import java.time.Instant;

public record IncidentResponse(
        Long id,
        String title,
        TicketCategory category,
        String affectedSystem,
        IncidentStatus status,
        int ticketCount,
        Instant detectedAt,
        Instant lastReportAt,
        Instant resolvedAt,
        String bannerMessage      // text ready for the portal banner
) {}