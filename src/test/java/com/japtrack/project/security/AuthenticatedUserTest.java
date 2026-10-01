package com.japtrack.project.security;

import com.japtrack.project.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    @Test
    void fromCopiesIdentityAndPasswordHashFromEntity() {
        User user = new User();
        user.setUserId(7L);
        user.setUserEmail("jdoe@example.com");
        user.setPasswordHash("{bcrypt}hashed");

        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);

        assertThat(authenticatedUser.getUserId()).isEqualTo(7L);
        assertThat(authenticatedUser.getEmail()).isEqualTo("jdoe@example.com");
        assertThat(authenticatedUser.getUsername()).isEqualTo("jdoe@example.com");
        assertThat(authenticatedUser.getPassword()).isEqualTo("{bcrypt}hashed");
        assertThat(authenticatedUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void eraseCredentialsClearsOnlyThePasswordHash() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(7L, "jdoe@example.com", "{bcrypt}hashed");

        authenticatedUser.eraseCredentials();

        assertThat(authenticatedUser.getPassword()).isNull();
        assertThat(authenticatedUser.getUserId()).isEqualTo(7L);
        assertThat(authenticatedUser.getUsername()).isEqualTo("jdoe@example.com");
        assertThat(authenticatedUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    // Verifies Spring Security itself erases the hash after a successful login,
    // using the same provider setup the login filter will use in Phase 2
    @Test
    void springSecurityErasesPasswordHashAfterSuccessfulAuthentication() {
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        AuthenticatedUser storedUser = new AuthenticatedUser(7L, "jdoe@example.com", passwordEncoder.encode("secret-password"));

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(username -> storedUser);
        provider.setPasswordEncoder(passwordEncoder);
        ProviderManager authenticationManager = new ProviderManager(provider);

        Authentication result = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("jdoe@example.com", "secret-password"));

        AuthenticatedUser principal = (AuthenticatedUser) result.getPrincipal();
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(principal.getUserId()).isEqualTo(7L);
        assertThat(principal.getPassword()).isNull();
        assertThat(result.getCredentials()).isNull();
    }
}
