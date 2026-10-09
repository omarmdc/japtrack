package com.japtrack.project.service.impl;

import com.japtrack.project.dto.request.CreateApplicationRequest;
import com.japtrack.project.dto.request.UpdateApplicationRequest;
import com.japtrack.project.dto.response.ApplicationResponse;
import com.japtrack.project.entity.Application;
import com.japtrack.project.entity.User;
import com.japtrack.project.exception.custom.ResourceNotFoundException;
import com.japtrack.project.repository.ApplicationRepository;
import com.japtrack.project.repository.UserRepository;
import com.japtrack.project.service.ApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }


    /*
       - HELPER METHODS:
            * convertToResponse
            * findOwnedApplication
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

    // 2) The single ownership check used by get, update and delete.
    // The query matches on BOTH the application ID and the owner, so another user's application
    // is simply "not found". We return 404 rather than 403 on purpose: a 403 would confirm that
    // the ID exists and belongs to someone else, which lets a client probe for other users' data.
    private Application findOwnedApplication(Long currentUserId, Long applicationId) {
        return applicationRepository.findByApplicationIdAndUser_UserId(applicationId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sorry, we couldn't find a job application with ID: " + applicationId));
    }


    /*
    - SIGNATURE METHODS:
         * getApplications
         * createApplication
         * getApplication
         * updateApplication
         * deleteApplication
    */

    // 1) Get all of the current user's job applications (newest first)
    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplications(Long currentUserId) {

        return applicationRepository.findByUser_UserIdOrderByDateAppliedDescApplicationIdDesc(currentUserId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // 2) Create a job application owned by the current user
    @Override
    @Transactional
    public ApplicationResponse createApplication(Long currentUserId, CreateApplicationRequest request) {

        // The owner comes only from the session. The request has no userId field, so a client
        // cannot create an application for someone else.
        // (The account could only be missing if it was deleted while this session was still active.)
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sorry, we couldn't find user with ID: " + currentUserId));

        Application application = new Application();

        application.setUser(owner);
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


    // 3) Get one of the current user's job applications
    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(Long currentUserId, Long applicationId) {

        return convertToResponse(findOwnedApplication(currentUserId, applicationId));
    }


    // 4) Update one of the current user's job applications
    @Override
    @Transactional
    public ApplicationResponse updateApplication(Long currentUserId, Long applicationId, UpdateApplicationRequest request) {

        // Ownership is checked before anything is changed
        Application application = findOwnedApplication(currentUserId, applicationId);


        // Check which field exactly does the user want to update,
        // this prevents the user from inputting all other fields again.
        // The owner is never updated: UpdateApplicationRequest has no userId field.

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


    // 5) Delete one of the current user's job applications
    @Override
    @Transactional
    public void deleteApplication(Long currentUserId, Long applicationId) {

        // Same ownership check as get/update: someone else's application is a 404 and is never deleted
        Application application = findOwnedApplication(currentUserId, applicationId);
        applicationRepository.delete(application);
    }
}
