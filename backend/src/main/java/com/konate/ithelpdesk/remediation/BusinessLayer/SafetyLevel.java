package com.konate.ithelpdesk.remediation.BusinessLayer;

public enum SafetyLevel {
    /** Read-only or navigation only: nothing on the computer changes. */
    SAFE,
    /** Small, reversible change the user performs themselves (e.g. clearing a cache). */
    LOW_RISK,
    /** Must be handled by IT; no self-service action is offered. */
    IT_ONLY
}
