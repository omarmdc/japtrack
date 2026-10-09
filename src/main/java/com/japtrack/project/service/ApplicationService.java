package com.japtrack.project.service;

import com.japtrack.project.dto.request.CreateApplicationRequest;
import com.japtrack.project.dto.request.UpdateApplicationRequest;
import com.japtrack.project.dto.response.ApplicationResponse;

import java.util.List;

// currentUserId always comes from the authenticated session (see ApplicationController), never from the client
public interface ApplicationService {

    List<ApplicationResponse> getApplications (Long currentUserId);
    ApplicationResponse createApplication (Long currentUserId, CreateApplicationRequest request);
    ApplicationResponse getApplication (Long currentUserId, Long applicationId);
    ApplicationResponse updateApplication (Long currentUserId, Long applicationId, UpdateApplicationRequest request);
    void deleteApplication (Long currentUserId, Long applicationId);
}
