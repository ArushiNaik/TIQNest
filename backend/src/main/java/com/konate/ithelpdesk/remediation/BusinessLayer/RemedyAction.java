package com.konate.ithelpdesk.remediation.BusinessLayer;

/**
 * A single reviewed action inside a remedy. {@code value} is a fixed ms-settings URI or a fixed command
 * string defined in {@link RemediationCatalog}; it never comes from user input.
 */
public record RemedyAction(
        String id,
        RemedyActionType type,
        String label,
        String value,
        String shell,
        String explanation,
        String effects
) {
    public static RemedyAction settings(String id, String label, String uri, String explanation) {
        return new RemedyAction(id, RemedyActionType.SETTINGS_SHORTCUT, label, uri, null, explanation,
                "Only opens a Settings page. Nothing changes unless you choose to change it.");
    }

    public static RemedyAction command(String id, RemedyActionType type, String label, String command,
                                       String shell, String explanation, String effects) {
        return new RemedyAction(id, type, label, command, shell, explanation, effects);
    }

    public static RemedyAction simple(String id, RemedyActionType type, String label, String explanation) {
        return new RemedyAction(id, type, label, null, null, explanation, null);
    }
}
