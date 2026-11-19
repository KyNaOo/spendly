package com.spendly.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.spendly.TestcontainersConfiguration;
import com.spendly.application.port.out.CurrentUser;
import com.spendly.domain.identity.AccessPolicy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Method security infrastructure (ADR 0004), with the whole application context:
 * {@code @PreAuthorize} on an application service, fine-grained rule of the domain, 403 {@code
 * ProblemDetail}.
 *
 * <p>The real manager endpoints arrive with the expense reports (issue #30): this test plugs in a
 * test-only service and controller built exactly like the future ones, with a hard-coded team
 * instead of the {@code employee} table.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({
  TestcontainersConfiguration.class,
  MethodSecurityTest.ApprovalTestService.class,
  MethodSecurityTest.ApprovalTestController.class
})
@ActiveProfiles("test")
class MethodSecurityTest {

  private static final String ALICE = "00000000-0000-4000-8000-000000000001";
  private static final String MARC = "00000000-0000-4000-8000-000000000003";
  private static final String OUTSIDER = "00000000-0000-4000-8000-000000000042";
  private static final String OTHER_MANAGER = "00000000-0000-4000-8000-000000000099";

  /** Author -> manager, as the {@code employee} table will hold it (ADR 0003). */
  private static final Map<String, String> MANAGER_OF =
      Map.of(ALICE, MARC, OUTSIDER, OTHER_MANAGER);

  @Autowired private MockMvcTester mvc;

  @Autowired
  @Qualifier("handlerExceptionResolver") private HandlerExceptionResolver exceptionResolver;

  @Test
  void employeeCannotCallAManagerEndpoint() {
    var response = assertThat(mvc.get().uri("/api/test/approvals").with(alice()));
    response
        .hasStatus(HttpStatus.FORBIDDEN)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    var body = response.bodyJson();
    body.extractingPath("$.status").isEqualTo(403);
    body.extractingPath("$.title").isEqualTo("Forbidden");
    body.extractingPath("$.instance").isEqualTo("/api/test/approvals");
    // The @PreAuthorize expression is never disclosed.
    body.extractingPath("$.detail").isEqualTo("You are not allowed to perform this operation");
    response.bodyText().doesNotContain("hasRole");
  }

  @Test
  void managerCanCallAManagerEndpoint() {
    assertThat(mvc.get().uri("/api/test/approvals").with(marc())).hasStatusOk();
  }

  @Test
  void managerCanApproveTheReportOfHisTeamMember() {
    assertThat(mvc.post().uri("/api/test/reports/{author}/approve", ALICE).with(marc()))
        .hasStatusOk();
  }

  @Test
  void managerCannotApproveTheReportOfAnEmployeeOutsideHisTeam() {
    var response =
        assertThat(mvc.post().uri("/api/test/reports/{author}/approve", OUTSIDER).with(marc()));
    response
        .hasStatus(HttpStatus.FORBIDDEN)
        .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    var body = response.bodyJson();
    body.extractingPath("$.status").isEqualTo(403);
    body.extractingPath("$.reason").isEqualTo("NOT_MANAGER_OF_AUTHOR");
    body.extractingPath("$.instance").isEqualTo("/api/test/reports/" + OUTSIDER + "/approve");
  }

  @Test
  void employeeCannotApproveEvenHisOwnReport() {
    assertThat(mvc.post().uri("/api/test/reports/{author}/approve", ALICE).with(alice()))
        .hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void filterChainRefusalsUseTheSameProblemDetail() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/anything");
    request.setRequestURI("/api/anything");
    var response = new MockHttpServletResponse();

    new ProblemDetailAccessDeniedHandler(exceptionResolver)
        .handle(request, response, new AccessDeniedException("Access Denied"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(MediaType.parseMediaType(response.getContentType()))
        .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(response.getContentAsString())
        .contains("\"status\":403", "\"title\":\"Forbidden\"", "\"instance\":\"/api/anything\"");
  }

  private static JwtRequestPostProcessor alice() {
    return token(ALICE, "EMPLOYEE");
  }

  private static JwtRequestPostProcessor marc() {
    return token(MARC, "EMPLOYEE", "MANAGER");
  }

  private static JwtRequestPostProcessor token(String subject, String... roles) {
    return jwt()
        .jwt(jwt -> jwt.subject(subject).claim("realm_access", Map.of("roles", List.of(roles))))
        // Same role mapping as production (otherwise jwt() reads the "scope" claim).
        .authorities(new KeycloakRealmRoleConverter());
  }

  /** Stand-in for a future application service: role on the method, team rule in the domain. */
  @Service
  static class ApprovalTestService {

    private final CurrentUser currentUser;

    ApprovalTestService(CurrentUser currentUser) {
      this.currentUser = currentUser;
    }

    @PreAuthorize("hasRole('MANAGER')")
    List<String> queue() {
      return List.of();
    }

    @PreAuthorize("hasRole('MANAGER')")
    void approve(String authorId) {
      AccessPolicy.requireManagerOf(currentUser.get(), authorId, MANAGER_OF.get(authorId));
    }
  }

  @RestController
  static class ApprovalTestController {

    private final ApprovalTestService service;

    ApprovalTestController(ApprovalTestService service) {
      this.service = service;
    }

    @GetMapping("/api/test/approvals")
    List<String> queue() {
      return service.queue();
    }

    @PostMapping("/api/test/reports/{author}/approve")
    void approve(@PathVariable String author) {
      service.approve(author);
    }
  }
}
