package com.japtrack.project.dto.request;

import com.japtrack.project.enums.ApplicationStatus;
import com.japtrack.project.enums.EmploymentType;
import com.japtrack.project.enums.WorkSetting;
import com.japtrack.project.enums.WorkType;

import java.time.LocalDate;

public class ApplicationRequest {

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

    public ApplicationRequest() {}

    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Double getPayRate() {
        return payRate;
    }
    public void setPayRate(Double payRate) {
        this.payRate = payRate;
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
}
