package com.japtrack.project.exception.handler;

import com.japtrack.project.dto.response.ApiError;
import com.japtrack.project.exception.custom.DuplicateResourceException;
import com.japtrack.project.exception.custom.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    // CUSTOMS

    // 1) Resource Not Found - 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> resourceNotFound (ResourceNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // 2) Duplicate Resource - 409
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> duplicatedResource (DuplicateResourceException e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }


    // CLIENT ERRORS - 400

    // 3) A @Valid request body failed validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validationFailed (MethodArgumentNotValidException e) {

        // One message per field (the first one, if a field breaks several rules)
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "Validation failed", Instant.now(), fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    // 4) Body is not valid JSON, or a value can't be converted (e.g. an unknown enum value or a bad date)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadableBody (HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body");
    }

    // 5) A path variable or request parameter has the wrong type (e.g. /api/users/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> wrongParameterType (MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'");
    }


    // GLOBAL HANDLER
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {

        // Spring MVC's own exceptions already carry the right status
        // (404 unknown route, 405 wrong method, 415 wrong content type, ...), so keep it
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return ResponseEntity.status(status)
                    .headers(errorResponse.getHeaders()) // e.g. the Allow header on a 405
                    .body(ApiError.of(status.value(), reasonPhrase(status)));
        }

        // Anything else is a genuine server error: log the details, but never send them to the client
        log.error("Unexpected error while handling request", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Sorry, something went wrong :(");
    }


    // HELPERS

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), message));
    }

    private String reasonPhrase(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known != null ? known.getReasonPhrase() : "Request failed";
    }
}
