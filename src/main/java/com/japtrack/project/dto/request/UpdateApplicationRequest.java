package com.japtrack.project.dto.request;

import com.japtrack.project.enums.ApplicationStatus;
import com.japtrack.project.enums.EmploymentType;
import com.japtrack.project.enums.WorkSetting;
import com.japtrack.project.enums.WorkType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

// PATCH body: every field is optional and null means "leave unchanged".
// Bean Validation treats null as valid for all of these constraints, so they only apply to fields that are sent.
// Like CreateApplicationRequest, there is no userId: an application can never be moved to another user.
@Data
@NoArgsConstructor
public class UpdateApplicationRequest {

    // @NotBlank would reject a missing field; this pattern only rejects a value that is present but blank
    @Pattern(regexp = ".*\\S.*", message = "Company name cannot be blank")
    @Size(max = 255, message = "Company name must be at most 255 characters")
    private String companyName;

    @Pattern(regexp = ".*\\S.*", message = "Position title cannot be blank")
    @Size(max = 255, message = "Position title must be at most 255 characters")
    private String positionTitle;

    @URL(regexp = "^https?://.*", message = "Job post URL must be a valid http or https link")
    @Size(max = 255, message = "Job post URL must be at most 255 characters")
    private String jobPostUrl;

    @PositiveOrZero(message = "Pay rate cannot be negative")
    private Double payRate;

    private WorkSetting workSetting;
    private WorkType workType;
    private EmploymentType employmentType;
    private ApplicationStatus status;
    private LocalDate dateApplied;

    @Size(max = 255, message = "Notes must be at most 255 characters")
    private String notes;
}
