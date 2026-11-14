package com.konate.ithelpdesk.remediation.DataLayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "remediation_attempts", indexes = {
        @Index(name = "idx_remediation_ticket", columnList = "ticket_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "remedy_id", nullable = false, length = 64)
    private String remedyId;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RemediationOutcome outcome;

    @Column(length = 1000)
    private String feedback;

    @Column(name = "reported_by")
    private String reportedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
