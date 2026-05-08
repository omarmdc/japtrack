package com.japtrack.project.repository;

import com.japtrack.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository <User, Long> {

    // Check if email is already in use
    boolean existsByUserEmail (String userEmail);

    // Check if username is already in use
    boolean existsByUserName (String userName);

    // Find user by email (for login)
    Optional<User> findByUserEmail(String userEmail);

    // Find user by username (for login)
    Optional<User> findByUserName(String userName);
}
