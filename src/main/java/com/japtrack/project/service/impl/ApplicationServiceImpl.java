package com.japtrack.project.service.impl;

import com.japtrack.project.dto.request.ApplicationRequest;
import com.japtrack.project.dto.response.ApplicationResponse;
import com.japtrack.project.dto.response.UserResponse;
import com.japtrack.project.entity.Application;
import com.japtrack.project.entity.User;
import com.japtrack.project.repository.ApplicationRepository;
import com.japtrack.project.repository.UserRepository;
import com.japtrack.project.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;


    /*
       - HELPER METHODS:
            * convertToResponse
            * findApplicationById
   */

    // 1) convertToResponse
    private ApplicationResponse convertToResponse(Application application) {

        ApplicationResponse response = new ApplicationResponse();

        response.setApplicationId(application.getApplicationId());
        response.setUserId(application.getUser().getUserId());
        response.setCompanyName(application.getCompanyName());
        response.setPositionTitle(application.getPositionTitle());
        response.setJobPostUrl(application.getJobPostUrl());
        response.setPayRate(application.getPayRate());
        response.setWorkSetting(application.getWorkSetting());
        response.setWorkType(application.getWorkType());
        response.setEmploymentType(application.getEmploymentType());
        response.setStatus(application.getStatus());
        response.setDateApplied(application.getDateApplied());
        response.setNotes(application.getNotes());
        response.setCreatedAt(application.getCreatedAt().toLocalDate());

        return response;
    }

    // 2) Find job application by its ID
    private Application findApplicationById(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sorry, we couldn't find the job application you are looking for :( "));
    }

     /*
    - SIGNATURE METHODS:
         * createUser
         * updateUser
         * deleteUser
         * getUserById
*/

    // 1) Create a job application
    public ApplicationResponse createApplication(ApplicationRequest request) {

        Application application = new Application();

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        application.setUser(user);
        application.setCompanyName(request.getCompanyName());
        application.setPositionTitle(request.getPositionTitle());
        application.setJobPostUrl(request.getJobPostUrl());
        application.setPayRate(request.getPayRate());
        application.setWorkSetting(request.getWorkSetting());
        application.setWorkType(request.getWorkType());
        application.setEmploymentType(request.getEmploymentType());
        application.setStatus(request.getStatus());
        application.setDateApplied(request.getDateApplied());
        application.setNotes(request.getNotes());

        Application savedApplication = applicationRepository.save(application);

        return convertToResponse(savedApplication);
    }


    // 2) Update a job application
    @Override
    public ApplicationResponse updateApplication(Long applicationId, ApplicationRequest request) {

        Application application = findApplicationById(applicationId);


        // Check which field exactly does the user want to update,
        // this prevents the user from inputting all other fields again.

        if (request.getCompanyName() != null) {
            application.setCompanyName(request.getCompanyName());
        }
        if (request.getPositionTitle() != null) {
            application.setPositionTitle(request.getPositionTitle());
        }
        if (request.getJobPostUrl() != null) {
            application.setJobPostUrl(request.getJobPostUrl());
        }
        if (request.getPayRate() != null) {
            application.setPayRate(request.getPayRate());
        }
        if (request.getWorkSetting() != null) {
            application.setWorkSetting(request.getWorkSetting());
        }
        if (request.getWorkType() != null) {
            application.setWorkType(request.getWorkType());
        }
        if (request.getEmploymentType() != null) {
            application.setEmploymentType(request.getEmploymentType());
        }
        if (request.getStatus() != null) {
            application.setStatus(request.getStatus());
        }
        if (request.getDateApplied() != null) {
            application.setDateApplied(request.getDateApplied());
        }
        if (request.getNotes() != null) {
            application.setNotes(request.getNotes());
        }

        Application updatedApplication = applicationRepository.save(application);
        return convertToResponse(updatedApplication);
    }


    // 3) Delete a job application
    @Override
    public String deleteApplication(Long applicationId) {

        Application application = findApplicationById(applicationId);
        applicationRepository.delete(application);

        return "Application deleted successfully!";
    }


    // 4) Get a job application by its ID
    @Override
    public ApplicationResponse getApplicationById(Long applicationId) {

        Application application = findApplicationById(applicationId);

        return convertToResponse(application);
    }


    // 5) Get all job applications from a user (based on user's ID)
    @Override
    public List<ApplicationResponse> getApplicationsByUserId(Long userId) {

        List<Application> userApplications = applicationRepository.findByUser_UserId(userId);

        return userApplications.stream()
                .map(application -> convertToResponse(application))
                .collect(Collectors.toList());
    }
}