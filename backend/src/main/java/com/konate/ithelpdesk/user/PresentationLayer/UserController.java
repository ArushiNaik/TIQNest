package com.konate.ithelpdesk.user.PresentationLayer;

import com.konate.ithelpdesk.user.BusinessLayer.AuthService;
import com.konate.ithelpdesk.user.DataLayer.Role;
import com.konate.ithelpdesk.user.DataLayer.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public List<UserResponseModel> findAll(@RequestAttribute("currentUser") User currentUser) {
        requireAdmin(currentUser);
        return authService.findAllUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseModel create(@RequestAttribute("currentUser") User currentUser,
                                    @RequestBody UserRequestModel request) {
        requireAdmin(currentUser);
        return authService.registerUser(request);
    }

    private static void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can manage users");
        }
    }
}
