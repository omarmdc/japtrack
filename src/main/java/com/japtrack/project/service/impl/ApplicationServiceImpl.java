package com.japtrack.project.service.impl;

import com.japtrack.project.dto.request.ApplicationRequest;
import com.japtrack.project.dto.response.ApplicationResponse;
import com.japtrack.project.dto.response.UserResponse;
import com.japtrack.project.entity.Application;
import com.japtrack.project.entity.User;
import com.japtrack.project.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationServiceImpl {

    @Autowired
    private ApplicationRepository applicationRepository;


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
    private Application findApplicationById(Long applicationId)  {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Sorry, we couldn't find the job application you are looking for :( "));
    }
}
