package com.konate.ithelpdesk.triage.AiLayer;

/** The AI provider could not produce a usable answer (not configured, timeout, auth, refusal, bad output). */
public class AiUnavailableException extends RuntimeException {

    public enum Reason { NOT_CONFIGURED, TIMEOUT, AUTHENTICATION, RATE_LIMITED, PROVIDER_ERROR, REFUSED, MALFORMED_OUTPUT }

    private final Reason reason;

    public AiUnavailableException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AiUnavailableException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
