package com.konate.ithelpdesk.remediation.DataLayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RemediationAttemptRepository extends JpaRepository<RemediationAttempt, Long> {

    List<RemediationAttempt> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    List<RemediationAttempt> findTop20ByReportedByIgnoreCaseOrderByCreatedAtDesc(String reportedBy);

    List<RemediationAttempt> findTop20ByOrderByCreatedAtDesc();
}
