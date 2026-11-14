package com.konate.ithelpdesk.remediation.Mapper;

import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationCatalog;
import com.konate.ithelpdesk.remediation.BusinessLayer.Remedy;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemedyAction;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemedyActionType;
import com.konate.ithelpdesk.remediation.DataLayer.RemediationAttempt;
import com.konate.ithelpdesk.remediation.PresentationLayer.RemediationAttemptResponse;
import com.konate.ithelpdesk.remediation.PresentationLayer.RemedyActionResponse;
import com.konate.ithelpdesk.remediation.PresentationLayer.RemedyResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/** Hand-written because remedies are catalog records (not entities) and attempt titles come from the catalog. */
@Component
public class RemediationMapper {

    private final RemediationCatalog catalog;

    public RemediationMapper(RemediationCatalog catalog) {
        this.catalog = catalog;
    }

    public RemedyResponse toResponse(Remedy remedy) {
        return toResponse(remedy, null, null);
    }

    public RemedyResponse toResponse(Remedy remedy, String matchReason, Integer confidence) {
        String deepLink = remedy.firstActionOf(RemedyActionType.SETTINGS_SHORTCUT).map(RemedyAction::value).orElse(null);
        String command = remedy.firstActionOf(RemedyActionType.REPAIR_COMMAND)
                .or(() -> remedy.firstActionOf(RemedyActionType.DIAGNOSTIC_COMMAND))
                .map(RemedyAction::value).orElse(null);
        return new RemedyResponse(
                remedy.id(), remedy.title(), remedy.summary(), remedy.issueType(), remedy.supportedPlatform(),
                remedy.safetyLevel(), remedy.primaryActionType(), deepLink, command,
                remedy.actions().stream().map(RemediationMapper::toResponse).toList(),
                remedy.steps(), remedy.expectedResult(), remedy.cautions(), remedy.userConfirmationRequired(),
                matchReason, confidence);
    }

    public RemediationAttemptResponse toResponse(RemediationAttempt a) {
        String title = catalog.findById(a.getRemedyId()).map(Remedy::title).orElse(a.getRemedyId());
        return new RemediationAttemptResponse(a.getId(), a.getRemedyId(), title, a.getTicketId(), a.getOutcome(),
                a.getFeedback(), a.getReportedBy(), a.getCreatedAt());
    }

    public List<RemediationAttemptResponse> toAttemptResponses(List<RemediationAttempt> attempts) {
        return attempts.stream().map(this::toResponse).toList();
    }

    private static RemedyActionResponse toResponse(RemedyAction a) {
        return new RemedyActionResponse(a.id(), a.type(), a.label(), a.value(), a.shell(), a.explanation(), a.effects());
    }
}
