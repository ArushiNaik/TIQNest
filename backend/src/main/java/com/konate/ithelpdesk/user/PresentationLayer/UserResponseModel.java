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
public class UserResponseModel {
    String name;

    String email;

    Role role;

}
