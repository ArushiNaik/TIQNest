package com.konate.ithelpdesk.ticket.Mapper;

import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.PresentationLayer.CreateTicketRequest;
import com.konate.ithelpdesk.ticket.PresentationLayer.TicketResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "incident", ignore = true)
    Ticket toEntity(CreateTicketRequest request);

    @Mapping(target = "incidentId", source = "incident.id")
    TicketResponse toResponse(Ticket ticket);

    List<TicketResponse> toResponseList(List<Ticket> tickets);
}