package com.konate.ithelpdesk.user.PresentationLayer;

public record LoginRequest(
        String email,
        String password) {
}
