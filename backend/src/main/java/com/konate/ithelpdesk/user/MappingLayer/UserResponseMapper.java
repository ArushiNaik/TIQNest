package com.konate.ithelpdesk.user.MappingLayer;

import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.PresentationLayer.UserResponseModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserResponseMapper {
    @Mappings({
            @Mapping(source = "userIdentifier.userId", target = "userId"),
            @Mapping(source = "name", target = "name"),
            @Mapping(source = "email", target = "email"),
            @Mapping(source = "role", target = "role")
    })
    UserResponseModel toResponseModel(User user);
    List<UserResponseModel> entityListToResponseModelList(List<User> userList);


}
