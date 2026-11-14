package com.konate.ithelpdesk.remediation.BusinessLayer;

/** Problem families TiqFix knows how to help with. OTHER always leads to escalation unless text matches. */
public enum IssueType {
    VPN, PRINTER, DNS, BROWSER_CACHE, OTHER
}
