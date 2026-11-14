package com.konate.ithelpdesk.remediation;

import com.konate.ithelpdesk.remediation.BusinessLayer.IssueType;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationCatalog;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher.MatchInput;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemediationMatcher.RemedyMatch;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemedyAction;
import com.konate.ithelpdesk.remediation.BusinessLayer.RemedyActionType;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests of the deterministic matcher and the allowlisted catalog (no Spring context). */
class RemediationMatcherTest {

    private final RemediationCatalog catalog = new RemediationCatalog();
    private final RemediationMatcher matcher = new RemediationMatcher(catalog);

    private String best(String text) {
        return matcher.match(new MatchInput(null, null, text)).getFirst().remedy().id();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "My VPN keeps disconnecting                 | vpn-settings",
            "Cannot connect to VPN from home            | vpn-settings",
            "VPN connection failed with error 809       | vpn-settings",
            "Printer offline on floor 3                 | printer-check",
            "Cannot print, the print queue is stuck     | printer-check",
            "Print spooler keeps crashing               | printer-check",
            "DNS resolution failure on the intranet     | dns-flush",
            "Website won't load: server not found       | dns-flush",
            "DNS cache issue                            | dns-flush",
            "Browser cache shows old content            | browser-cache",
            "Website displays old content after update  | browser-cache",
            "Browser not loading correctly              | browser-cache",
            "My monitor makes a weird noise             | escalate-to-it",
    })
    void picksTheExpectedRemedyFromFreeText(String text, String expected) {
        assertThat(best(text)).isEqualTo(expected);
    }

    @Test
    void explicitIssueTypeWinsAndExplainsWhy() {
        RemedyMatch m = matcher.match(new MatchInput(IssueType.PRINTER, null, "")).getFirst();
        assertThat(m.remedy().id()).isEqualTo(RemediationCatalog.PRINTER_CHECK);
        assertThat(m.reason()).contains("selected printer");
    }

    @Test
    void ticketCategoryAloneIsEnough() {
        assertThat(matcher.match(new MatchInput(null, TicketCategory.VPN, "help")).getFirst().remedy().id())
                .isEqualTo(RemediationCatalog.VPN_SETTINGS);
    }

    @Test
    void securityProblemsAlwaysEscalateEvenWhenOtherKeywordsMatch() {
        List<RemedyMatch> matches = matcher.match(new MatchInput(IssueType.VPN, null,
                "VPN stopped working after I clicked a phishing link"));
        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().remedy().id()).isEqualTo(RemediationCatalog.ESCALATE);
        assertThat(matches.getFirst().reason()).contains("phishing");
    }

    @Test
    void otherWithNoKnownKeywordsEscalates() {
        assertThat(matcher.match(new MatchInput(IssueType.OTHER, TicketCategory.HARDWARE, "laptop is slow"))
                .getFirst().remedy().id()).isEqualTo(RemediationCatalog.ESCALATE);
    }

    @Test
    void catalogOnlyContainsAllowlistedLinksAndCommands() {
        List<String> values = catalog.all().stream()
                .flatMap(r -> r.actions().stream())
                .map(RemedyAction::value)
                .filter(v -> v != null)
                .toList();
        assertThat(values).containsExactlyInAnyOrder(
                "ms-settings:network-vpn",
                "ms-settings:printers",
                "Get-Service -Name Spooler",
                "ipconfig /flushdns",
                "ms-settings:network-status");

        catalog.all().forEach(r -> r.actions().stream()
                .filter(a -> a.type() == RemedyActionType.SETTINGS_SHORTCUT)
                .forEach(a -> assertThat(a.value()).matches("^ms-settings:[a-z0-9-]+$")));
    }
}
