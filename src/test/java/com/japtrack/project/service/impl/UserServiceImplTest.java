package com.japtrack.project.service.impl;

import com.japtrack.project.dto.request.UserRequest;
import com.japtrack.project.dto.response.UserResponse;
import com.japtrack.project.entity.User;
import com.japtrack.project.exception.custom.DuplicateResourceException;
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

    private UserRequest newUserRequest() {
        UserRequest request = new UserRequest();
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
    void updateUserHashesNewPassword() {
        User existing = withDatabaseFields(new User());
        existing.setPasswordHash(passwordEncoder.encode("old-password"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRequest request = new UserRequest();
        request.setPassword(RAW_PASSWORD);
        userService.updateUser(1L, request);

        assertThat(existing.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(RAW_PASSWORD, existing.getPasswordHash())).isTrue();
    }
}
