package com.konate.ithelpdesk.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * The one error shape every endpoint returns, so the React app can always read
 * {status, error, message} and, for validation errors, fieldErrors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)   // fieldErrors is left out when there are none
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {}
