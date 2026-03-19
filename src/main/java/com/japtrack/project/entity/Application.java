package com.japtrack.project.entity;

import com.japtrack.project.enums.ApplicationStatus;
import com.japtrack.project.enums.EmploymentType;
import com.japtrack.project.enums.WorkSetting;
import com.japtrack.project.enums.WorkType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
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

    @Enumerated(EnumType.STRING)
    private WorkSetting workSetting;

    @Enumerated(EnumType.STRING)
    private WorkType workType;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Column(nullable = false)
    private LocalDate dateApplied;

    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}