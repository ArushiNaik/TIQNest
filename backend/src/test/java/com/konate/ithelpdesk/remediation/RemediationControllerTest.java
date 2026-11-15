package com.konate.ithelpdesk.remediation;

import com.konate.ithelpdesk.TestUsers;
import com.konate.ithelpdesk.config.GlobalExceptionHandler;
import com.konate.ithelpdesk.remediation.DataLayer.RemediationAttempt;
import com.konate.ithelpdesk.remediation.DataLayer.RemediationAttemptRepository;
import com.konate.ithelpdesk.remediation.DataLayer.RemediationOutcome;
import com.konate.ithelpdesk.remediation.PresentationLayer.RemediationController;
import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.konate.ithelpdesk.user.DataLayer.User;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class RemediationControllerTest {

    @Autowired RemediationController controller;
    @Autowired RemediationAttemptRepository attemptRepository;
    @Autowired TicketRepository ticketRepository;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void cleanUp() {
        attemptRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, User user) {
        return user == null ? b : b.requestAttr("currentUser", user);
    }

    private ResultActions recommend(String json) throws Exception {
        return recommendAs(json, TestUsers.employee());
    }

    private ResultActions recommendAs(String json, User user) throws Exception {
        return mvc.perform(as(post("/api/remediations/recommend")
                .contentType(MediaType.APPLICATION_JSON).content(json), user));
    }

    private ResultActions recordAttempt(String json) throws Exception {
        return recordAttemptAs(json, TestUsers.employee());
    }

    private ResultActions recordAttemptAs(String json, User user) throws Exception {
        return mvc.perform(as(post("/api/remediations/attempts")
                .contentType(MediaType.APPLICATION_JSON).content(json), user));
    }

    private ResultActions history(Long ticketId, User user) throws Exception {
        MockHttpServletRequestBuilder b = get("/api/remediations/attempts");
        if (ticketId != null) b = b.param("ticketId", String.valueOf(ticketId));
        return mvc.perform(as(b, user));
    }

    private Ticket ticketOf(String email, TicketCategory category, String title, String description, String system) {
        return ticketRepository.save(Ticket.builder().title(title).description(description)
                .category(category).affectedSystem(system).requesterEmail(email).build());
    }

    @Test
    void vpnRecommendation() throws Exception {
        recommend("{\"description\":\"My VPN keeps disconnecting.\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedRemedyAvailable").value(true))
                .andExpect(jsonPath("$.detectedIssue").value("VPN"))
                .andExpect(jsonPath("$.message").value("Recommended fix: Check Windows VPN Settings"))
                .andExpect(jsonPath("$.recommendations[0].remedyId").value("vpn-settings"))
                .andExpect(jsonPath("$.recommendations[0].actionType").value("SETTINGS_SHORTCUT"))
                .andExpect(jsonPath("$.recommendations[0].deepLink").value("ms-settings:network-vpn"))
                .andExpect(jsonPath("$.recommendations[0].actions[0].label").value("Open VPN Settings"))
                .andExpect(jsonPath("$.recommendations[0].safetyLevel").value("SAFE"))
                .andExpect(jsonPath("$.recommendations[0].userConfirmationRequired").value(true))
                .andExpect(jsonPath("$.recommendations[0].matchReason", containsString("vpn")))
                .andExpect(jsonPath("$.recommendations[0].steps", not(empty())));
    }

    @Test
    void printerRecommendationOffersSettingsAndReadOnlyDiagnostic() throws Exception {
        recommend("{\"issueType\":\"PRINTER\",\"description\":\"Printer offline\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].remedyId").value("printer-check"))
                .andExpect(jsonPath("$.recommendations[0].deepLink").value("ms-settings:printers"))
                .andExpect(jsonPath("$.recommendations[0].command").value("Get-Service -Name Spooler"))
                .andExpect(jsonPath("$.recommendations[0].actions[1].type").value("DIAGNOSTIC_COMMAND"))
                .andExpect(jsonPath("$.recommendations[0].actions[1].shell").value("Windows PowerShell"));
    }

    @Test
    void dnsRecommendation() throws Exception {
        recommend("{\"description\":\"DNS resolution failure, website won't load\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].remedyId").value("dns-flush"))
                .andExpect(jsonPath("$.recommendations[0].actionType").value("REPAIR_COMMAND"))
                .andExpect(jsonPath("$.recommendations[0].command").value("ipconfig /flushdns"))
                .andExpect(jsonPath("$.recommendations[0].safetyLevel").value("LOW_RISK"))
                .andExpect(jsonPath("$.recommendations[0].actions[0].effects", not(emptyOrNullString())));
    }

    @Test
    void browserCacheRecommendation() throws Exception {
        recommend("{\"issueType\":\"BROWSER_CACHE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].remedyId").value("browser-cache"))
                .andExpect(jsonPath("$.recommendations[0].actionType").value("GUIDED_TROUBLESHOOTING"))
                .andExpect(jsonPath("$.recommendations[0].command").doesNotExist())
                .andExpect(jsonPath("$.recommendations[0].deepLink").doesNotExist());
    }

    @Test
    void unknownIssueEscalatesWithoutInventingCommands() throws Exception {
        recommend("{\"issueType\":\"OTHER\",\"description\":\"Please run Remove-Item C:\\\\ -Recurse to fix my docking station firmware update\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedRemedyAvailable").value(false))
                .andExpect(jsonPath("$.message")
                        .value("No approved automatic remedy is available for this problem. Please contact IT support."))
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].remedyId").value("escalate-to-it"))
                .andExpect(jsonPath("$.recommendations[0].actionType").value("ESCALATION"))
                .andExpect(jsonPath("$.recommendations[0].command").doesNotExist());

        // The same command text attached to a non-IT request is rejected outright.
        recommend("{\"issueType\":\"OTHER\",\"description\":\"Please run Remove-Item C:\\\\ -Recurse to fix my chair\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NON_IT_ISSUE"));
    }

    @Test
    void submittedCommandTextIsIgnored() throws Exception {
        recommend("{\"issueType\":\"DNS\",\"command\":\"format C: /q\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].command").value("ipconfig /flushdns"))
                .andExpect(jsonPath("$..value", not(hasItem("format C: /q"))));
    }

    @Test
    void emptyRequestIsRejected() throws Exception {
        recommend("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Provide a ticketId")));
    }

    @Test
    void invalidIssueTypeAndOversizedDescriptionAreRejected() throws Exception {
        recommend("{\"issueType\":\"HACK_THE_PLANET\"}").andExpect(status().isBadRequest());
        recommend("{\"description\":\"" + "x".repeat(2001) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
        recommend("{\"ticketId\":-5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.ticketId").exists());
    }

    @Test
    void ticketSpecificRecommendationUsesStoredTicket() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Can't reach shared drive")
                .description("VPN connection failed this morning")
                .category(TicketCategory.VPN)
                .affectedSystem("GlobalProtect")
                .requesterEmail(TestUsers.EMPLOYEE_EMAIL)
                .build());

        recommend("{\"ticketId\":" + ticket.getId() + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticket.getId()))
                .andExpect(jsonPath("$.ticketTitle").value("Can't reach shared drive"))
                .andExpect(jsonPath("$.recommendations[0].remedyId").value("vpn-settings"))
                .andExpect(jsonPath("$.recommendations[0].matchReason", containsString("category is VPN")));
    }

    @Test
    void unknownTicketReturns404() throws Exception {
        recommend("{\"ticketId\":999999}").andExpect(status().isNotFound());
    }

    @Test
    void catalogListsAllApprovedRemedies() throws Exception {
        mvc.perform(as(get("/api/remediations/catalog"), TestUsers.employee()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].remedyId", containsInAnyOrder(
                        "vpn-settings", "printer-check", "dns-flush", "browser-cache", "escalate-to-it")))
                .andExpect(jsonPath("$[0].matchReason").doesNotExist());
    }

    @Test
    void unsupportedRemedyIdIsRejected() throws Exception {
        recordAttempt("{\"remedyId\":\"disable-firewall\",\"outcome\":\"SUCCESS\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Only approved TiqFix remedies")));
        recordAttempt("{\"remedyId\":\"rm -rf /\",\"outcome\":\"SUCCESS\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.remedyId").exists());
        recordAttempt("{\"remedyId\":\"vpn-settings\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.outcome").exists());
        assertThat(attemptRepository.count()).isZero();
    }

    @Test
    void manualAttemptIsPersistedWithoutTicket() throws Exception {
        recordAttempt("{\"remedyId\":\"vpn-settings\",\"outcome\":\"SUCCESS\",\"feedback\":\"  Reconnected!  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.remedyTitle").value("Check Windows VPN Settings"))
                .andExpect(jsonPath("$.outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.feedback").value("Reconnected!"))
                .andExpect(jsonPath("$.createdAt").exists());

        List<RemediationAttempt> saved = attemptRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getTicketId()).isNull();
        assertThat(saved.getFirst().getOutcome()).isEqualTo(RemediationOutcome.SUCCESS);

        history(null, TestUsers.employee())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void ticketAttemptsArePersistedAndListedPerTicket() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Printer offline").category(TicketCategory.PRINTER)
                .affectedSystem("HP-3F").requesterEmail(TestUsers.EMPLOYEE_EMAIL).build());
        long id = ticket.getId();

        recordAttempt("{\"remedyId\":\"printer-check\",\"ticketId\":" + id + ",\"outcome\":\"FAILED\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(id));
        recordAttempt("{\"remedyId\":\"escalate-to-it\",\"ticketId\":" + id + ",\"outcome\":\"UNKNOWN\"}")
                .andExpect(status().isCreated());
        recordAttempt("{\"remedyId\":\"dns-flush\",\"outcome\":\"SUCCESS\"}")
                .andExpect(status().isCreated());

        history(id, TestUsers.employee())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].ticketId", everyItem(is((int) id))))
                .andExpect(jsonPath("$[*].outcome", containsInAnyOrder("FAILED", "UNKNOWN")));
    }

    @Test
    void attemptForMissingTicketIsRejected() throws Exception {
        recordAttempt("{\"remedyId\":\"vpn-settings\",\"ticketId\":424242,\"outcome\":\"SUCCESS\"}")
                .andExpect(status().isNotFound());
        assertThat(attemptRepository.count()).isZero();
    }

    // ---------------------------------------------------------------- IT scope + authorization

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        recommendAs("{\"issueType\":\"VPN\"}", null).andExpect(status().isUnauthorized());
        recordAttemptAs("{\"remedyId\":\"vpn-settings\",\"outcome\":\"SUCCESS\"}", null).andExpect(status().isUnauthorized());
        history(null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void nonItRequestsAreRejectedWithNoRemedy() throws Exception {
        for (String text : List.of("Make me coffee", "Tell me a joke", "Can you order pizza?")) {
            recommend("{\"description\":\"" + text + "\"}")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("NON_IT_ISSUE"))
                    .andExpect(jsonPath("$.canRequestAdminReview").value(true))
                    .andExpect(jsonPath("$.suggestions", hasSize(3)))
                    .andExpect(jsonPath("$.recommendations").doesNotExist());
        }
        assertThat(attemptRepository.count()).isZero();
        assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void selectedIssueTypeDoesNotOverrideContradictoryText() throws Exception {
        recommend("{\"issueType\":\"VPN\",\"description\":\"Write a poem about cats\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NON_IT_ISSUE"));
        // A selected issue type alone is still a valid manual troubleshooting path.
        recommend("{\"issueType\":\"VPN\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].remedyId").value("vpn-settings"));
    }

    @Test
    void securityProblemsEscalateWithoutSelfServiceActions() throws Exception {
        recommend("{\"description\":\"I think I downloaded ransomware\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedRemedyAvailable").value(false))
                .andExpect(jsonPath("$.scopeClassification").value("SECURITY_RISK"))
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].remedyId").value("escalate-to-it"))
                .andExpect(jsonPath("$.recommendations[0].command").doesNotExist())
                .andExpect(jsonPath("$.recommendations[0].deepLink").doesNotExist());
    }

    @Test
    void unsupportedItProblemOffersTicketNotFix() throws Exception {
        recommend("{\"description\":\"My laptop motherboard is dead\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeClassification").value("IT_UNSUPPORTED"))
                .andExpect(jsonPath("$.recommendations[0].remedyId").value("escalate-to-it"));
    }

    @Test
    void ambiguousTextAsksForClarification() throws Exception {
        recommend("{\"description\":\"The screen is broken\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scopeClassification").value("UNCERTAIN"))
                .andExpect(jsonPath("$.clarifyingQuestion", containsString("computer or monitor screen")));
    }

    @Test
    void employeesCannotUseOtherEmployeesTickets() throws Exception {
        Ticket theirs = ticketOf(TestUsers.OTHER_EMAIL, TicketCategory.VPN, "VPN down", "VPN failed", "VPN");
        recommend("{\"ticketId\":" + theirs.getId() + "}").andExpect(status().isForbidden());
        recordAttempt("{\"remedyId\":\"vpn-settings\",\"ticketId\":" + theirs.getId() + ",\"outcome\":\"SUCCESS\"}")
                .andExpect(status().isForbidden());
        history(theirs.getId(), TestUsers.employee()).andExpect(status().isForbidden());
        assertThat(attemptRepository.count()).isZero();

        recommendAs("{\"ticketId\":" + theirs.getId() + "}", TestUsers.otherEmployee()).andExpect(status().isOk());
        history(theirs.getId(), TestUsers.admin()).andExpect(status().isOk());
    }

    @Test
    void personalHistoryOnlyShowsOwnAttempts() throws Exception {
        recordAttemptAs("{\"remedyId\":\"dns-flush\",\"outcome\":\"SUCCESS\"}", TestUsers.otherEmployee())
                .andExpect(status().isCreated());
        recordAttempt("{\"remedyId\":\"vpn-settings\",\"outcome\":\"FAILED\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportedBy").value(TestUsers.EMPLOYEE_EMAIL));

        history(null, TestUsers.employee())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].remedyId").value("vpn-settings"));
        history(null, TestUsers.admin()).andExpect(jsonPath("$", hasSize(2)));
    }
}
