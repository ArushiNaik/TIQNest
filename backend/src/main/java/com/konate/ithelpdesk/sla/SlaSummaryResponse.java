package com.konate.ithelpdesk.sla;

public record SlaSummaryResponse(
        long onTrack,
        long atRisk,
        long breached,
        long met,
        long total
) {}
