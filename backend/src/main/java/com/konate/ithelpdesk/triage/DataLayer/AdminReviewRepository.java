package com.konate.ithelpdesk.triage.DataLayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AdminReviewRepository extends JpaRepository<AdminReviewRequest, Long> {

    boolean existsByRequesterEmailIgnoreCaseAndSubmittedTextIgnoreCaseAndCreatedAtAfter(
            String requesterEmail, String submittedText, Instant since);

    long countByRequesterEmailIgnoreCaseAndStatus(String requesterEmail, AdminReviewStatus status);

    List<AdminReviewRequest> findAllByOrderByCreatedAtDesc();

    List<AdminReviewRequest> findByRequesterEmailIgnoreCaseOrderByCreatedAtDesc(String requesterEmail);
}
