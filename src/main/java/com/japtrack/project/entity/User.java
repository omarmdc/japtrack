package com.japtrack.project.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users") // avoiding conflicts with MySQL reserved word 'user'
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {


    // Attributes

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY) // When a new record is added, increment the number
    private Long userId;

    @Getter(AccessLevel.NONE) // We have 2 methods with almost the same name, this way we avoid errors
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

    // We have 2 methods with almost the same name, this way we avoid errors
    public String getUserName() {
        return userName;
    }


    // Security (UserDetails methods~)

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return userEmail;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

