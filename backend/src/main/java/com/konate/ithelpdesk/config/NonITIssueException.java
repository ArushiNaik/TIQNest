package com.konate.ithelpdesk.config;

import org.springframework.http.HttpStatus;

import java.util.List;

/** Thrown when a submission is confidently outside IT support. Mapped to HTTP 422 NON_IT_ISSUE. */
public class NonITIssueException extends CodedApiException {

    public static final String CODE = "NON_IT_ISSUE";
    public static final String MESSAGE = "This doesn't appear to be an IT support issue. Tiqnest helps with "
            + "computers, software, accounts, networks, printers and other IT problems. "
            + "Please describe an IT issue instead.";
    public static final List<String> SUGGESTIONS = List.of(
            "Describe an IT-related problem",
            "Upload a screenshot of a technical error",
            "Request IT admin review");

    public NonITIssueException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, CODE, MESSAGE, SUGGESTIONS, true);
    }
}
