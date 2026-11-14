package com.konate.ithelpdesk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")   // uses the "test" section of application.yaml (H2, no Docker needed)
class ItHelpDeskApplicationTests {

    @Test
    void contextLoads() {
    }

}
