package com.konate.ithelpdesk.user.PresentationLayer;

import com.konate.ithelpdesk.user.BusinessLayer.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return authService.loginUser(request.email(), request.password())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseModel> me(
            @RequestHeader(value = "Authorization", required = false) String header) {
        return authService.getCurrentUser(AuthService.extractToken(header))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String header) {
        authService.logout(AuthService.extractToken(header));
        return ResponseEntity.noContent().build();
    }
}