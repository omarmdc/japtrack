package com.japtrack.project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users") // avoiding conflicts with MySQL reserved word 'user'
public class User {


    // Attributes

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY) // When a new record is added, increment the number
    private Long userID;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL) // CascadeType.ALL (if user is deleted, the applications are too)
    private List<Application> applications;

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(nullable = false)
    private String userFirstName;

    @Column(nullable = false)
    private String userLastName;

    @Column(nullable = false, unique = true)
    private String userEmail;

    @Column(nullable = false)
    private String passwordHash;

    @CreationTimestamp
    @Column(updatable = false)       // safety measure (making sure createdAt can't be overwritten)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    // Constructor
    public User() {
    }


    // Getters and Setters

    public Long getUserID() {
        return userID;
    }
    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public List<Application> getApplications() {
        return applications;
    }
    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserFirstName() {
        return userFirstName;
    }
    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public String getUserLastName() {
        return userLastName;
    }
    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }

    public String getUserEmail() {
        return userEmail;
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt (LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
