package com.konate.ithelpdesk.triage.BusinessLayer;

import com.konate.ithelpdesk.remediation.BusinessLayer.IssueType;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher.MatchInput;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher.RemedyMatch;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemedyActionType;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Deterministic IT-scope classification. Used on its own when Claude is not configured or fails,
 * as a fast pre-check, and as a safety net (security problems are always escalated).
 * When nothing matches it answers UNCERTAIN, never NON_IT.
 */
@Component
public class RuleBasedScopeClassifier {

    public static final String CLARIFY_SCREEN =
            "Do you mean your computer or monitor screen, or something else?";
    public static final String CLARIFY_MISSING =
            "Which device, application or system is affected, and what error message (if any) do you see?";
    public static final String CLARIFY_GENERIC =
            "Could you describe the technical problem: which device or application, and what goes wrong?";

    private static final List<String> SECURITY_TERMS = List.of(
            "phishing", "ransomware", "malware", "virus", "trojan", "spyware", "keylogger", "hacked", "hacker",
            "compromised", "breach", "stolen", "suspicious link", "suspicious email", "suspicious e-mail",
            "clicked a link", "clicked on a link", "infected", "data leak", "lost my laptop", "lost laptop",
            "lost my phone", "unknown login", "someone logged into", "someone accessed");

    private static final List<String> DAMAGE_TERMS = List.of(
            "damaged", "dead", "broken", "cracked", "smashed", "spilled", "spill", "water damage", "fried",
            "burnt", "burned", "won't turn on", "will not turn on", "doesn't turn on", "does not turn on");

    private static final List<String> HARDWARE_PARTS = List.of(
            "motherboard", "hard drive", "hard disk", "hdd", "ssd", "laptop", "computer", "monitor",
            "keyboard", "battery", "charger", "usb port", "cpu", "ram", "docking station", "webcam", "headset");

    private static final Map<String, TicketCategory> IT_TERMS = new LinkedHashMap<>();

    static {
        for (String t : List.of("outlook", "email", "e-mail", "inbox", "exchange", "calendar invite"))
            IT_TERMS.put(t, TicketCategory.EMAIL);
        for (String t : List.of("wifi", "wi-fi", "wireless", "internet", "network", "ethernet", "intranet", "ip address"))
            IT_TERMS.put(t, TicketCategory.NETWORK);
        for (String t : List.of("password", "account", "login", "log in", "logon", "sign in", "sign-in", "mfa",
                "2fa", "authenticat", "locked out", "access denied", "permission"))
            IT_TERMS.put(t, TicketCategory.ACCOUNT);
        for (String t : List.of("software", "install", "update", "application", "excel", "word document",
                "powerpoint", "teams", "sharepoint", "onedrive", "zoom", "citrix", "license", "crash", "freez",
                "error code", "error message", "blue screen", "bsod", "windows", "macos", "driver"))
            IT_TERMS.put(t, TicketCategory.SOFTWARE);
        for (String t : List.of("laptop", "computer", "desktop", "pc ", "monitor", "keyboard", "mouse", "webcam",
                "headset", "bluetooth", "usb", "docking", "battery", "charger", "hard drive"))
            IT_TERMS.put(t, TicketCategory.HARDWARE);
        for (String t : List.of("server", "database", "website", "browser", "app ", "system is down", "error"))
            IT_TERMS.put(t, TicketCategory.OTHER);
    }

    private static final List<String> NON_IT_TERMS = List.of(
            "pizza", "sandwich", "coffee", "recipe", "cook ", "cooking", "pasta", "lunch", "dinner", "restaurant", "food",
            "poem", "joke", "story", "song", "lyrics", "weather", "forecast", "capital of", "football", "soccer",
            "basketball", "who won", "score of the", "birthday", "party", "vacation", "holiday plan", "movie",
            "horoscope", "chair", "plant", "parking spot", "homework", "girlfriend", "boyfriend");

    private static final List<String> VAGUE_PHRASES = List.of(
            "it doesn't work", "it does not work", "doesn't work", "not working", "it's broken", "help",
            "problem", "issue", "something is wrong", "fix it", "urgent");

    private final RemediationMatcher remediationMatcher;

    public RuleBasedScopeClassifier(RemediationMatcher remediationMatcher) {
        this.remediationMatcher = remediationMatcher;
    }

    public ScopeResult classify(String rawText) {
        String text = normalize(rawText);
        if (text.isBlank()) {
            return result(ScopeClassification.INSUFFICIENT_INFO, 0.9, "No description was provided.",
                    CLARIFY_MISSING, null, null);
        }

        Optional<String> security = firstMatch(text, SECURITY_TERMS);
        if (security.isPresent()) {
            return result(ScopeClassification.SECURITY_RISK, 0.9,
                    "Mentions \"" + security.get() + "\", a possible security incident that IT must handle immediately.",
                    null, null, TicketCategory.OTHER);
        }

        Optional<String> part = firstMatch(text, HARDWARE_PARTS);
        Optional<String> damage = firstMatch(text, DAMAGE_TERMS);
        if (part.isPresent() && damage.isPresent()) {
            return result(ScopeClassification.IT_UNSUPPORTED, 0.8,
                    "Physical damage to IT hardware (\"" + part.get() + "\") needs a technician; there is no self-service fix.",
                    null, null, TicketCategory.HARDWARE);
        }

        RemedyMatch match = remediationMatcher.match(new MatchInput(null, null, rawText)).getFirst();
        if (match.remedy().primaryActionType() != RemedyActionType.ESCALATION) {
            IssueType issue = match.remedy().issueType();
            return result(ScopeClassification.IT_SUPPORTED, 0.85,
                    "Matches a known IT problem: " + match.remedy().title() + ".", null, issue, categoryOf(issue));
        }

        Optional<Map.Entry<String, TicketCategory>> itTerm = IT_TERMS.entrySet().stream()
                .filter(e -> containsTerm(text, e.getKey())).findFirst();
        if (itTerm.isPresent()) {
            return result(ScopeClassification.IT_SUPPORTED, 0.7,
                    "Mentions \"" + itTerm.get().getKey().trim() + "\", an IT system.", null, null, itTerm.get().getValue());
        }

        Optional<String> nonIt = firstMatch(text, NON_IT_TERMS);
        if (nonIt.isPresent()) {
            return result(ScopeClassification.NON_IT, 0.9,
                    "Mentions \"" + nonIt.get() + "\" and no computer, software, account or network problem.",
                    null, null, null);
        }

        if (containsTerm(text, "screen")) {
            return result(ScopeClassification.UNCERTAIN, 0.6, "\"Screen\" could mean a computer screen or something else.",
                    CLARIFY_SCREEN, null, TicketCategory.HARDWARE);
        }

        long words = text.split("\\s+").length;
        if (words <= 4 || firstMatch(text, VAGUE_PHRASES).isPresent() && words <= 8) {
            return result(ScopeClassification.INSUFFICIENT_INFO, 0.7, "The description is too short to identify the problem.",
                    CLARIFY_MISSING, null, null);
        }

        return result(ScopeClassification.UNCERTAIN, 0.4, "No clear IT system or problem could be identified.",
                CLARIFY_GENERIC, null, null);
    }

    public static TicketCategory categoryOf(IssueType issue) {
        if (issue == null) return null;
        return switch (issue) {
            case VPN -> TicketCategory.VPN;
            case PRINTER -> TicketCategory.PRINTER;
            case DNS -> TicketCategory.NETWORK;
            case BROWSER_CACHE -> TicketCategory.SOFTWARE;
            case OTHER -> TicketCategory.OTHER;
        };
    }

    private static ScopeResult result(ScopeClassification c, double confidence, String reason, String question,
                                      IssueType issue, TicketCategory category) {
        return ScopeResult.of(c, confidence, reason, question, issue, category, ScopeSource.RULES);
    }

    static String normalize(String raw) {
        if (raw == null) return "";
        return (" " + raw.toLowerCase(Locale.ROOT).replace('’', '\'').replaceAll("\\s+", " ") + " ")
                .replaceAll("\\bwont\\b", "won't").replaceAll("\\bdoesnt\\b", "doesn't");
    }

    private static Optional<String> firstMatch(String text, List<String> terms) {
        return terms.stream().filter(t -> containsTerm(text, t)).findFirst();
    }

    private static boolean containsTerm(String text, String term) {
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(term)).matcher(text).find();
    }
}
