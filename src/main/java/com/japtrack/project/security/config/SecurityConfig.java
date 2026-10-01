package com.japtrack.project.security.config;

import com.japtrack.project.security.handler.JsonAccessDeniedHandler;
import com.japtrack.project.security.handler.JsonAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfig {

    // Stores hashes as "{bcrypt}...", so the algorithm can be upgraded later without breaking existing passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JsonAuthenticationEntryPoint authenticationEntryPoint,
                                                   JsonAccessDeniedHandler accessDeniedHandler) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .anyRequest().authenticated())

                // Spring Security's login filter handles the session: it changes the session ID,
                // rotates the CSRF token and saves the SecurityContext in the HTTP session.
                // Expects a form-encoded POST with "username" (email or username) and "password".
                // No permitAll() needed: the login filter answers before authorization rules are checked,
                // and leaving it out avoids opening Spring's unused default "/login" page URL.
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler(new AuthenticationEntryPointFailureHandler(authenticationEntryPoint)))

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies("JSESSIONID"))

                .httpBasic(AbstractHttpConfigurer::disable)

                // XSRF-TOKEN cookie readable by the frontend, sent back in the X-XSRF-TOKEN header
                .csrf(csrf -> csrf.spa())

                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId()))

                // The SPA handles its own navigation, so there is no "redirect back after login" to remember.
                // This also stops unauthenticated API calls from creating sessions.
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
