package com.konate.ithelpdesk.incident;

import com.konate.ithelpdesk.TestUsers;
import com.konate.ithelpdesk.audit.DataLayer.AuditAction;
import com.konate.ithelpdesk.audit.DataLayer.AuditEventRepository;
import com.konate.ithelpdesk.config.GlobalExceptionHandler;
import com.konate.ithelpdesk.config.NonITIssueException;
import com.konate.ithelpdesk.incident.BusinessLayer.AffectedSystemNormalizer;
import com.konate.ithelpdesk.incident.DataLayer.Incident;
import com.konate.ithelpdesk.incident.DataLayer.IncidentRepository;
import com.konate.ithelpdesk.incident.DataLayer.IncidentStatus;
import com.konate.ithelpdesk.incident.PresentationLayer.IncidentController;
import com.konate.ithelpdesk.ticket.BusinessLayer.TicketService;
import com.konate.ithelpdesk.ticket.DataTier.Ticket;
import com.konate.ithelpdesk.ticket.DataTier.TicketCategory;
import com.konate.ithelpdesk.ticket.DataTier.TicketRepository;
import com.konate.ithelpdesk.ticket.DataTier.TicketStatus;
import com.konate.ithelpdesk.ticket.PresentationLayer.CreateTicketRequest;
import com.konate.ithelpdesk.ticket.PresentationLayer.TicketResponse;
import com.konate.ithelpdesk.user.DataLayer.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TiqRadar: 3 distinct employees reporting the same system within 5 minutes form one incident. */
@SpringBootTest
@ActiveProfiles("test")
class OutageRadarIntegrationTest {

    @Autowired TicketService ticketService;
    @Autowired TicketRepository ticketRepository;
    @Autowired IncidentRepository incidentRepository;
    @Autowired AuditEventRepository auditEventRepository;
    @Autowired IncidentController incidentController;

    @AfterEach
    void cleanUp() {
        auditEventRepository.deleteAll();
        ticketRepository.deleteAll();
        incidentRepository.deleteAll();
    }

    private TicketResponse report(String who, TicketCategory category, String system) {
        return ticketService.create(new CreateTicketRequest("Problem with " + system, "Reported by " + who,
                category, system, who + "@demo.com", null));
    }

    private TicketResponse vpn(String who) {
        return report(who, TicketCategory.VPN, "VPN");
    }

    private List<Incident> active() {
        return incidentRepository.findByStatusOrderByDetectedAtDesc(IncidentStatus.ACTIVE);
    }

    @Test
    void oneAndTwoReportsDoNotCreateAnIncident() {
        vpn("alice");
        assertThat(active()).isEmpty();
        vpn("bob");
        assertThat(active()).isEmpty();
    }

    @Test
    void thirdDistinctReporterCreatesOneIncidentAndLinksAllTickets() {
        vpn("alice");
        vpn("bob");
        TicketResponse third = report("carol", TicketCategory.VPN, "vpn client"); // normalized to VPN

        assertThat(active()).hasSize(1);
        Incident incident = active().getFirst();
        assertThat(incident.getTicketCount()).isEqualTo(3);
        assertThat(third.incidentId()).isEqualTo(incident.getId());
        assertThat(ticketRepository.findByIncidentId(incident.getId()))
                .hasSize(3)
                .allMatch(t -> t.getStatus() == TicketStatus.LINKED_TO_INCIDENT);
        assertThat(auditEventRepository.findAll())
                .filteredOn(e -> e.getAction() == AuditAction.LINKED_TO_INCIDENT).hasSize(3);
    }

    @Test
    void fourthAndFifthReportsJoinTheExistingIncident() {
        vpn("alice");
        vpn("bob");
        vpn("carol");
        vpn("dan");
        vpn("emma");
        assertThat(active()).hasSize(1);
        assertThat(active().getFirst().getTicketCount()).isEqualTo(5);
        assertThat(incidentRepository.count()).isEqualTo(1);
    }

    @Test
    void reportsOutsideTheWindowDoNotCount() {
        ticketRepository.save(Ticket.builder().title("old").category(TicketCategory.VPN).affectedSystem("VPN")
                .requesterEmail("old@demo.com").createdAt(Instant.now().minus(Duration.ofMinutes(10))).build());
        vpn("alice");
        vpn("bob");
        assertThat(active()).isEmpty();
    }

    @Test
    void differentSystemsFormSeparateClusters() {
        vpn("alice");
        vpn("bob");
        report("carol", TicketCategory.EMAIL, "Outlook");
        report("dan", TicketCategory.PRINTER, "HP-3F");
        assertThat(active()).isEmpty();
    }

    @Test
    void sameEmployeeRepeatingDoesNotCountTwice() {
        vpn("alice");
        assertThatThrownBy(() -> vpn("alice")).isInstanceOf(ResponseStatusException.class); // duplicate guard
        report("alice", TicketCategory.VPN, "GlobalProtect VPN"); // same outage, other wording
        vpn("bob");
        assertThat(active()).as("only 2 distinct people affected").isEmpty();
    }

    @Test
    void genericSystemsAreNeverGrouped() {
        report("alice", TicketCategory.OTHER, "Laptop");
        report("bob", TicketCategory.OTHER, "laptop");
        report("carol", TicketCategory.OTHER, "LAPTOP");
        assertThat(active()).isEmpty();
        assertThat(AffectedSystemNormalizer.isGeneric("Unknown")).isTrue();
    }

    @Test
    void concurrentReportsCreateExactlyOneIncident() throws Exception {
        int reporters = 6;
        ExecutorService pool = Executors.newFixedThreadPool(reporters);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TicketResponse>> results = new ArrayList<>();
        for (int i = 0; i < reporters; i++) {
            String who = "user" + i;
            results.add(pool.submit(() -> {
                start.await();
                return vpn(who);
            }));
        }
        start.countDown();
        for (Future<TicketResponse> f : results) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(active()).hasSize(1);
        Incident incident = active().getFirst();
        assertThat(ticketRepository.findByIncidentId(incident.getId())).hasSize(reporters);
        assertThat(incident.getTicketCount()).isEqualTo(reporters);
    }

    @Test
    void resolvingAnIncidentUpdatesStatusAndAllowsANewOneLater() throws Exception {
        vpn("alice");
        vpn("bob");
        vpn("carol");
        Incident incident = active().getFirst();

        MockMvc mvc = MockMvcBuilders.standaloneSetup(incidentController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(patch("/api/incidents/" + incident.getId() + "/resolve").requestAttr("currentUser", TestUsers.employee()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/incidents/" + incident.getId() + "/resolve").requestAttr("currentUser", TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        Incident resolved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(resolved.getActiveKey()).isNull();
        assertThat(ticketRepository.findByIncidentId(incident.getId())).allMatch(t -> t.getStatus() == TicketStatus.RESOLVED);
        assertThat(auditEventRepository.findAll())
                .anyMatch(e -> TestUsers.ADMIN_EMAIL.equals(e.getActor()) && e.getAction() == AuditAction.STATUS_CHANGED);

        // A new wave of reports after resolution creates a fresh incident.
        report("dan", TicketCategory.VPN, "Company VPN");
        report("emma", TicketCategory.VPN, "VPN");
        report("frank", TicketCategory.VPN, "VPN");
        assertThat(active()).hasSize(1);
        assertThat(active().getFirst().getId()).isNotEqualTo(incident.getId());
    }

    @Test
    void nonItSubmissionsNeverReachTheRadar() {
        User employee = TestUsers.employee();
        for (int i = 0; i < 3; i++) {
            User u = TestUsers.user("emp" + i + "@demo.com", employee.getRole());
            assertThatThrownBy(() -> ticketService.create(new CreateTicketRequest("Can you order pizza for the team?",
                    "Pepperoni", TicketCategory.OTHER, "Lunch order", null, null), u))
                    .isInstanceOf(NonITIssueException.class);
        }
        assertThat(ticketRepository.count()).isZero();
        assertThat(incidentRepository.count()).isZero();
    }

    @Test
    void bannerApiExposesOnlySystemLevelInformation() throws Exception {
        vpn("alice");
        vpn("bob");
        vpn("carol");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(incidentController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/incidents/active").requestAttr("currentUser", TestUsers.employee()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bannerMessage", containsString("VPN")))
                .andExpect(jsonPath("$[0].requesterEmail").doesNotExist())
                .andExpect(jsonPath("$[0].tickets").doesNotExist())
                .andExpect(jsonPath("$[0].activeKey").doesNotExist());
        mvc.perform(get("/api/incidents").requestAttr("currentUser", TestUsers.employee()))
                .andExpect(status().isForbidden());
    }
}
