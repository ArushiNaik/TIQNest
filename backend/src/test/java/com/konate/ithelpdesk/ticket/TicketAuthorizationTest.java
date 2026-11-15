package com.konate.ithelpdesk.ticket;

import com.konate.ithelpdesk.TestUsers;
import com.konate.ithelpdesk.audit.DataLayer.AuditEventRepository;
import com.konate.ithelpdesk.audit.PresentationLayer.AuditController;
import com.konate.ithelpdesk.config.GlobalExceptionHandler;
import com.konate.ithelpdesk.incident.DataLayer.IncidentRepository;
import com.konate.ithelpdesk.sla.SlaController;
import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import com.konate.ithelpdesk.ticket.PresentationLayer.TicketController;
import com.konate.ithelpdesk.user.DataLayer.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Backend authorization for tickets, history and SLA data (AI not configured in tests: rules only). */
@SpringBootTest
@ActiveProfiles("test")
class TicketAuthorizationTest {

    @Autowired TicketController ticketController;
    @Autowired AuditController auditController;
    @Autowired SlaController slaController;
    @Autowired TicketRepository ticketRepository;
    @Autowired IncidentRepository incidentRepository;
    @Autowired AuditEventRepository auditEventRepository;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(ticketController, auditController, slaController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void cleanUp() {
        auditEventRepository.deleteAll();
        ticketRepository.deleteAll();
        incidentRepository.deleteAll();
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder b, User user) {
        return user == null ? b : b.requestAttr("currentUser", user);
    }

    private Ticket ticketOf(String email) {
        return ticketRepository.save(Ticket.builder().title("Outlook not syncing").category(TicketCategory.EMAIL)
                .affectedSystem("Outlook").requesterEmail(email).build());
    }

    @Test
    void unauthenticatedRequestsGet401() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"VPN down\",\"category\":\"VPN\",\"affectedSystem\":\"VPN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeesOnlySeeTheirOwnTickets() throws Exception {
        Ticket mine = ticketOf(TestUsers.EMPLOYEE_EMAIL);
        Ticket theirs = ticketOf(TestUsers.OTHER_EMAIL);

        mvc.perform(as(get("/api/tickets"), TestUsers.employee()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(mine.getId()));
        mvc.perform(as(get("/api/tickets/" + theirs.getId()), TestUsers.employee())).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/tickets/" + theirs.getId() + "/history"), TestUsers.employee())).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/tickets/" + mine.getId()), TestUsers.employee())).andExpect(status().isOk());

        mvc.perform(as(get("/api/tickets"), TestUsers.admin())).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(as(get("/api/tickets/" + theirs.getId()), TestUsers.admin())).andExpect(status().isOk());
    }

    @Test
    void onlyAdminsUpdateTicketsAndSeeOperationalData() throws Exception {
        Ticket mine = ticketOf(TestUsers.EMPLOYEE_EMAIL);
        String patch = "{\"status\":\"RESOLVED\",\"actor\":\"someone-else@demo.com\"}";
        mvc.perform(as(patch("/api/tickets/" + mine.getId()).contentType(MediaType.APPLICATION_JSON).content(patch),
                TestUsers.employee())).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/tickets/queue"), TestUsers.employee())).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/sla/summary"), TestUsers.employee())).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/sla/alerts"), TestUsers.employee())).andExpect(status().isForbidden());

        mvc.perform(as(patch("/api/tickets/" + mine.getId()).contentType(MediaType.APPLICATION_JSON).content(patch),
                TestUsers.admin())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
        // The audit actor is the signed-in admin, not the client-supplied "actor".
        assertThat(auditEventRepository.findAll()).anyMatch(e -> TestUsers.ADMIN_EMAIL.equals(e.getActor()));
        assertThat(auditEventRepository.findAll()).noneMatch(e -> "someone-else@demo.com".equals(e.getActor()));
    }

    @Test
    void requesterIdentityComesFromTheSessionNotTheBody() throws Exception {
        mvc.perform(as(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"title\":\"VPN keeps dropping\",\"category\":\"VPN\",\"affectedSystem\":\"VPN\",\"requesterEmail\":\"ceo@demo.com\"}"),
                        TestUsers.employee()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requesterEmail").value(TestUsers.EMPLOYEE_EMAIL));
    }

    @Test
    void securityTicketsAreRaisedToHighPriority() throws Exception {
        mvc.perform(as(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"title\":\"I clicked a phishing link\",\"category\":\"OTHER\",\"affectedSystem\":\"Email security\",\"priority\":\"LOW\"}"),
                        TestUsers.employee()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }
}
