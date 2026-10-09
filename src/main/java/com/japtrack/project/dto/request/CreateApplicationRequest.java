package com.japtrack.project.dto.request;

import com.japtrack.project.enums.ApplicationStatus;
import com.japtrack.project.enums.EmploymentType;
import com.japtrack.project.enums.WorkSetting;
import com.japtrack.project.enums.WorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

// There is deliberately no userId field: the owner is always the logged-in user (see ApplicationServiceImpl).
// Any "userId" a client sends is ignored when the JSON is read.
@Data
@NoArgsConstructor
public class CreateApplicationRequest {

    // String limits match the database columns (VARCHAR 255), so a long value is a 400, not a database error
    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must be at most 255 characters")
    private String companyName;

    @NotBlank(message = "Position title is required")
    @Size(max = 255, message = "Position title must be at most 255 characters")
    private String positionTitle;

    // Only http(s) links, so the frontend can safely render it as a clickable link
    @URL(regexp = "^https?://.*", message = "Job post URL must be a valid http or https link")
    @Size(max = 255, message = "Job post URL must be at most 255 characters")
    private String jobPostUrl;

    @PositiveOrZero(message = "Pay rate cannot be negative")
    private Double payRate;

    private WorkSetting workSetting;
    private WorkType workType;
    private EmploymentType employmentType;
    private ApplicationStatus status;

    @NotNull(message = "Date applied is required")
    private LocalDate dateApplied;

    @Size(max = 255, message = "Notes must be at most 255 characters")
    private String notes;
}
