package com.konate.ithelpdesk.triage.BusinessLayer;

import com.konate.ithelpdesk.remediation.BusinessLayer.IssueType;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import com.konate.ithelpdesk.triage.AiLayer.AiTriageOutput;
import com.konate.ithelpdesk.triage.AiLayer.AiUnavailableException;
import com.konate.ithelpdesk.triage.AiLayer.ClaudeClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Central IT-scope gate shared by triage, ticket creation and TiqFix.
 * <ul>
 *   <li>Security problems found by rules are always escalated, whatever the AI says.</li>
 *   <li>Claude's answer is validated field by field; invalid enums are never trusted.</li>
 *   <li>If Claude fails, the deterministic rules are used. An AI failure never turns an
 *       unknown problem into NON_IT: rules only say NON_IT for clearly unrelated requests.</li>
 * </ul>
 */
@Service
public class AiScopeClassifier {

    private static final String CLARIFY_MIXED =
            "It sounds like this may include an IT problem. Which device or application is affected, and what goes wrong?";

    private final RuleBasedScopeClassifier rules;
    private final ClaudeClient claude;

    public AiScopeClassifier(RuleBasedScopeClassifier rules, ClaudeClient claude) {
        this.rules = rules;
        this.claude = claude;
    }

    /** Full classification: asks Claude when configured. */
    public ScopeResult classify(String text) {
        ScopeResult ruleResult = rules.classify(text);
        if (ruleResult.classification() == ScopeClassification.SECURITY_RISK) return ruleResult;
        if (!claude.isConfigured()) return ruleResult;

        try {
            return reconcile(fromAi(claude.triage(text, List.of())), ruleResult);
        } catch (AiUnavailableException e) {
            return ruleResult.withSource(ScopeSource.RULES_AI_UNAVAILABLE);
        }
    }

    /**
     * Cheaper check used before creating tickets or recommending fixes: when the rules already
     * recognise a clear IT problem (or a security risk) Claude is not called.
     */
    public ScopeResult classifyForGate(String text) {
        ScopeResult ruleResult = rules.classify(text);
        return switch (ruleResult.classification()) {
            case SECURITY_RISK, IT_SUPPORTED, IT_UNSUPPORTED -> ruleResult;
            default -> classify(text);
        };
    }

    /** Validates a Claude result. Unknown classification values make the whole answer unusable. */
    public static ScopeResult fromAi(AiTriageOutput out) {
        ScopeClassification c = parseClassification(out == null ? null : out.classification());
        if (c == null) {
            throw new AiUnavailableException(AiUnavailableException.Reason.MALFORMED_OUTPUT,
                    "The AI returned an unknown classification");
        }
        String question = (c == ScopeClassification.UNCERTAIN || c == ScopeClassification.INSUFFICIENT_INFO)
                ? clean(out.clarifyingQuestion(), 300) : null;
        if (question == null && c == ScopeClassification.UNCERTAIN) question = RuleBasedScopeClassifier.CLARIFY_GENERIC;
        if (question == null && c == ScopeClassification.INSUFFICIENT_INFO) question = RuleBasedScopeClassifier.CLARIFY_MISSING;
        TicketCategory category = c == ScopeClassification.NON_IT ? null : parseCategory(out.category());
        return ScopeResult.of(c, clampConfidence(out.confidence()), clean(out.reason(), 300), question,
                null, category, ScopeSource.AI);
    }

    private static ScopeResult reconcile(ScopeResult ai, ScopeResult ruleResult) {
        // The AI says "not IT" but the rules see an IT system: do not reject a genuine problem, ask instead.
        if (ai.classification() == ScopeClassification.NON_IT && ruleResult.isITRelated()) {
            return ScopeResult.of(ScopeClassification.UNCERTAIN, 0.5,
                    "The request mixes an IT term with something unrelated.", CLARIFY_MIXED,
                    null, ruleResult.category(), ScopeSource.AI);
        }
        // The AI wants more detail, but the rules matched a concrete system with an approved fix
        // ("VPN is broken; also tell me a joke"): handle the genuine IT issue.
        if (ai.classification() == ScopeClassification.INSUFFICIENT_INFO
                && ruleResult.classification() == ScopeClassification.IT_SUPPORTED
                && ruleResult.issueType() != null) {
            return ruleResult.withSource(ScopeSource.AI);
        }
        IssueType issue = ruleResult.issueType();
        TicketCategory category = ai.category() != null ? ai.category() : ruleResult.category();
        return new ScopeResult(ai.classification(), ai.isITRelated(), ai.confidence(), ai.reason(),
                ai.clarifyingQuestion(), ai.isITRelated() ? issue : null, category, ScopeSource.AI);
    }

    public static ScopeClassification parseClassification(String value) {
        if (value == null) return null;
        try {
            return ScopeClassification.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static TicketCategory parseCategory(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return TicketCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static double clampConfidence(double value) {
        if (Double.isNaN(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }

    /** Trims, strips control characters and caps length; blank becomes null. */
    public static String clean(String value, int max) {
        if (value == null) return null;
        String s = value.replaceAll("[\\p{Cntrl}&&[^\\n]]", "").trim();
        if (s.isEmpty()) return null;
        return s.length() > max ? s.substring(0, max).trim() : s;
    }
}
