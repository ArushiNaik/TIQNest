package com.konate.ithelpdesk.user.DataLayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findAllByName(String name);

    Optional<User> findAllByUserIdentifier_UserId(String userIdentifierUserId);

    Optional<User> findAllByEmail(String email);

}
