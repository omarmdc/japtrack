package com.japtrack.project.service;

import com.japtrack.project.dto.request.ApplicationRequest;
import com.japtrack.project.dto.response.ApplicationResponse;
import com.japtrack.project.entity.Application;

import java.util.List;

public interface ApplicationService {

    ApplicationResponse createApplication (ApplicationRequest request);
    ApplicationResponse updateApplication (Long applicationId, ApplicationRequest request);
    String deleteApplication (Long applicationId);
    ApplicationResponse getApplicationById (Long applicationId);
    List<ApplicationResponse> getApplicationsByUserId (Long userId);
}
