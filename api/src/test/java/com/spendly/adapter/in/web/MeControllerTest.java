package com.spendly.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.spendly.TestcontainersConfiguration;
import com.spendly.config.KeycloakRealmRoleConverter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * {@code GET /api/me} and the HTTP security rules, with the whole application context.
 *
 * <p>{@code jwt()} (spring-security-test) injects an already-validated token into the request: the
 * decoder (signature, issuer, audience) is bypassed, see {@code JwtValidationTest} for that part.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class MeControllerTest {

  private static final String MARC_ID = "00000000-0000-4000-8000-000000000003";

  @Autowired private MockMvcTester mvc;

  @Test
  void withoutTokenIsUnauthorized() {
    assertThat(mvc.get().uri("/api/me"))
        .hasStatus(HttpStatus.UNAUTHORIZED)
        .headers()
        .hasHeaderSatisfying(
            HttpHeaders.WWW_AUTHENTICATE,
            values -> assertThat(values).singleElement().asString().startsWith("Bearer"));
  }

  @Test
  void withValidTokenReturnsIdentityNameAndRoles() {
    var token =
        jwt()
            .jwt(jwt ->
                jwt.subject(MARC_ID)
                    .claim("name", "Marc Lefèvre")
                    .claim(
                        "realm_access",
                        Map.of(
                            "roles",
                            List.of(
                                "offline_access", "MANAGER", "EMPLOYEE", "default-roles-spendly"))))
            // Same role mapping as production (otherwise jwt() reads the "scope" claim).
            .authorities(new KeycloakRealmRoleConverter());

    var response = assertThat(mvc.get().uri("/api/me").with(token)).hasStatusOk().bodyJson();
    response.extractingPath("$.id").isEqualTo(MARC_ID);
    response.extractingPath("$.name").isEqualTo("Marc Lefèvre");
    response.extractingPath("$.roles").isEqualTo(List.of("EMPLOYEE", "MANAGER"));
  }

  @Test
  void healthStaysPublic() {
    assertThat(mvc.get().uri("/actuator/health")).hasStatusOk();
  }

  @Test
  void sessionIsNeverCreated() {
    var token = jwt().jwt(jwt -> jwt.subject(MARC_ID));
    assertThat(mvc.get().uri("/api/me").with(token))
        .hasStatusOk()
        .headers()
        .doesNotContainHeader(HttpHeaders.SET_COOKIE);
  }

  @Test
  void corsPreflightIsAllowedForTheFrontEnd() {
    assertThat(
            mvc.options()
                .uri("/api/me")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
        .hasStatusOk()
        .headers()
        .hasSingleValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200");
  }

  @Test
  void corsPreflightIsRejectedForOtherOrigins() {
    assertThat(
            mvc.options()
                .uri("/api/me")
                .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .hasStatus(HttpStatus.FORBIDDEN)
        .headers()
        .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
  }
}
