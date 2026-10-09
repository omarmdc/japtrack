package com.japtrack.project.service;

import com.japtrack.project.dto.request.RegisterRequest;
import com.japtrack.project.dto.request.UserRequest;
import com.japtrack.project.dto.response.UserResponse;

public interface UserService {

    UserResponse createUser (RegisterRequest request);
    UserResponse updateUser (Long userId, UserRequest request);
    String deleteUser (Long userId);
    UserResponse getUserById (Long userId);
}
