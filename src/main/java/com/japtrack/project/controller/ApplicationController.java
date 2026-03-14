package com.japtrack.project.controller;

import com.japtrack.project.dto.request.ApplicationRequest;
import com.japtrack.project.dto.response.ApplicationResponse;
import com.japtrack.project.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    // 1) Create a job application
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(@RequestBody ApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // 2) Update a job application
    @PatchMapping("/{applicationId}")
    public ApplicationResponse updateApplication(@PathVariable Long applicationId, @RequestBody ApplicationRequest request) {
        return applicationService.updateApplication(applicationId, request);
    }


    // 3) Delete a job application
    @DeleteMapping("/{applicationId}")
    public String deleteApplication(@PathVariable Long applicationId) {
        return applicationService.deleteApplication(applicationId);
    }


    // 4) Get a job application by its ID
    @GetMapping("/{applicationId}")
    public ApplicationResponse getApplicationById(@PathVariable Long applicationId) {
        return applicationService.getApplicationById(applicationId);
    }


    // 5) Get all job applications from a user (based on user's ID)
    @GetMapping("/user/{userId}")
    public List<ApplicationResponse> getApplicationsByUserId(@PathVariable Long userId) {
        return applicationService.getApplicationsByUserId(userId);
    }
}
