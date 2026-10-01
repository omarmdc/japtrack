package com.japtrack.project.security.handler;

import com.japtrack.project.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

// Returns 403 as JSON. Also handles requests rejected for a missing or invalid CSRF token.
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public JsonAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String message = accessDeniedException instanceof CsrfException
                ? "Invalid or missing CSRF token"
                : "Access denied";

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), ApiError.of(HttpStatus.FORBIDDEN.value(), message));
    }
}
