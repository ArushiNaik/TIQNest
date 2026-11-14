package com.konate.ithelpdesk.user.PresentationLayer;


import com.konate.ithelpdesk.user.DataLayer.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseModel {
    String userId;

    String name;

    String email;

    Role role;

}
