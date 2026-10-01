package com.japtrack.project.security.service;

import com.japtrack.project.entity.User;
import com.japtrack.project.repository.UserRepository;
import com.japtrack.project.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private MyUserDetailsService userDetailsService;
    private User user;

    @BeforeEach
    void setUp() {
        userDetailsService = new MyUserDetailsService(userRepository);

        user = new User();
        user.setUserId(7L);
        user.setUserName("jdoe");
        user.setUserEmail("jdoe@example.com");
        user.setPasswordHash("{bcrypt}hashed");
    }

    @Test
    void loadsUserByEmail() {
        when(userRepository.findByUserEmail("jdoe@example.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("jdoe@example.com");

        assertThat(details).isInstanceOf(AuthenticatedUser.class);
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) details;
        assertThat(authenticatedUser.getUserId()).isEqualTo(7L);
        assertThat(authenticatedUser.getUsername()).isEqualTo("jdoe@example.com");
        assertThat(authenticatedUser.getPassword()).isEqualTo("{bcrypt}hashed");
        assertThat(authenticatedUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        verify(userRepository, never()).findByUserName(anyString());
    }

    @Test
    void loadsUserByUsername() {
        when(userRepository.findByUserName("jdoe")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("jdoe");

        assertThat(((AuthenticatedUser) details).getUserId()).isEqualTo(7L);
        verify(userRepository, never()).findByUserEmail(anyString());
    }

    @Test
    void unknownEmailThrowsGenericUsernameNotFound() {
        when(userRepository.findByUserEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void unknownUsernameThrowsGenericUsernameNotFound() {
        when(userRepository.findByUserName("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Invalid credentials");
    }
}
