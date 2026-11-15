package com.konate.ithelpdesk.triage;

import com.konate.ithelpdesk.TestUsers;
import com.konate.ithelpdesk.config.GlobalExceptionHandler;
import com.konate.ithelpdesk.incident.DataLayer.IncidentRepository;
import com.konate.ithelpdesk.remediation.DataLayer.RemediationAttemptRepository;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import com.konate.ithelpdesk.ticket.PresentationLayer.TicketController;
import com.konate.ithelpdesk.triage.AiLayer.AiTriageOutput;
import com.konate.ithelpdesk.triage.AiLayer.ClaudeClient;
import com.konate.ithelpdesk.triage.DataLayer.AdminReviewRepository;
import com.konate.ithelpdesk.triage.PresentationLayer.TriageController;
import com.konate.ithelpdesk.user.DataLayer.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TiqVision HTTP API with the AI provider mocked out (no network). */
@SpringBootTest
@ActiveProfiles("test")
class TriageControllerTest {

    @MockitoBean ClaudeClient claude;

    @Autowired TriageController triageController;
    @Autowired TicketController ticketController;
    @Autowired TicketRepository ticketRepository;
    @Autowired IncidentRepository incidentRepository;
    @Autowired RemediationAttemptRepository attemptRepository;
    @Autowired AdminReviewRepository reviewRepository;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(triageController, ticketController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(claude.isConfigured()).thenReturn(true);
    }

    @AfterEach
    void cleanUp() {
        reviewRepository.deleteAll();
        attemptRepository.deleteAll();
        ticketRepository.deleteAll();
        incidentRepository.deleteAll();
    }

    private static <B extends AbstractMockHttpServletRequestBuilder<B>> B as(B b, User user) {
        return user == null ? b : b.requestAttr("currentUser", user);
    }

    private ResultActions classify(String text, User user) throws Exception {
        return mvc.perform(as(post("/api/triage/classify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"" + text + "\"}"), user));
    }

    private ResultActions review(String json, User user) throws Exception {
        return mvc.perform(as(post("/api/triage/reviews").contentType(MediaType.APPLICATION_JSON).content(json), user));
    }

    static AiTriageOutput out(String classification) {
        return new AiTriageOutput(classification, 0.9, "reason", "", "VPN failure", "VPN error dialog", "VPN",
                "VPN", "809", "", List.of(), List.of("vpn-settings"), false);
    }

    @Test
    void analyzeAcceptsMultipartScreenshot() throws Exception {
        when(claude.triage(any(), anyList())).thenReturn(out("IT_SUPPORTED"));
        mvc.perform(as(multipart("/api/triage/analyze")
                        .file(new MockMultipartFile("files", "vpn.png", "image/png", TriageServiceTest.PNG))
                        .param("description", "VPN dialog"), TestUsers.employee()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classification").value("IT_SUPPORTED"))
                .andExpect(jsonPath("$.category").value("VPN"))
                .andExpect(jsonPath("$.visibleErrorCode").value("809"))
                .andExpect(jsonPath("$.suggestedRemedyIds[0]").value("vpn-settings"))
                .andExpect(jsonPath("$.recommendedNextStep").value("TRY_FIX"));
        assertThat(ticketRepository.count()).as("no ticket before the user confirms").isZero();
    }

    @Test
    void analyzeRejectsNonImagesAndBlankUploads() throws Exception {
        mvc.perform(as(multipart("/api/triage/analyze")
                        .file(new MockMultipartFile("files", "x.png", "image/png", "GIF89a-not-allowed".getBytes())),
                        TestUsers.employee()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE"));
        mvc.perform(as(multipart("/api/triage/analyze"), TestUsers.employee()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BLANK_UPLOAD"));
    }

    @Test
    void analyzeRequiresSignIn() throws Exception {
        mvc.perform(multipart("/api/triage/analyze")
                        .file(new MockMultipartFile("files", "vpn.png", "image/png", TriageServiceTest.PNG)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonItClassificationReturns422AndCreatesNothing() throws Exception {
        when(claude.triage(any(), anyList())).thenReturn(out("NON_IT"));
        classify("Can you order me pizza?", TestUsers.employee())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NON_IT_ISSUE"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("This doesn't appear to be an IT support issue")))
                .andExpect(jsonPath("$.suggestions", hasSize(3)))
                .andExpect(jsonPath("$.canRequestAdminReview").value(true));

        // A non-IT ticket submitted directly is rejected too.
        mvc.perform(as(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"title\":\"Can you order me pizza?\",\"description\":\"Pepperoni please\",\"category\":\"OTHER\",\"affectedSystem\":\"Lunch\"}"),
                        TestUsers.employee()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NON_IT_ISSUE"));

        assertThat(ticketRepository.count()).isZero();
        assertThat(incidentRepository.count()).isZero();
        assertThat(attemptRepository.count()).isZero();
        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    void classifyReturnsStructuredResultForItProblems() throws Exception {
        when(claude.triage(any(), anyList())).thenReturn(out("IT_SUPPORTED"));
        classify("My VPN is disconnected", TestUsers.employee())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classification").value("IT_SUPPORTED"))
                .andExpect(jsonPath("$.recommendedNextStep").value("TRY_FIX"))
                .andExpect(jsonPath("$.suggestedRemedyIds[0]").value("vpn-settings"));
        classify("I clicked a suspicious link and my computer might be infected.", TestUsers.employee())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classification").value("SECURITY_RISK"))
                .andExpect(jsonPath("$.recommendedNextStep").value("ESCALATE_SECURITY"))
                .andExpect(jsonPath("$.suggestedPriority").value("HIGH"))
                .andExpect(jsonPath("$.suggestedRemedyIds", hasSize(0)));
    }

    @Test
    void adminReviewIsOptInDeduplicatedAndPrivate() throws Exception {
        String body = "{\"text\":\"Can you order me pizza?\",\"classification\":\"NON_IT\",\"confirmed\":%s}";
        review(body.formatted("false"), TestUsers.employee())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONFIRMATION_REQUIRED"));
        assertThat(reviewRepository.count()).isZero();

        review(body.formatted("true"), TestUsers.employee())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterEmail").value(TestUsers.EMPLOYEE_EMAIL))
                .andExpect(jsonPath("$.status").value("OPEN"));
        review(body.formatted("true"), TestUsers.employee())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REVIEW"));

        mvc.perform(as(get("/api/triage/reviews"), TestUsers.otherEmployee())).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(as(get("/api/triage/reviews"), TestUsers.admin())).andExpect(jsonPath("$", hasSize(1)));

        Long id = reviewRepository.findAll().getFirst().getId();
        mvc.perform(as(patch("/api/triage/reviews/" + id + "/close"), TestUsers.employee())).andExpect(status().isForbidden());
        mvc.perform(as(patch("/api/triage/reviews/" + id + "/close"), TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        assertThat(ticketRepository.count()).as("review requests never become tickets").isZero();
        assertThat(incidentRepository.count()).isZero();
    }
}
