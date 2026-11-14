package com.konate.ithelpdesk.remediation.BusinessLayer;

import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A reviewed, allowlisted remedy. Instances only exist inside {@link RemediationCatalog}.
 *
 * @param keywords lower-case phrase -> weight, used by {@link RemediationMatcher}
 */
public record Remedy(
        String id,
        IssueType issueType,
        String title,
        String summary,
        String supportedPlatform,
        SafetyLevel safetyLevel,
        RemedyActionType primaryActionType,
        List<RemedyAction> actions,
        List<String> steps,
        String expectedResult,
        List<String> cautions,
        boolean userConfirmationRequired,
        Set<TicketCategory> categories,
        Map<String, Integer> keywords
) {
    public Optional<RemedyAction> firstActionOf(RemedyActionType type) {
        return actions.stream().filter(a -> a.type() == type).findFirst();
    }
}
