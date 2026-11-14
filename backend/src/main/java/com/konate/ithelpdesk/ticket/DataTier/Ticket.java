package com.konate.ithelpdesk.ticket.DataTier;

import com.konate.ithelpdesk.incident.DataLayer.Incident;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_radar", columnList = "category, affected_system, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketCategory category;

    @Column(name = "affected_system", nullable = false)
    private String affectedSystem;

    @Column(name = "requester_email", nullable = false)
    private String requesterEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "assignee")
    private String assignee;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (affectedSystem != null) affectedSystem = affectedSystem.trim().toUpperCase();
    }
}
