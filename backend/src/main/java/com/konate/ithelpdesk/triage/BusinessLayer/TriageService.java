package com.konate.ithelpdesk.triage.BusinessLayer;

import com.konate.ithelpdesk.config.CodedApiException;
import com.konate.ithelpdesk.config.NonITIssueException;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationCatalog;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher.MatchInput;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher.RemedyMatch;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemedyActionType;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import com.konate.ithelpdesk.ticket.DataTier.TicketPriority;
import com.konate.ithelpdesk.triage.AiLayer.AiTriageOutput;
import com.konate.ithelpdesk.triage.AiLayer.AiUnavailableException;
import com.konate.ithelpdesk.triage.AiLayer.ClaudeClient;
import com.konate.ithelpdesk.triage.AiLayer.ValidatedImage;
import com.konate.ithelpdesk.triage.PresentationLayer.ClassifyResponse;
import com.konate.ithelpdesk.triage.PresentationLayer.TriageAnalysisResponse;
import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.config.AccessControl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TiqVision: classify text and analyze screenshots. Creates nothing; the employee must confirm a
 * ticket separately. Uploaded images live only in memory for the duration of the request.
 */
@Service
public class TriageService {

    private static final Pattern ERROR_CODE = Pattern.compile("^[A-Za-z0-9#:._\\- ]{1,64}$");
    private static final int MAX_DESCRIPTION = 2000;

    private final ClaudeClient claude;
    private final AiScopeClassifier scopeClassifier;
    private final RuleBasedScopeClassifier rules;
    private final ImageValidator imageValidator;
    private final TriageRateLimiter rateLimiter;
    private final RemediationCatalog catalog;
    private final RemediationMatcher matcher;
    private final boolean demoFixturesEnabled;

    public TriageService(ClaudeClient claude, AiScopeClassifier scopeClassifier, RuleBasedScopeClassifier rules,
                         ImageValidator imageValidator, TriageRateLimiter rateLimiter,
                         RemediationCatalog catalog, RemediationMatcher matcher,
                         @Value("${tiqnest.triage.demo-fixtures-enabled:false}") boolean demoFixturesEnabled) {
        this.claude = claude;
        this.scopeClassifier = scopeClassifier;
        this.rules = rules;
        this.imageValidator = imageValidator;
        this.rateLimiter = rateLimiter;
        this.catalog = catalog;
        this.matcher = matcher;
        this.demoFixturesEnabled = demoFixturesEnabled;
    }

    public boolean aiConfigured() {
        return claude.isConfigured();
    }

    public boolean demoFixturesEnabled() {
        return demoFixturesEnabled;
    }

    public ClassifyResponse classify(String description, User user) {
        AccessControl.requireUser(user);
        rateLimiter.check(user.getEmail());
        ScopeResult scope = scopeClassifier.classify(description);
        if (scope.classification() == ScopeClassification.NON_IT) throw new NonITIssueException();
        List<String> remedies = scope.classification() == ScopeClassification.IT_SUPPORTED
                ? matchRemedies(description) : List.of();
        return new ClassifyResponse(scope.classification(), scope.isITRelated(), scope.confidence(), scope.reason(),
                scope.clarifyingQuestion(), scope.issueType(), scope.category(), scope.source(),
                nextStep(scope.classification(), remedies), priorityFor(scope.classification()), remedies);
    }

    public TriageAnalysisResponse analyze(List<MultipartFile> files, String description, String demoScenario, User user) {
        AccessControl.requireUser(user);
        String desc = description == null ? "" : description.trim();
        if (desc.length() > MAX_DESCRIPTION) {
            throw new CodedApiException(HttpStatus.BAD_REQUEST, "DESCRIPTION_TOO_LONG",
                    "The description must be at most " + MAX_DESCRIPTION + " characters.");
        }
        List<ValidatedImage> images = imageValidator.validate(files);

        if (demoScenario != null && !demoScenario.isBlank()) {
            if (!demoFixturesEnabled) {
                throw new CodedApiException(HttpStatus.BAD_REQUEST, "DEMO_DISABLED",
                        "Demo fixtures are disabled on this server.");
            }
            return demoFixture(images.size());
        }

        rateLimiter.check(user.getEmail());
        AiTriageOutput output;
        try {
            output = claude.triage(desc, images);
        } catch (AiUnavailableException e) {
            throw unavailable(e);
        }
        return toResponse(output, desc, images.size());
    }

    /** Validates and normalizes raw AI output. Package-visible for tests. */
    TriageAnalysisResponse toResponse(AiTriageOutput out, String employeeText, int imageCount) {
        ScopeResult scope;
        try {
            scope = AiScopeClassifier.fromAi(out);
        } catch (AiUnavailableException e) {
            throw unavailable(e);
        }

        String title = AiScopeClassifier.clean(out.title(), 200);
        String description = AiScopeClassifier.clean(out.description(), MAX_DESCRIPTION);
        ScopeClassification classification = scope.classification();

        // Safety net: security wording anywhere means escalation, whatever the model said.
        ScopeResult ruleCheck = rules.classify(joined(employeeText, title, description));
        if (ruleCheck.classification() == ScopeClassification.SECURITY_RISK) {
            classification = ScopeClassification.SECURITY_RISK;
        } else if (classification == ScopeClassification.NON_IT && rules.classify(employeeText).isITRelated()) {
            classification = ScopeClassification.UNCERTAIN;
        }
        if (classification == ScopeClassification.NON_IT) throw new NonITIssueException();

        TicketCategory category = scope.category() != null ? scope.category()
                : ruleCheck.category() != null ? ruleCheck.category() : TicketCategory.OTHER;
        String system = AiScopeClassifier.clean(out.affectedSystem(), 100);
        if (system == null) system = defaultSystem(category);

        String code = AiScopeClassifier.clean(out.visibleErrorCode(), 64);
        if (code != null && !ERROR_CODE.matcher(code).matches()) code = null;

        List<String> remedies = classification == ScopeClassification.IT_SUPPORTED
                ? approvedRemedies(out.suggestedRemedyIds(), joined(employeeText, title, description))
                : List.of();

        List<String> evidence = out.evidence() == null ? List.of() : out.evidence().stream()
                .map(e -> AiScopeClassifier.clean(e, 300)).filter(Objects::nonNull).limit(5).toList();

        String question = classification == ScopeClassification.UNCERTAIN
                || classification == ScopeClassification.INSUFFICIENT_INFO
                ? Objects.requireNonNullElse(scope.clarifyingQuestion(), RuleBasedScopeClassifier.CLARIFY_GENERIC)
                : null;

        if (title == null) title = (classification == ScopeClassification.SECURITY_RISK
                ? "Possible security incident" : "IT issue") + " – " + system;

        return new TriageAnalysisResponse(
                classification, classification.isItRelated(), scope.confidence(), confidenceLabel(scope.confidence()),
                scope.reason(), title, description, category, system, code,
                classification == ScopeClassification.SECURITY_RISK ? null : AiScopeClassifier.clean(out.possibleCause(), 500),
                evidence, remedies, nextStep(classification, remedies), true, priorityFor(classification),
                question, out.promptInjectionDetected(), "CLAUDE", false, imageCount);
    }

    private List<String> approvedRemedies(List<String> aiIds, String text) {
        Set<String> ids = new LinkedHashSet<>();
        if (aiIds != null) {
            aiIds.stream().filter(Objects::nonNull).map(String::trim)
                    .filter(id -> catalog.isApproved(id) && !RemediationCatalog.ESCALATE.equals(id))
                    .limit(3).forEach(ids::add);
        }
        if (ids.isEmpty()) ids.addAll(matchRemedies(text));
        return new ArrayList<>(ids);
    }

    private List<String> matchRemedies(String text) {
        List<RemedyMatch> matches = matcher.match(new MatchInput(null, null, text));
        return matches.stream()
                .filter(m -> m.remedy().primaryActionType() != RemedyActionType.ESCALATION)
                .map(m -> m.remedy().id())
                .toList();
    }

    static NextStep nextStep(ScopeClassification c, List<String> remedies) {
        return switch (c) {
            case SECURITY_RISK -> NextStep.ESCALATE_SECURITY;
            case IT_SUPPORTED -> remedies.isEmpty() ? NextStep.CREATE_TICKET : NextStep.TRY_FIX;
            case IT_UNSUPPORTED -> NextStep.CREATE_TICKET;
            default -> NextStep.CLARIFY;
        };
    }

    static TicketPriority priorityFor(ScopeClassification c) {
        return c == ScopeClassification.SECURITY_RISK ? TicketPriority.HIGH : TicketPriority.MEDIUM;
    }

    static String confidenceLabel(double confidence) {
        if (confidence >= 0.75) return "HIGH";
        if (confidence >= 0.45) return "MEDIUM";
        return "LOW";
    }

    private static String defaultSystem(TicketCategory category) {
        return switch (category) {
            case VPN -> "VPN";
            case EMAIL -> "Outlook";
            case NETWORK -> "Network";
            case PRINTER -> "Printer";
            case ACCOUNT -> "User account";
            case SOFTWARE -> "Software";
            case HARDWARE -> "Hardware";
            case OTHER -> "Unknown";
        };
    }

    private static String joined(String... parts) {
        return Stream.of(parts).filter(Objects::nonNull).collect(Collectors.joining(" "));
    }

    private static CodedApiException unavailable(AiUnavailableException e) {
        return switch (e.getReason()) {
            case NOT_CONFIGURED -> new CodedApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_NOT_CONFIGURED",
                    "AI screenshot analysis is not configured on this server. Describe the problem in text or create a ticket instead.");
            case MALFORMED_OUTPUT -> new CodedApiException(HttpStatus.BAD_GATEWAY, "AI_BAD_RESPONSE",
                    "The AI returned an unusable result. Please try again or create a ticket manually.");
            case RATE_LIMITED -> new CodedApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                    "The AI service is busy right now. Please try again in a minute or create a ticket manually.");
            default -> new CodedApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE",
                    "AI analysis is temporarily unavailable. You can still describe the problem and create a ticket.");
        };
    }

    /** Clearly labelled presentation fixture. The uploaded image is NOT analyzed. */
    private TriageAnalysisResponse demoFixture(int imageCount) {
        return new TriageAnalysisResponse(
                ScopeClassification.IT_SUPPORTED, true, 0.9, "HIGH",
                "DEMO FIXTURE – this result is simulated; the uploaded image was not analyzed.",
                "Company VPN connection failure", "Demo data: a VPN client error dialog reporting a failed connection.",
                TicketCategory.VPN, "VPN", null, "The VPN server may not be reachable from this network.",
                List.of("Demo data – not derived from the uploaded image"),
                List.of(RemediationCatalog.VPN_SETTINGS), NextStep.TRY_FIX, true, TicketPriority.MEDIUM,
                null, false, "DEMO_FIXTURE", true, imageCount);
    }
}
