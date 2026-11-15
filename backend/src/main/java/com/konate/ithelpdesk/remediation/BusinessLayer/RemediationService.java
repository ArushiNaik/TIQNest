package com.konate.ithelpdesk.remediation.BusinessLayer;

import com.konate.ithelpdesk.config.NonITIssueException;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher.MatchInput;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher.RemedyMatch;
import com.konate.ithelpdesk.remediation.DataLayer.RemediationAttempt;
import com.konate.ithelpdesk.remediation.DataLayer.RemediationAttemptRepository;
import com.konate.ithelpdesk.remediation.Mapper.RemediationMapper;
import com.konate.ithelpdesk.remediation.PresentationLayer.RecommendationRequest;
import com.konate.ithelpdesk.remediation.PresentationLayer.RecommendationResponse;
import com.konate.ithelpdesk.remediation.PresentationLayer.RemediationAttemptRequest;
import com.konate.ithelpdesk.remediation.PresentationLayer.RemediationAttemptResponse;
import com.konate.ithelpdesk.remediation.PresentationLayer.RemedyResponse;
import com.konate.ithelpdesk.ticket.BusinessLayer.TicketService;
import com.konate.ithelpdesk.ticket.PresentationLayer.TicketResponse;
import com.konate.ithelpdesk.triage.BusinessLayer.AiScopeClassifier;
import com.konate.ithelpdesk.triage.BusinessLayer.ScopeResult;
import com.konate.ithelpdesk.user.DataLayer.User;
import com.konate.ithelpdesk.user.config.AccessControl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RemediationService {

    static final String SECURITY_MESSAGE = "This looks like a security problem. TiqFix does not offer "
            + "self-service actions for security incidents. Contact IT security now and create a high-priority ticket.";
    static final String UNSUPPORTED_MESSAGE = "This is an IT problem, but there is no approved self-service fix "
            + "for it. Please create a support ticket so a technician can help.";
    static final String CLARIFY_MESSAGE = "TiqFix needs a little more detail before it can suggest a safe fix.";

    private final RemediationCatalog catalog;
    private final RemediationMatcher matcher;
    private final RemediationAttemptRepository attemptRepository;
    private final RemediationMapper mapper;
    private final TicketService ticketService;
    private final AiScopeClassifier scopeClassifier;

    public RemediationService(RemediationCatalog catalog, RemediationMatcher matcher,
                              RemediationAttemptRepository attemptRepository, RemediationMapper mapper,
                              TicketService ticketService, AiScopeClassifier scopeClassifier) {
        this.catalog = catalog;
        this.matcher = matcher;
        this.attemptRepository = attemptRepository;
        this.mapper = mapper;
        this.ticketService = ticketService;
        this.scopeClassifier = scopeClassifier;
    }

    public List<RemedyResponse> catalog() {
        return catalog.all().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RecommendationResponse recommend(RecommendationRequest req, User user) {
        AccessControl.requireUser(user);
        boolean hasManualInput = req.issueType() != null || req.category() != null
                || hasText(req.affectedSystem()) || hasText(req.description());
        if (req.ticketId() == null && !hasManualInput) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide a ticketId, an issue type, a category, or a description of the problem");
        }

        // 404 if missing, 403 if the ticket belongs to someone else.
        TicketResponse ticket = req.ticketId() == null ? null : ticketService.findByIdFor(req.ticketId(), user);

        // Free text is checked against IT scope. A selected issue type never overrides clear evidence.
        if (ticket == null && hasText(req.description())) {
            ScopeResult scope = scopeClassifier.classifyForGate(joinText(req.description(), req.affectedSystem()));
            switch (scope.classification()) {
                case NON_IT -> throw new NonITIssueException();
                case SECURITY_RISK -> {
                    return escalation(scope, SECURITY_MESSAGE, null);
                }
                case IT_UNSUPPORTED -> {
                    return escalation(scope, UNSUPPORTED_MESSAGE, null);
                }
                case UNCERTAIN, INSUFFICIENT_INFO -> {
                    if (req.issueType() == null || req.issueType() == IssueType.OTHER) {
                        return escalation(scope, CLARIFY_MESSAGE, scope.clarifyingQuestion());
                    }
                }
                default -> {
                }
            }
        }

        String text = ticket != null
                ? joinText(ticket.title(), ticket.description(), ticket.affectedSystem(), req.description())
                : joinText(req.description(), req.affectedSystem());
        MatchInput input = new MatchInput(req.issueType(),
                ticket != null ? ticket.category() : req.category(), text);

        List<RemedyMatch> matches = matcher.match(input);
        RemedyMatch best = matches.getFirst();
        boolean approved = best.remedy().primaryActionType() != RemedyActionType.ESCALATION;

        List<RemedyResponse> recs = matches.stream()
                .map(m -> mapper.toResponse(m.remedy(), m.reason(), Math.min(99, m.score())))
                .toList();

        return new RecommendationResponse(
                ticket != null ? ticket.id() : null,
                ticket != null ? ticket.title() : null,
                best.remedy().issueType(),
                approved,
                approved ? "Recommended fix: " + best.remedy().title() : catalog.escalation().summary(),
                recs,
                approved ? "IT_SUPPORTED" : null,
                null);
    }

    private RecommendationResponse escalation(ScopeResult scope, String message, String question) {
        String reason = scope.reason() != null ? scope.reason() : message;
        return new RecommendationResponse(null, null, IssueType.OTHER, false, message,
                List.of(mapper.toResponse(catalog.escalation(), reason, null)),
                scope.classification().name(), question);
    }

    @Transactional
    public RemediationAttemptResponse recordAttempt(RemediationAttemptRequest req, User user) {
        AccessControl.requireUser(user);
        if (!catalog.isApproved(req.remedyId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown remedy '" + req.remedyId() + "'. Only approved TiqFix remedies can be recorded.");
        }
        if (req.ticketId() != null) {
            ticketService.findByIdFor(req.ticketId(), user); // 404 / 403
        }

        RemediationAttempt saved = attemptRepository.save(RemediationAttempt.builder()
                .remedyId(req.remedyId())
                .ticketId(req.ticketId())
                .outcome(req.outcome())
                .feedback(clean(req.feedback()))
                .reportedBy(user.getEmail())
                .build());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RemediationAttemptResponse> history(Long ticketId, User user) {
        AccessControl.requireUser(user);
        List<RemediationAttempt> attempts;
        if (ticketId != null) {
            ticketService.findByIdFor(ticketId, user); // only the requester or an admin
            attempts = attemptRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
        } else if (AccessControl.isAdmin(user)) {
            attempts = attemptRepository.findTop20ByOrderByCreatedAtDesc();
        } else {
            attempts = attemptRepository.findTop20ByReportedByIgnoreCaseOrderByCreatedAtDesc(user.getEmail());
        }
        return mapper.toAttemptResponses(attempts);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String joinText(String... parts) {
        return Stream.of(parts).filter(Objects::nonNull).collect(Collectors.joining(" "));
    }

    private static String clean(String feedback) {
        if (feedback == null) return null;
        String s = feedback.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").trim();
        return s.isEmpty() ? null : s;
    }
}
