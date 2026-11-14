package com.konate.ithelpdesk.audit.PresentationLayer;

import com.konate.ithelpdesk.audit.DataLayer.AuditAction;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        Long ticketId,
        AuditAction action,
        String oldValue,
        String newValue,
        String actor,
        String message,
        Instant createdAt
) {}
