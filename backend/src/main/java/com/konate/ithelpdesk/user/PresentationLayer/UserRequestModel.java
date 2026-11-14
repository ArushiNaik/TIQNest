package com.konate.ithelpdesk.user.PresentationLayer;


import com.konate.ithelpdesk.user.DataLayer.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestModel {
    String name;

    String email;

    String password;

    Role role;

}
