package com.japtrack.project.service.impl;

import com.japtrack.project.dto.request.UserRequest;
import com.japtrack.project.dto.response.UserResponse;
import com.japtrack.project.entity.User;
import com.japtrack.project.exception.custom.DuplicateResourceException;
import com.japtrack.project.exception.custom.ResourceNotFoundException;
import com.japtrack.project.repository.UserRepository;
import com.japtrack.project.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    /*
       - HELPER METHODS:
            * convertToResponse
            * findUserById
   */

    // 1) convertToResponse
    private UserResponse convertToResponse(User user) {

        UserResponse response = new UserResponse();

        response.setUserId(user.getUserId());
        response.setUserName(user.getUserName());
        response.setUserFirstName(user.getUserFirstName());
        response.setUserLastName(user.getUserLastName());
        response.setUserEmail(user.getUserEmail());
        response.setCreatedAt(user.getCreatedAt().toLocalDate());

        return response;
    }

    // 2) findUserById
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sorry, we couldn't find user with ID: " +userId));
    }


 /*
    - SIGNATURE METHODS:
         * createUser
         * updateUser
         * deleteUser
         * getUserById
*/

    // 1) createUser
    @Override
    public UserResponse createUser (UserRequest request) {

        // Check if email is already in use
        if (userRepository.existsByUserEmail(request.getUserEmail())) {
            throw new DuplicateResourceException(
                    "Email already in use, please try another one.");
        }

        // Check if username is already in use
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new DuplicateResourceException(
                    "@username already in use, please try another one.");
        }

        User user = new User();

        user.setUserEmail(request.getUserEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUserName(request.getUserName());
        user.setUserFirstName(request.getUserFirstName());
        user.setUserLastName(request.getUserLastName());

        User savedUser = userRepository.save(user);

        return convertToResponse(savedUser);
    }


    // 2) updateUser
    @Override
    public UserResponse updateUser(Long userId, UserRequest request) {

        User user = findUserById(userId);


        // Check which field exactly does the user want to update,
        // this prevents the user from inputting all other fields again.

        if (request.getUserName() != null) {

            if (userRepository.existsByUserName(request.getUserName())) {
                throw new DuplicateResourceException(
                        "@username already in use, please try another one.");
            }

            user.setUserName(request.getUserName());
        }

        if (request.getUserFirstName() != null) {
            user.setUserFirstName(request.getUserFirstName());
        }

        if (request.getUserLastName() != null) {
            user.setUserLastName(request.getUserLastName());
        }

        if (request.getUserEmail() != null) {

            if (userRepository.existsByUserEmail(request.getUserEmail())) {
                throw new DuplicateResourceException(
                        "Email already in use, please try another one.");
            }

            user.setUserEmail(request.getUserEmail());
        }

        // Temporary: password changes move to a dedicated endpoint in a later phase
        if (request.getPassword() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }


    // 3) deleteUser
    @Override
    public String deleteUser(Long userId) {

        User user = findUserById(userId);
        userRepository.delete(user);

        return "User deleted successfully!";
    }


    // 4) getUserById
    @Override
    public UserResponse getUserById(Long userId) {

        User user = findUserById(userId);
        return convertToResponse(user);
    }
}
