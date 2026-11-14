package com.konate.ithelpdesk.remediation.BusinessLayer;

import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deterministic, explainable matching of a problem description against the approved catalog.
 * No AI and no free-form generation: the output is always a subset of {@link RemediationCatalog}.
 */
@Component
public class RemediationMatcher {

    /** Minimum score for a remedy to be recommended. */
    static final int MIN_SCORE = 10;
    private static final int ISSUE_TYPE_SCORE = 100;
    private static final int CATEGORY_SCORE = 40;
    private static final int MAX_RESULTS = 3;

    /** Problems that must always go to a human, whatever else the text mentions. */
    private static final List<String> HIGH_RISK_TERMS = List.of(
            "virus", "malware", "ransomware", "phishing", "hacked", "compromised", "breach",
            "antivirus", "firewall", "defender", "bitlocker", "stolen", "data loss",
            "blue screen", "bsod", "locked out");

    private final RemediationCatalog catalog;

    public RemediationMatcher(RemediationCatalog catalog) {
        this.catalog = catalog;
    }

    public record MatchInput(IssueType issueType, TicketCategory category, String text) {}

    public record RemedyMatch(Remedy remedy, int score, String reason) {}

    /** Returns the matching remedies, best first. Falls back to the escalation remedy, never to a new action. */
    public List<RemedyMatch> match(MatchInput input) {
        String text = normalize(input.text());

        List<String> risky = HIGH_RISK_TERMS.stream().filter(t -> containsTerm(text, t)).toList();
        if (!risky.isEmpty()) {
            return List.of(new RemedyMatch(catalog.escalation(), 100,
                    "Your description mentions " + quoteList(risky)
                            + ", which may be a security or data issue. These are always handled by IT, not self-service."));
        }

        List<RemedyMatch> matches = new ArrayList<>();
        for (Remedy remedy : catalog.all()) {
            if (remedy.primaryActionType() == RemedyActionType.ESCALATION) continue;

            int score = 0;
            List<String> reasons = new ArrayList<>();

            if (input.issueType() != null && input.issueType() == remedy.issueType()) {
                score += ISSUE_TYPE_SCORE;
                reasons.add("you selected " + label(remedy.issueType()) + " problems");
            }
            if (input.category() != null && remedy.categories().contains(input.category())) {
                score += CATEGORY_SCORE;
                reasons.add("the ticket category is " + input.category().name());
            }

            List<String> hits = remedy.keywords().entrySet().stream()
                    .filter(e -> containsTerm(text, e.getKey()))
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .toList();
            for (String hit : hits) score += remedy.keywords().get(hit);
            if (!hits.isEmpty()) {
                reasons.add("the description mentions " + quoteList(hits.subList(0, Math.min(3, hits.size()))));
            }

            if (score >= MIN_SCORE) {
                matches.add(new RemedyMatch(remedy, score, capitalize(String.join(" and ", reasons)) + "."));
            }
        }

        if (matches.isEmpty()) {
            return List.of(new RemedyMatch(catalog.escalation(), 0,
                    "This problem does not match any reviewed self-service fix, so it should go to IT."));
        }

        return matches.stream()
                .sorted(Comparator.comparingInt(RemedyMatch::score).reversed())
                .limit(MAX_RESULTS)
                .toList();
    }

    static String normalize(String raw) {
        if (raw == null) return "";
        String s = raw.toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("\\s+", " ");
        s = s.replaceAll("\\bwont\\b", "won't").replaceAll("\\bcant\\b", "can't");
        return s.trim();
    }

    /** Term must start at a word boundary; suffixes are allowed ("print" matches "printing"). */
    static boolean containsTerm(String text, String term) {
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(term)).matcher(text).find();
    }

    private static String quoteList(List<String> terms) {
        return terms.stream().map(t -> "\"" + t + "\"").collect(Collectors.joining(", "));
    }

    private static String label(IssueType type) {
        return switch (type) {
            case VPN -> "VPN";
            case PRINTER -> "printer";
            case DNS -> "DNS / network";
            case BROWSER_CACHE -> "browser cache";
            case OTHER -> "other";
        };
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
