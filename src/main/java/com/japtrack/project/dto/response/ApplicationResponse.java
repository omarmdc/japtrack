package com.japtrack.project.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.japtrack.project.entity.User;
import com.japtrack.project.enums.ApplicationStatus;
import com.japtrack.project.enums.EmploymentType;
import com.japtrack.project.enums.WorkSetting;
import com.japtrack.project.enums.WorkType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@JsonPropertyOrder({"applicationId", "userId", "jobPostUrl", "companyName", "positionTitle", "payRate", "workSetting", "workType", "employmentType", "status", "dateApplied", "notes", "createdAt"})
@Data
@NoArgsConstructor
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
}
