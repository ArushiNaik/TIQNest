package com.konate.ithelpdesk.remediation.BusinessLayer;

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

    private final RemediationCatalog catalog;
    private final RemediationMatcher matcher;
    private final RemediationAttemptRepository attemptRepository;
    private final RemediationMapper mapper;
    private final TicketService ticketService;

    public RemediationService(RemediationCatalog catalog, RemediationMatcher matcher,
                              RemediationAttemptRepository attemptRepository, RemediationMapper mapper,
                              TicketService ticketService) {
        this.catalog = catalog;
        this.matcher = matcher;
        this.attemptRepository = attemptRepository;
        this.mapper = mapper;
        this.ticketService = ticketService;
    }

    public List<RemedyResponse> catalog() {
        return catalog.all().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RecommendationResponse recommend(RecommendationRequest req) {
        boolean hasManualInput = req.issueType() != null || req.category() != null
                || hasText(req.affectedSystem()) || hasText(req.description());
        if (req.ticketId() == null && !hasManualInput) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide a ticketId, an issue type, a category, or a description of the problem");
        }

        // Reuse the ticket module's public service: 404 if the ticket does not exist.
        TicketResponse ticket = req.ticketId() == null ? null : ticketService.findById(req.ticketId());

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
                recs);
    }

    @Transactional
    public RemediationAttemptResponse recordAttempt(RemediationAttemptRequest req, String reportedBy) {
        if (!catalog.isApproved(req.remedyId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown remedy '" + req.remedyId() + "'. Only approved TiqFix remedies can be recorded.");
        }
        if (req.ticketId() != null) {
            ticketService.findById(req.ticketId()); // 404 if the ticket does not exist
        }

        RemediationAttempt saved = attemptRepository.save(RemediationAttempt.builder()
                .remedyId(req.remedyId())
                .ticketId(req.ticketId())
                .outcome(req.outcome())
                .feedback(clean(req.feedback()))
                .reportedBy(reportedBy)
                .build());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RemediationAttemptResponse> history(Long ticketId, String reportedBy) {
        List<RemediationAttempt> attempts;
        if (ticketId != null) {
            attempts = attemptRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
        } else if (reportedBy != null) {
            attempts = attemptRepository.findTop20ByReportedByIgnoreCaseOrderByCreatedAtDesc(reportedBy);
        } else {
            attempts = attemptRepository.findTop20ByOrderByCreatedAtDesc();
        }
        return mapper.toAttemptResponses(attempts);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String joinText(String... parts) {
        return Stream.of(parts).filter(Objects::nonNull).collect(Collectors.joining(" "));
    }

    /** Strip control characters (keeping newlines/tabs); blank becomes null. */
    private static String clean(String feedback) {
        if (feedback == null) return null;
        String s = feedback.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").trim();
        return s.isEmpty() ? null : s;
    }
}
