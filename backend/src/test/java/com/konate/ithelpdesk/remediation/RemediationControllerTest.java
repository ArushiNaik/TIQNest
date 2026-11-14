package com.konate.ithelpdesk.remediation;

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

    private ResultActions recommend(String json) throws Exception {
        return mvc.perform(post("/api/remediations/recommend")
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions recordAttempt(String json) throws Exception {
        return mvc.perform(post("/api/remediations/attempts")
                .contentType(MediaType.APPLICATION_JSON).content(json));
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
        recommend("{\"issueType\":\"OTHER\",\"description\":\"Please run Remove-Item C:\\\\ -Recurse to fix my chair\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedRemedyAvailable").value(false))
                .andExpect(jsonPath("$.message")
                        .value("No approved automatic remedy is available for this problem. Please contact IT support."))
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].remedyId").value("escalate-to-it"))
                .andExpect(jsonPath("$.recommendations[0].actionType").value("ESCALATION"))
                .andExpect(jsonPath("$.recommendations[0].command").doesNotExist());
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
                .requesterEmail("bahman@demo.com")
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
        mvc.perform(get("/api/remediations/catalog"))
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

        mvc.perform(get("/api/remediations/attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void ticketAttemptsArePersistedAndListedPerTicket() throws Exception {
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title("Printer offline").category(TicketCategory.PRINTER)
                .affectedSystem("HP-3F").requesterEmail("amin@demo.com").build());
        long id = ticket.getId();

        recordAttempt("{\"remedyId\":\"printer-check\",\"ticketId\":" + id + ",\"outcome\":\"FAILED\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(id));
        recordAttempt("{\"remedyId\":\"escalate-to-it\",\"ticketId\":" + id + ",\"outcome\":\"UNKNOWN\"}")
                .andExpect(status().isCreated());
        recordAttempt("{\"remedyId\":\"dns-flush\",\"outcome\":\"SUCCESS\"}")
                .andExpect(status().isCreated());

        mvc.perform(get("/api/remediations/attempts").param("ticketId", String.valueOf(id)))
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
}
