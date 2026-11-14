package com.konate.ithelpdesk.remediation.BusinessLayer;

import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class RemediationCatalog {

    public static final String VPN_SETTINGS = "vpn-settings";
    public static final String PRINTER_CHECK = "printer-check";
    public static final String DNS_FLUSH = "dns-flush";
    public static final String BROWSER_CACHE = "browser-cache";
    public static final String ESCALATE = "escalate-to-it";

    private static final Pattern MS_SETTINGS_URI = Pattern.compile("^ms-settings:[a-z0-9-]+$");

    private final Map<String, Remedy> remedies;
    private final List<Remedy> ordered;

    public RemediationCatalog() {
        Map<String, Remedy> map = new LinkedHashMap<>();
        for (Remedy r : List.of(vpn(), printer(), dns(), browserCache(), buildEscalation())) {
            validate(r);
            map.put(r.id(), r);
        }
        this.remedies = Map.copyOf(map);
        this.ordered = List.copyOf(map.values());
    }

    public List<Remedy> all() {
        return ordered;
    }

    public Optional<Remedy> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(remedies.get(id));
    }

    public boolean isApproved(String id) {
        return id != null && remedies.containsKey(id);
    }

    public Remedy escalation() {
        return remedies.get(ESCALATE);
    }

    private static Remedy vpn() {
        return new Remedy(
                VPN_SETTINGS,
                IssueType.VPN,
                "Check Windows VPN Settings",
                "Open Windows VPN settings to inspect your connection and reconnect.",
                "Windows 10 / 11",
                SafetyLevel.SAFE,
                RemedyActionType.SETTINGS_SHORTCUT,
                List.of(RemedyAction.settings("open-vpn-settings", "Open VPN Settings", "ms-settings:network-vpn",
                        "Opens Settings > Network & internet > VPN, where your company VPN connection is listed.")),
                List.of(
                        "Make sure your normal internet (Wi-Fi or Ethernet) works first - the VPN cannot connect without it.",
                        "Click \"Open VPN Settings\". If your browser asks to open Settings, choose Open.",
                        "Find your company VPN connection in the list and check its status.",
                        "If it says Disconnected, select it and choose Connect. Sign in again if prompted.",
                        "If it keeps dropping, choose Disconnect, wait about 10 seconds, then Connect again."),
                "Your VPN shows \"Connected\" and internal sites and shared drives load again.",
                List.of("Do not delete or edit the VPN profile - IT configures it for you."),
                true,
                Set.of(TicketCategory.VPN),
                Map.ofEntries(
                        Map.entry("vpn", 30),
                        Map.entry("anyconnect", 25),
                        Map.entry("globalprotect", 25),
                        Map.entry("forticlient", 25),
                        Map.entry("remote access", 15),
                        Map.entry("tunnel", 10),
                        Map.entry("disconnect", 5)));
    }

    private static Remedy printer() {
        return new Remedy(
                PRINTER_CHECK,
                IssueType.PRINTER,
                "Check Printer Status & Print Spooler",
                "Open Printers & scanners to see whether your printer is online and clear your stuck jobs. "
                        + "Optionally run a read-only check of the Windows Print Spooler service.",
                "Windows 10 / 11",
                SafetyLevel.SAFE,
                RemedyActionType.SETTINGS_SHORTCUT,
                List.of(
                        RemedyAction.settings("open-printer-settings", "Open Printers & Scanners", "ms-settings:printers",
                                "Opens Settings > Bluetooth & devices > Printers & scanners."),
                        RemedyAction.command("spooler-diagnostic", RemedyActionType.DIAGNOSTIC_COMMAND,
                                "Copy Diagnostic Command", "Get-Service -Name Spooler", "Windows PowerShell",
                                "Read-only check: shows whether the Windows Print Spooler service (which manages print jobs) "
                                        + "is Running or Stopped.",
                                "No changes are made to your computer. It only displays the service status.")),
                List.of(
                        "Check the printer itself: it is switched on, has paper, and shows no error light.",
                        "Click \"Open Printers & Scanners\" and select your printer.",
                        "If it shows Offline or Error, note the message. Choose \"Open print queue\" and cancel only your own stuck jobs.",
                        "Optional diagnostic: click \"Copy Diagnostic Command\", open Start, type PowerShell, open Windows PowerShell, paste (Ctrl+V) and press Enter.",
                        "If the Status column says Stopped, report it to IT - do not restart system services yourself."),
                "The printer shows Ready/Idle and your document prints - or you know the Spooler status to share with IT.",
                List.of("TiqFix never restarts services. The diagnostic command is read-only."),
                true,
                Set.of(TicketCategory.PRINTER),
                Map.ofEntries(
                        Map.entry("printer", 30),
                        Map.entry("print", 20),
                        Map.entry("spooler", 30),
                        Map.entry("print queue", 30),
                        Map.entry("toner", 10),
                        Map.entry("scanner", 10)));
    }

    private static Remedy dns() {
        return new Remedy(
                DNS_FLUSH,
                IssueType.DNS,
                "Clear the DNS Cache",
                "Clear your computer's saved list of website addresses so it looks them up fresh.",
                "Windows 10 / 11",
                SafetyLevel.LOW_RISK,
                RemedyActionType.REPAIR_COMMAND,
                List.of(
                        RemedyAction.command("flush-dns", RemedyActionType.REPAIR_COMMAND,
                                "Copy Repair Command", "ipconfig /flushdns", "Command Prompt or Windows PowerShell",
                                "Clears the DNS resolver cache - the list of recently looked-up website addresses - "
                                        + "so outdated or broken entries are discarded.",
                                "Temporary and safe: the first visit to each website may take a moment longer. "
                                        + "No files, passwords, or settings are changed. Administrator rights are not required."),
                        RemedyAction.settings("open-network-status", "Open Network Status", "ms-settings:network-status",
                                "Opens Settings > Network & internet to confirm you are connected.")),
                List.of(
                        "Confirm you are connected to the network (use \"Open Network Status\" if unsure).",
                        "Click \"Copy Repair Command\". TiqFix cannot run it for you - browsers cannot run commands on your computer.",
                        "Open Start, type cmd, and open Command Prompt.",
                        "Paste the command (Ctrl+V or right-click), check it reads exactly: ipconfig /flushdns, then press Enter.",
                        "You should see \"Successfully flushed the DNS Resolver Cache.\" Close and reopen your browser, then try the site again."),
                "Windows reports \"Successfully flushed the DNS Resolver Cache.\" and the website loads again.",
                List.of("Only run the command if it matches exactly what is shown here.",
                        "If many people cannot reach the same site, it is likely an outage - contact IT instead."),
                true,
                Set.of(TicketCategory.NETWORK),
                Map.ofEntries(
                        Map.entry("dns", 30),
                        Map.entry("flushdns", 30),
                        Map.entry("err_name_not_resolved", 30),
                        Map.entry("name resolution", 25),
                        Map.entry("server not found", 20),
                        Map.entry("can't be reached", 20),
                        Map.entry("cannot be reached", 20),
                        Map.entry("website won't load", 20),
                        Map.entry("websites won't load", 20),
                        Map.entry("site won't load", 20),
                        Map.entry("sites won't load", 20),
                        Map.entry("won't load", 10),
                        Map.entry("will not load", 10),
                        Map.entry("resolve", 15),
                        Map.entry("no internet", 10),
                        Map.entry("intranet", 10)));
    }

    private static Remedy browserCache() {
        return new Remedy(
                BROWSER_CACHE,
                IssueType.BROWSER_CACHE,
                "Clear Cached Images and Files",
                "Remove only your browser's cached files so pages load fresh, without touching passwords or cookies.",
                "Chrome, Edge, Firefox on Windows",
                SafetyLevel.LOW_RISK,
                RemedyActionType.GUIDED_TROUBLESHOOTING,
                List.of(RemedyAction.simple("view-browser-steps", RemedyActionType.GUIDED_TROUBLESHOOTING,
                        "View Instructions", "Shows the reviewed step-by-step instructions for your browser.")),
                List.of(
                        "Try a hard refresh first: press Ctrl+F5 (or Ctrl+Shift+R) on the page.",
                        "Still wrong? In Chrome or Edge press Ctrl+Shift+Delete to open \"Clear browsing data\".",
                        "Set the time range to \"Last 24 hours\" (choose \"All time\" only if the problem persists).",
                        "Tick ONLY \"Cached images and files\". Leave Cookies, Passwords, Browsing history and Autofill unticked.",
                        "Click \"Clear data\" and reload the page. (Firefox: Ctrl+Shift+Delete, tick only \"Temporary cached files and pages\", then Clear.)"),
                "The page loads with current content and displays correctly.",
                List.of("Do not clear cookies or saved passwords unless IT asks - it will sign you out of sites."),
                true,
                Set.of(),
                Map.ofEntries(
                        Map.entry("browser cache", 35),
                        Map.entry("clear cache", 25),
                        Map.entry("cached", 25),
                        Map.entry("old content", 25),
                        Map.entry("old version", 15),
                        Map.entry("outdated", 15),
                        Map.entry("stale", 15),
                        Map.entry("hard refresh", 20),
                        Map.entry("not loading correctly", 20),
                        Map.entry("displays incorrectly", 15),
                        Map.entry("browser", 15),
                        Map.entry("chrome", 10),
                        Map.entry("edge", 10),
                        Map.entry("firefox", 10)));
    }

    private static Remedy buildEscalation() {
        return new Remedy(
                ESCALATE,
                IssueType.OTHER,
                "Contact IT Support",
                "No approved automatic remedy is available for this problem. Please contact IT support.",
                "Any device",
                SafetyLevel.IT_ONLY,
                RemedyActionType.ESCALATION,
                List.of(RemedyAction.simple("escalate", RemedyActionType.ESCALATION, "Escalate to IT",
                        "Sends your problem to the IT team as a ticket so a technician can help.")),
                List.of(
                        "Describe what you were doing and what went wrong.",
                        "Include when it started and the exact text of any error message.",
                        "Do not try unreviewed fixes or scripts found online.",
                        "Click \"Escalate to IT\" - a technician will follow up."),
                "A technician reviews your problem and contacts you.",
                List.of("TiqFix never invents commands for problems it does not recognise."),
                false,
                Set.of(),
                Map.of());
    }

    private static void validate(Remedy r) {
        for (RemedyAction a : r.actions()) {
            if (a.type() == RemedyActionType.SETTINGS_SHORTCUT
                    && (a.value() == null || !MS_SETTINGS_URI.matcher(a.value()).matches())) {
                throw new IllegalStateException("Remedy " + r.id() + " has a non-allowlisted settings link");
            }
        }
    }
}
