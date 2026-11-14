package com.konate.ithelpdesk.ticket.DataTier;


import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {


    List<Ticket> findByCategoryAndAffectedSystemAndCreatedAtAfterOrderByCreatedAtAsc(
            TicketCategory category, String affectedSystem, Instant since);

    boolean existsByRequesterEmailIgnoreCaseAndCategoryAndAffectedSystemAndCreatedAtAfter(
            String requesterEmail, TicketCategory category, String affectedSystem, Instant since);

    List<Ticket> findAllByOrderByCreatedAtDesc();
    List<Ticket> findByStatusInOrderByCreatedAtDesc(List<TicketStatus> statuses);

    List<Ticket> findByIncidentId(Long incidentId);
}