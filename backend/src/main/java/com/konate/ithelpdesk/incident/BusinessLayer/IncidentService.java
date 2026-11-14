package com.konate.ithelpdesk.incident.BusinessLayer;

import com.konate.ithelpdesk.incident.DataLayer.Incident;
import com.konate.ithelpdesk.incident.DataLayer.IncidentRepository;
import com.konate.ithelpdesk.incident.DataLayer.IncidentStatus;
import com.konate.ithelpdesk.incident.Mapper.IncidentMapper;
import com.konate.ithelpdesk.incident.PresentationLayer.IncidentResponse;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import com.konate.ithelpdesk.ticket.DataTier.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final TicketRepository ticketRepository;
    private final IncidentMapper incidentMapper;

    @Transactional(readOnly = true)
    public List<IncidentResponse> findActive() {
        return incidentMapper.toResponseList(
                incidentRepository.findByStatusOrderByDetectedAtDesc(IncidentStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> findAll() {
        return incidentMapper.toResponseList(incidentRepository.findAll());
    }

    @Transactional
    public IncidentResponse resolve(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Incident " + id + " not found"));

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Incident " + id + " is already resolved");
        }

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());
        ticketRepository.findByIncidentId(id)
                .forEach(t -> t.setStatus(TicketStatus.RESOLVED));

        return incidentMapper.toResponse(incident);
    }
}