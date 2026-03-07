package com.japtrack.project.dto.response;

import com.japtrack.project.entity.User;
import com.japtrack.project.enums.ApplicationStatus;
import com.japtrack.project.enums.EmploymentType;
import com.japtrack.project.enums.WorkSetting;
import com.japtrack.project.enums.WorkType;

import java.time.LocalDate;

public class ApplicationResponse {

    private Long applicationId;
    private Long userId;
    private String companyName;
    private String positionTitle;
    private String jobPostUrl;
    private Double payRate;
    private WorkSetting workSetting;
    private WorkType workType;
    private EmploymentType employmentType;
    private ApplicationStatus status;
    private LocalDate dateApplied;
    private String notes;
    private LocalDate createdAt;


    public ApplicationResponse() {}


    public Long getApplicationId() {
        return applicationId;
    }
    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
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

    public WorkSetting getWorkSetting() {
        return workSetting;
    }
    public void setWorkSetting(WorkSetting workSetting) {
        this.workSetting = workSetting;
    }

    public WorkType getWorkType() {
        return workType;
    }
    public void setWorkType(WorkType workType) {
        this.workType = workType;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }
    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public ApplicationStatus getStatus() {
        return status;
    }
    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDate getDateApplied() {
        return dateApplied;
    }
    public void setDateApplied(LocalDate dateApplied) {
        this.dateApplied = dateApplied;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
}
