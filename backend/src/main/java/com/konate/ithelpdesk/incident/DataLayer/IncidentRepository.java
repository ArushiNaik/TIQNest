package com.konate.ithelpdesk.incident.DataLayer;

import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findFirstByCategoryAndAffectedSystemAndStatus(
            TicketCategory category, String affectedSystem, IncidentStatus status);

    List<Incident> findByStatusOrderByDetectedAtDesc(IncidentStatus status);

    Optional<Incident> findByActiveKey(String activeKey);
}