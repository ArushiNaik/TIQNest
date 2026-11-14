package com.konate.ithelpdesk.incident.Mapper;

import com.konate.ithelpdesk.incident.DataLayer.Incident;
import com.konate.ithelpdesk.incident.PresentationLayer.IncidentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IncidentMapper {

    @Mapping(target = "bannerMessage", expression = "java(bannerMessage(incident))")
    IncidentResponse toResponse(Incident incident);

    List<IncidentResponse> toResponseList(List<Incident> incidents);

    default String bannerMessage(Incident incident) {
        return "⚠ " + incident.getAffectedSystem() + " outage detected: "
                + incident.getTicketCount() + " reports. Our team is working on it, "
                + "no need to open a new ticket.";
    }
}