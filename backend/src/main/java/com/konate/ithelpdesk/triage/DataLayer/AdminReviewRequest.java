package com.konate.ithelpdesk.triage.DataLayer;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An employee's explicit request for a human to look at a submission judged out of IT scope.
 * Deliberately separate from tickets so it never reaches SLA tracking or TiqRadar outage detection.
 */
@Entity
@Table(name = "admin_review_requests", indexes = {
        @Index(name = "idx_review_requester", columnList = "requester_email, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReviewRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_email", nullable = false)
    private String requesterEmail;

    @Column(name = "submitted_text", nullable = false, length = 1000)
    private String submittedText;

    @Column(name = "classification", length = 32)
    private String classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private AdminReviewStatus status = AdminReviewStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private String closedBy;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
