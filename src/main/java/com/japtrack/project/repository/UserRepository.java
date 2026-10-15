package com.japtrack.project.repository;

import com.japtrack.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository <User, Long> {

    // Check if email is already in use
    boolean existsByUserEmail (String userEmail);

    // Check if username is already in use
    boolean existsByUserName (String userName);

    // Profile updates: is this email/username taken by a DIFFERENT account?
    // Excluding the user's own ID means re-sending your current email or username isn't a false 409.
    boolean existsByUserEmailAndUserIdNot (String userEmail, Long userId);
    boolean existsByUserNameAndUserIdNot (String userName, Long userId);

    // Find user by email (for login)
    Optional<User> findByUserEmail(String userEmail);

    // Find user by username (for login)
    Optional<User> findByUserName(String userName);
}
