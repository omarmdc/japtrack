package com.japtrack.project.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.util.Map;

// One JSON error shape for the whole API. fieldErrors is only included for validation failures.
@JsonPropertyOrder({"status", "message", "timestamp", "fieldErrors"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, Instant timestamp, Map<String, String> fieldErrors) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, Instant.now(), null);
    }
}
