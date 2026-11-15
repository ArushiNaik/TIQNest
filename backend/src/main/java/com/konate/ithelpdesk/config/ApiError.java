package com.konate.ithelpdesk.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors,
        String code,
        List<String> suggestions,
        Boolean canRequestAdminReview
) {
    public ApiError(Instant timestamp, int status, String error, String message, String path,
                    Map<String, String> fieldErrors) {
        this(timestamp, status, error, message, path, fieldErrors, null, null, null);
    }
}
