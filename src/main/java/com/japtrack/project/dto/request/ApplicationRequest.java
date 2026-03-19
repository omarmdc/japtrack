package com.japtrack.project.dto.request;

import com.japtrack.project.enums.ApplicationStatus;
import com.japtrack.project.enums.EmploymentType;
import com.japtrack.project.enums.WorkSetting;
import com.japtrack.project.enums.WorkType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ApplicationRequest {

    private String companyName;
    private Long userId;
    private String positionTitle;
    private String jobPostUrl;
    private Double payRate;
    private WorkSetting workSetting;
    private WorkType workType;
    private EmploymentType employmentType;
    private ApplicationStatus status;
    private LocalDate dateApplied;
    private String notes;
}
