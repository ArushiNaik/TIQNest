package com.konate.ithelpdesk.remediation.BusinessLayer;

public enum RemedyActionType {
    /** Opens an allowlisted Windows Settings page (ms-settings: URI). Changes nothing by itself. */
    SETTINGS_SHORTCUT,
    /** Step-by-step instructions the user follows. */
    GUIDED_TROUBLESHOOTING,
    /** Read-only command the user may copy and run themselves. */
    DIAGNOSTIC_COMMAND,
    /** Reviewed, low-impact repair command the user copies and runs themselves after confirming. */
    REPAIR_COMMAND,
    /** Hand the problem to a human technician. */
    ESCALATION
}
