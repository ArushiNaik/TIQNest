package com.konate.ithelpdesk.user.DataLayer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.util.UUID;

@Embeddable
@Getter
public class UserIdentifier {
    @Column(name = "user_id", unique = true)
    private String userId;

    public UserIdentifier(String userId) {
        this.userId = userId;
    }

    public UserIdentifier() {
        this.userId = UUID.randomUUID().toString();
    }
}
