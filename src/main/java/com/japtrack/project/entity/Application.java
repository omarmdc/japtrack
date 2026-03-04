package com.japtrack.project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {

    // Attributes

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // When a new record is added, increment the number
    private Long applicationId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String positionTitle;

    private String jobPostUrl;

    private Double payRate;

    private String workSetting;

    private String workType;

    private String employmentType;

    @Column(nullable = false)
    private LocalDate dateApplied;

    private String status;

    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    // Constructor
    public Application() {
    }

    public Long getApplicationId() {
        return applicationId;
    }
    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPositionTitle() {
        return positionTitle;
    }
    public void setPositionTitle(String positionTitle) {
        this.positionTitle = positionTitle;
    }

    public String getJobPostUrl() {
        return jobPostUrl;
    }
    public void setJobPostUrl(String jobPostUrl) {
        this.jobPostUrl = jobPostUrl;
    }

    public Double getPayRate() {
        return payRate;
    }
    public void setPayRate(Double payRate) {
        this.payRate = payRate;
    }

    public String getWorkSetting() {
        return workSetting;
    }
    public void setWorkSetting(String workSetting) {
        this.workSetting = workSetting;
    }

    public String getWorkType() {
        return workType;
    }
    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public String getEmploymentType() {
        return employmentType;
    }
    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public LocalDate getDateApplied() {
        return dateApplied;
    }
    public void setDateApplied(LocalDate dateApplied) {
        this.dateApplied = dateApplied;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}