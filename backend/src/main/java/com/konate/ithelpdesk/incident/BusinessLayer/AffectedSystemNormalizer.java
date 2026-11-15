package com.konate.ithelpdesk.incident.BusinessLayer;

import java.util.Locale;
import java.util.Set;

/**
 * Normalizes free-text affected-system names so that "VPN", "vpn client" and "GlobalProtect VPN"
 * count as the same system for outage detection, while different printers or apps stay separate.
 */
public final class AffectedSystemNormalizer {

    private static final Set<String> GENERIC = Set.of(
            "", "UNKNOWN", "OTHER", "NA", "N A", "NONE", "COMPUTER", "LAPTOP", "PC", "DESKTOP", "IT",
            "GENERAL", "MY COMPUTER", "MY LAPTOP", "DEVICE", "SYSTEM", "EVERYTHING", "SCREEN");

    private AffectedSystemNormalizer() {
    }

    /** Canonical key used for clustering, e.g. "VPN", "OUTLOOK", "WIFI", "HP 3F". */
    public static String key(String raw) {
        if (raw == null) return "";
        String s = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", " ").trim().replaceAll("\s+", " ");
        String padded = " " + s + " ";
        if (padded.contains(" VPN") || padded.contains("GLOBALPROTECT") || padded.contains("ANYCONNECT")
                || padded.contains("FORTICLIENT")) return "VPN";
        if (padded.contains(" OUTLOOK ") || padded.contains(" EXCHANGE ") || padded.equals(" EMAIL ")
                || padded.equals(" MAIL ") || padded.contains(" E MAIL ")) return "OUTLOOK";
        if (padded.contains(" WIFI ") || padded.contains(" WI FI ") || padded.contains(" WIRELESS ")
                || padded.contains(" WLAN ")) return "WIFI";
        return s;
    }

    /** True when the name is too vague to group reports from different people. */
    public static boolean isGeneric(String raw) {
        return GENERIC.contains(key(raw));
    }
}
