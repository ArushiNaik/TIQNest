package com.konate.ithelpdesk.user.config;

import com.konate.ithelpdesk.user.BusinessLayer.AuthService;
import com.konate.ithelpdesk.user.DataLayer.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Browsers send an OPTIONS "preflight" request first (CORS). Let it through.
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = AuthService.extractToken(request.getHeader("Authorization"));
        Optional<User> user = authService.findByToken(token);

        if (user.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        request.setAttribute("currentUser", user.get());
        return true;
    }
}
