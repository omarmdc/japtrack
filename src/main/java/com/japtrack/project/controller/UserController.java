package com.japtrack.project.controller;

import com.japtrack.project.dto.request.UserRequest;
import com.japtrack.project.dto.response.UserResponse;
import com.japtrack.project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;


    // 1) Create a user
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2) Update a user
    @PatchMapping("/{userId}")
    public UserResponse updateUser(@PathVariable Long userId, @RequestBody UserRequest request) {
        return userService.updateUser(userId, request);
    }

    // 3) Delete a user
    @DeleteMapping("/{userId}")
    public String deleteUser (@PathVariable Long userId) {
        return userService.deleteUser(userId);
    }

    // 4) Get a user by id
    @GetMapping("/{userId}")
    public UserResponse getUserById (@PathVariable Long userId) {
        return userService.getUserById(userId);
    }
}
