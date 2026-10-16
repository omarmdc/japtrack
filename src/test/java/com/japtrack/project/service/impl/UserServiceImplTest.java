package com.japtrack.project.service.impl;

import com.japtrack.project.dto.request.ChangePasswordRequest;
import com.japtrack.project.dto.request.RegisterRequest;
import com.japtrack.project.dto.request.UpdateUserRequest;
import com.japtrack.project.dto.response.UserResponse;
import com.japtrack.project.entity.User;
import com.japtrack.project.exception.custom.DuplicateResourceException;
import com.japtrack.project.exception.custom.IncorrectPasswordException;
import com.japtrack.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String RAW_PASSWORD = "correct-horse-battery";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    private RegisterRequest newUserRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUserName("jdoe");
        request.setUserEmail("jdoe@example.com");
        request.setUserFirstName("Jane");
        request.setUserLastName("Doe");
        request.setPassword(RAW_PASSWORD);
        return request;
    }

    // Simulates what the database fills in on insert
    private User withDatabaseFields(User user) {
        user.setUserId(1L);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void createUserStoresBcryptHashNotRawPassword() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> withDatabaseFields(invocation.getArgument(0)));

        userService.createUser(newUserRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String storedHash = captor.getValue().getPasswordHash();

        assertThat(storedHash).startsWith("{bcrypt}");
        assertThat(storedHash).doesNotContain(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, storedHash)).isTrue();
    }

    @Test
    void createUserResponseDoesNotExposePassword() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> withDatabaseFields(invocation.getArgument(0)));

        UserResponse response = userService.createUser(newUserRequest());

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUserName()).isEqualTo("jdoe");
        assertThat(response.toString()).doesNotContain(RAW_PASSWORD).doesNotContain("{bcrypt}");
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        when(userRepository.existsByUserEmail("jdoe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(newUserRequest()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserTrimsAndLowercasesEmail() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> withDatabaseFields(invocation.getArgument(0)));
        RegisterRequest request = newUserRequest();
        request.setUserEmail("  Jane.Doe@Example.COM ");

        UserResponse response = userService.createUser(request);

        verify(userRepository).existsByUserEmail("jane.doe@example.com");
        assertThat(response.getUserEmail()).isEqualTo("jane.doe@example.com");
    }

    @Test
    void createUserDuplicateEmailCheckIgnoresCase() {
        when(userRepository.existsByUserEmail("jdoe@example.com")).thenReturn(true);
        RegisterRequest request = newUserRequest();
        request.setUserEmail("JDoe@Example.com");

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        when(userRepository.existsByUserName("jdoe")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(newUserRequest()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateCurrentUserChecksDuplicatesOnlyAgainstOtherAccounts() {
        User existing = withDatabaseFields(new User());
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUserName("jdoe");
        request.setUserEmail(" JDoe@Example.com ");
        userService.updateCurrentUser(1L, request);

        // The checks exclude the current account (ID 1) and use the normalized email
        verify(userRepository).existsByUserNameAndUserIdNot("jdoe", 1L);
        verify(userRepository).existsByUserEmailAndUserIdNot("jdoe@example.com", 1L);
        assertThat(existing.getUserEmail()).isEqualTo("jdoe@example.com");
    }

    @Test
    void updateCurrentUserRejectsEmailOwnedByAnotherAccount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(withDatabaseFields(new User())));
        when(userRepository.existsByUserEmailAndUserIdNot("taken@example.com", 1L)).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUserEmail("taken@example.com");

        assertThatThrownBy(() -> userService.updateCurrentUser(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    private ChangePasswordRequest changePasswordRequest(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    @Test
    void changePasswordStoresANewHashWhenCurrentPasswordMatches() {
        User existing = withDatabaseFields(new User());
        existing.setPasswordHash(passwordEncoder.encode("old-password"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        userService.changePassword(1L, changePasswordRequest("old-password", RAW_PASSWORD));

        verify(userRepository).save(existing);
        assertThat(existing.getPasswordHash()).startsWith("{bcrypt}").doesNotContain(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, existing.getPasswordHash())).isTrue();
    }

    @Test
    void changePasswordRejectsWrongCurrentPasswordAndChangesNothing() {
        User existing = withDatabaseFields(new User());
        String originalHash = passwordEncoder.encode("old-password");
        existing.setPasswordHash(originalHash);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.changePassword(1L, changePasswordRequest("wrong-password", RAW_PASSWORD)))
                .isInstanceOf(IncorrectPasswordException.class)
                .hasMessage("Current password is incorrect");

        assertThat(existing.getPasswordHash()).isEqualTo(originalHash);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordRequestNeverPrintsPasswords() {
        assertThat(changePasswordRequest("old-password", RAW_PASSWORD).toString())
                .doesNotContain("old-password")
                .doesNotContain(RAW_PASSWORD);
    }

    @Test
    void registerRequestNeverPrintsThePassword() {
        String printed = newUserRequest().toString();

        assertThat(printed).doesNotContain(RAW_PASSWORD);
        // The other fields are still printed, so the object stays useful in logs
        assertThat(printed).contains("jdoe@example.com");
    }
}
