package com.konate.ithelpdesk.triage.BusinessLayer;

public enum ScopeClassification {
    /** A real IT issue Tiqnest can analyze and possibly fix with an approved remedy. */
    IT_SUPPORTED,
    /** IT-related, but no approved self-service remedy exists (e.g. physical damage). */
    IT_UNSUPPORTED,
    /** Clearly unrelated to IT support. */
    NON_IT,
    /** Possibly IT-related, but ambiguous. Ask one clarifying question. */
    UNCERTAIN,
    /** A legitimate IT security problem that must be escalated immediately. */
    SECURITY_RISK,
    /** Too little information to classify. */
    INSUFFICIENT_INFO;

    public boolean isItRelated() {
        return this == IT_SUPPORTED || this == IT_UNSUPPORTED || this == SECURITY_RISK;
    }
}
