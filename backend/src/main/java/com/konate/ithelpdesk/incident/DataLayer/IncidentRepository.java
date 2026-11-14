package com.konate.ithelpdesk.incident.DataLayer;

import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // Is there already an open outage for this category + system?
    // If yes, new tickets join it instead of creating a second incident.
    Optional<Incident> findFirstByCategoryAndAffectedSystemAndStatus(
            TicketCategory category, String affectedSystem, IncidentStatus status);

    // For the banner: all active outages, newest first
    List<Incident> findByStatusOrderByDetectedAtDesc(IncidentStatus status);
}