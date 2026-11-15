package com.konate.ithelpdesk;

import com.konate.ithelpdesk.user.DataLayer.Role;
import com.konate.ithelpdesk.user.DataLayer.User;

/** In-memory users for tests: the "currentUser" request attribute the AuthInterceptor would set. */
public final class TestUsers {

    public static final String EMPLOYEE_EMAIL = "bahman@demo.com";
    public static final String OTHER_EMAIL = "amin@demo.com";
    public static final String ADMIN_EMAIL = "christine@demo.com";

    private TestUsers() {
    }

    public static User employee() {
        return user(EMPLOYEE_EMAIL, Role.EMPLOYEE);
    }

    public static User otherEmployee() {
        return user(OTHER_EMAIL, Role.EMPLOYEE);
    }

    public static User admin() {
        return user(ADMIN_EMAIL, Role.ADMIN);
    }

    public static User user(String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setName(email);
        u.setRole(role);
        return u;
    }
}
