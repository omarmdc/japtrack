package com.japtrack.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "users") // avoiding conflicts with MySQL reserved word 'user'
@Getter
@Setter
@NoArgsConstructor
public class User {


    // Attributes

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY) // When a new record is added, increment the number
    private Long userId;

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(nullable = false, unique = true)
    private String userEmail;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String userFirstName;

    @Column(nullable = false)
    private String userLastName;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL) // CascadeType.ALL (if user is deleted, the applications are too)
    private List<Application> applications;

    @CreationTimestamp
    @Column(updatable = false)       // safety measure (making sure createdAt can't be overwritten)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    // Emails are stored trimmed and lowercase, so lookups and duplicate checks are case-insensitive
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

