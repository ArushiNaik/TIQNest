package com.konate.ithelpdesk.audit.Mapper;

import com.konate.ithelpdesk.audit.DataLayer.AuditEvent;
import com.konate.ithelpdesk.audit.PresentationLayer.AuditEventResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditEventMapper {

    AuditEventResponse toResponse(AuditEvent event);

    List<AuditEventResponse> toResponseList(List<AuditEvent> events);
}
