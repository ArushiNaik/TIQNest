package com.konate.ithelpdesk.user.BusinessLayer;

import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.PresentationLayer.LoginResponse;
import com.konate.ithelpdesk.user.PresentationLayer.UserRequestModel;
import com.konate.ithelpdesk.user.PresentationLayer.UserResponseModel;

import java.util.List;
import java.util.Optional;

public interface AuthService {
    UserResponseModel registerUser(UserRequestModel requestModel);

    List<UserResponseModel> findAllUsers();

    Optional<LoginResponse> loginUser(String email, String password);

    Optional<User> findByToken(String token);

    Optional<UserResponseModel> getCurrentUser(String token);

    void logout(String token);

    // Reads the token from the header "Authorization: Bearer <token>"
    static String extractToken(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
