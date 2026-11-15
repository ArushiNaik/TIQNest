package com.konate.ithelpdesk.config;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * An application error with a stable machine-readable {@code code} that the frontend can branch on.
 */
public class CodedApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<String> suggestions;
    private final Boolean canRequestAdminReview;

    public CodedApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null, null);
    }

    public CodedApiException(HttpStatus status, String code, String message,
                             List<String> suggestions, Boolean canRequestAdminReview) {
        super(message);
        this.status = status;
        this.code = code;
        this.suggestions = suggestions;
        this.canRequestAdminReview = canRequestAdminReview;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public Boolean getCanRequestAdminReview() {
        return canRequestAdminReview;
    }
}
