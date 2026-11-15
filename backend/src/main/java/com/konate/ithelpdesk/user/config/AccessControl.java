package com.konate.ithelpdesk.user.config;

import com.konate.ithelpdesk.user.DataLayer.Role;
import com.konate.ithelpdesk.user.DataLayer.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Server-side authorization checks. The authenticated {@link User} comes from the
 * "currentUser" request attribute set by {@link AuthInterceptor}, never from request bodies.
 */
public final class AccessControl {

    public static final String CURRENT_USER = "currentUser";

    private AccessControl() {
    }

    public static User requireUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        return user;
    }

    public static boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    public static User requireAdmin(User user) {
        requireUser(user);
        if (!isAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only IT administrators can do this");
        }
        return user;
    }

    public static boolean isOwner(User user, String ownerEmail) {
        return user != null && ownerEmail != null && user.getEmail() != null
                && user.getEmail().equalsIgnoreCase(ownerEmail);
    }

    public static void requireOwnerOrAdmin(User user, String ownerEmail) {
        requireUser(user);
        if (!isAdmin(user) && !isOwner(user, ownerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own tickets");
        }
    }
}
