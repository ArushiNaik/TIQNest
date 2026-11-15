package com.konate.ithelpdesk.triage.BusinessLayer;

public enum ScopeSource {
    /** Claude classified the submission (validated server-side). */
    AI,
    /** Deterministic rules only (AI not configured, or not needed). */
    RULES,
    /** Claude was configured but failed; deterministic rules were used instead. */
    RULES_AI_UNAVAILABLE
}
