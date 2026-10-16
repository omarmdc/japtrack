package com.japtrack.project.service.impl;

import com.japtrack.project.dto.request.ChangePasswordRequest;
import com.japtrack.project.dto.request.RegisterRequest;
import com.japtrack.project.dto.request.UpdateUserRequest;
import com.japtrack.project.dto.response.UserResponse;
import com.japtrack.project.entity.User;
import com.japtrack.project.exception.custom.DuplicateResourceException;
import com.japtrack.project.exception.custom.IncorrectPasswordException;
import com.japtrack.project.exception.custom.ResourceNotFoundException;
import com.japtrack.project.repository.UserRepository;
import com.japtrack.project.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
         * getCurrentUser
         * updateCurrentUser
         * deleteCurrentUser
         * changePassword
*/

    // 1) createUser (registration)
    @Override
    public UserResponse createUser (RegisterRequest request) {

        String email = User.normalizeEmail(request.getUserEmail());

        // Check if email is already in use
        if (userRepository.existsByUserEmail(email)) {
            throw new DuplicateResourceException(
                    "Email already in use, please try another one.");
        }

        // Check if username is already in use
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new DuplicateResourceException(
                    "@username already in use, please try another one.");
        }

        User user = new User();

        user.setUserEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUserName(request.getUserName());
        user.setUserFirstName(request.getUserFirstName());
        user.setUserLastName(request.getUserLastName());

        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }


    // 2) getCurrentUser
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long currentUserId) {

        return convertToResponse(findUserById(currentUserId));
    }


    // 3) updateCurrentUser (profile fields only; the password is not part of UpdateUserRequest)
    @Override
    @Transactional
    public UserResponse updateCurrentUser(Long currentUserId, UpdateUserRequest request) {

        User user = findUserById(currentUserId);


        // Check which field exactly does the user want to update,
        // this prevents the user from inputting all other fields again.

        if (request.getUserName() != null) {

            // Only another account holding this username is a conflict; keeping your own is fine
            if (userRepository.existsByUserNameAndUserIdNot(request.getUserName(), currentUserId)) {
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

            // Normalize first, so "Jane@Example.com" is checked and stored the same way as "jane@example.com".
            // Login looks emails up in this normalized form too (MyUserDetailsService).
            String email = User.normalizeEmail(request.getUserEmail());

            if (userRepository.existsByUserEmailAndUserIdNot(email, currentUserId)) {
                throw new DuplicateResourceException(
                        "Email already in use, please try another one.");
            }

            user.setUserEmail(email);
        }

        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }


    // 4) deleteCurrentUser
    @Override
    @Transactional
    public void deleteCurrentUser(Long currentUserId) {

        User user = findUserById(currentUserId);

        // User.applications is mapped with CascadeType.ALL, so JPA deletes this user's applications too.
        // The transaction matters here: the applications list is loaded lazily during the delete, and with
        // open-in-view disabled that only works while a transaction (and its persistence context) is open.
        userRepository.delete(user);
    }


    // 5) changePassword
    @Override
    @Transactional
    public void changePassword(Long currentUserId, ChangePasswordRequest request) {

        // Load the stored hash from the database. The AuthenticatedUser in the session can't be used for this:
        // Spring Security erases its password hash right after login (eraseCredentials), so it's null by now.
        User user = findUserById(currentUserId);

        // A hash can't be compared with ==. matches() re-hashes the raw password using the salt (and algorithm,
        // via the {bcrypt} prefix) stored inside the hash, then compares the results.
        // Requiring the current password means a stolen or unattended session alone can't lock the owner out.
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IncorrectPasswordException("Current password is incorrect");
        }

        // Only the encoded hash is stored, and neither raw password is logged anywhere:
        // a database leak or a log file must never expose a usable password.
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
