package com.konate.ithelpdesk.user.MappingLayer;

import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.DataLayer.UserIdentifier;
import com.konate.ithelpdesk.user.PresentationLayer.UserRequestModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UserRequestMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(expression = "java(userIdentifier)", target = "userIdentifier"),
            @Mapping(source = "requestModel.name", target = "name"),
            @Mapping(source = "requestModel.email", target = "email"),
            @Mapping(source = "requestModel.password", target = "password"),
            @Mapping(source = "requestModel.role", target = "role")
    })
    User toRequestModel(UserRequestModel requestModel, UserIdentifier userIdentifier);
}
