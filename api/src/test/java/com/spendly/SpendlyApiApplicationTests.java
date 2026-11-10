package com.spendly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SpendlyApiApplicationTests {

  @Autowired private MockMvcTester mvc;

  @Test
  void contextLoads() {
    // Fails if the Spring context (datasource, Flyway, JPA, security...) cannot start.
  }

  @Test
  void healthIsPublicAndUpWhenDatabaseIsReachable() {
    assertThat(mvc.get().uri("/actuator/health"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("UP");
  }

  @Test
  void infoRequiresAuthentication() {
    assertThat(mvc.get().uri("/actuator/info")).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void infoIsExposedToAuthenticatedUsers() {
    assertThat(mvc.get().uri("/actuator/info").with(user("someone")))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.app.name")
        .isEqualTo("spendly-api");
  }
}
