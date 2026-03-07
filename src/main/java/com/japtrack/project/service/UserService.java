package com.japtrack.project.service;

import com.japtrack.project.dto.request.UserRequest;
import com.japtrack.project.dto.response.UserResponse;

public interface UserService {

    UserResponse createUser (UserRequest request);
    UserResponse updateUser (Long userId, UserRequest request);
    String deleteUser (Long userId);
    UserResponse getUserById (Long userId);
}
