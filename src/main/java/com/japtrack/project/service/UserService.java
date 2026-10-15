package com.japtrack.project.service;

import com.japtrack.project.dto.request.RegisterRequest;
import com.japtrack.project.dto.request.UpdateUserRequest;
import com.japtrack.project.dto.response.UserResponse;

// currentUserId always comes from the authenticated session, never from the client.
// There is deliberately no "get/update/delete user by ID" operation: a user can only manage their own account.
public interface UserService {

    UserResponse createUser (RegisterRequest request);
    UserResponse getCurrentUser (Long currentUserId);
    UserResponse updateCurrentUser (Long currentUserId, UpdateUserRequest request);
    void deleteCurrentUser (Long currentUserId);
}
