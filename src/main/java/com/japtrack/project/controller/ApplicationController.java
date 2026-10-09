package com.japtrack.project.controller;

import com.japtrack.project.dto.request.CreateApplicationRequest;
import com.japtrack.project.dto.request.UpdateApplicationRequest;
import com.japtrack.project.dto.response.ApplicationResponse;
import com.japtrack.project.security.AuthenticatedUser;
import com.japtrack.project.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Every endpoint works on the logged-in user's applications only.
// @AuthenticationPrincipal gives us the AuthenticatedUser that Spring Security restored from the session,
// so the user ID can't be tampered with by the client. No endpoint accepts a userId.
// All routes require login (SecurityConfig), so currentUser is always present here.
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }


    // 1) List the current user's job applications
    @GetMapping
    public List<ApplicationResponse> getApplications(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return applicationService.getApplications(currentUser.getUserId());
    }


    // 2) Create a job application for the current user
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                                 @Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(currentUser.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // 3) Get one of the current user's job applications
    @GetMapping("/{applicationId}")
    public ApplicationResponse getApplication(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                              @PathVariable Long applicationId) {
        return applicationService.getApplication(currentUser.getUserId(), applicationId);
    }


    // 4) Update one of the current user's job applications
    @PatchMapping("/{applicationId}")
    public ApplicationResponse updateApplication(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                 @PathVariable Long applicationId,
                                                 @Valid @RequestBody UpdateApplicationRequest request) {
        return applicationService.updateApplication(currentUser.getUserId(), applicationId, request);
    }


    // 5) Delete one of the current user's job applications
    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> deleteApplication(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                  @PathVariable Long applicationId) {
        applicationService.deleteApplication(currentUser.getUserId(), applicationId);
        return ResponseEntity.noContent().build();
    }
}
