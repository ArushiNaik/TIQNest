package com.konate.ithelpdesk.sla;

import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketPriority;
import com.konate.ithelpdesk.ticket.DataTier.TicketStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class SlaPolicy {

    @Value("${sla.hours.critical:1}")
    private long criticalHours;

    @Value("${sla.hours.high:4}")
    private long highHours;

    @Value("${sla.hours.medium:24}")
    private long mediumHours;

    @Value("${sla.hours.low:72}")
    private long lowHours;

    public Instant deadlineFor(TicketPriority priority, Instant from) {
        long hours = switch (priority) {
            case CRITICAL -> criticalHours;
            case HIGH -> highHours;
            case MEDIUM -> mediumHours;
            case LOW -> lowHours;
        };
        return from.plus(Duration.ofHours(hours));
    }

    public static SlaStatus statusOf(Ticket t) {
        if (t.getSlaDeadline() == null || t.getCreatedAt() == null) return null;

        boolean done = t.getStatus() == TicketStatus.RESOLVED || t.getStatus() == TicketStatus.CLOSED;
        if (done) {
            return t.getResolvedAt() != null && !t.getResolvedAt().isAfter(t.getSlaDeadline())
                    ? SlaStatus.MET : SlaStatus.BREACHED;
        }

        Instant now = Instant.now();
        if (now.isAfter(t.getSlaDeadline())) return SlaStatus.BREACHED;

        long total = Duration.between(t.getCreatedAt(), t.getSlaDeadline()).toSeconds();
        long left = Duration.between(now, t.getSlaDeadline()).toSeconds();
        return left * 4 < total ? SlaStatus.AT_RISK : SlaStatus.ON_TRACK;
    }
}