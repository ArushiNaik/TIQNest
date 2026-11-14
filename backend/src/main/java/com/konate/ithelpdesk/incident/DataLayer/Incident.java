package com.konate.ithelpdesk.incident.DataLayer;

import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketCategory category;

    @Column(name = "affected_system", nullable = false)
    private String affectedSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.ACTIVE;

    @Column(name = "ticket_count", nullable = false)
    @Builder.Default
    private int ticketCount = 0;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(name = "last_report_at")
    private Instant lastReportAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void onCreate() {
        if (detectedAt == null) detectedAt = Instant.now();
        if (lastReportAt == null) lastReportAt = detectedAt;
    }
}
