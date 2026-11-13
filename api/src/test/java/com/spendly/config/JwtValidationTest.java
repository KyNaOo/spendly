package com.spendly.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.spendly.TestcontainersConfiguration;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * End-to-end validation of real signed tokens sent as {@code Authorization: Bearer}: signature,
 * expiration, issuer and audience {@code spendly-api}, then the role mapping.
 *
 * <p>No Keycloak: a tiny HTTP server of the JDK plays the realm. It publishes the OIDC discovery
 * document ({@code /.well-known/openid-configuration}) and the public key (JWKS) of an RSA key pair
 * generated at startup, with which the test signs its tokens. The resource server is configured
 * exactly as in production ({@code issuer-uri} + audience of {@code application.yml}), only the
 * issuer URL points at this fake realm.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class JwtValidationTest {

  private static final String REALM_PATH = "/realms/spendly";
  private static final String ALICE_ID = "00000000-0000-4000-8000-000000000001";
  private static final KeyPair KEYS = generateRsaKeyPair();
  private static final KeyPair OTHER_KEYS = generateRsaKeyPair();

  @Autowired private MockMvcTester mvc;

  private static HttpServer realm;
  private static String issuer;

  @BeforeAll
  static void startFakeRealm() throws IOException {
    realm = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    issuer = "http://localhost:" + realm.getAddress().getPort() + REALM_PATH;
    String jwks =
        new JWKSet(new RSAKey.Builder((RSAPublicKey) KEYS.getPublic()).build()).toString();
    String discovery =
        """
        {"issuer": "%s", "jwks_uri": "%s/protocol/openid-connect/certs"}\
        """
            .formatted(issuer, issuer);
    serveJson(REALM_PATH + "/.well-known/openid-configuration", discovery);
    serveJson(REALM_PATH + "/protocol/openid-connect/certs", jwks);
    realm.start();
  }

  @AfterAll
  static void stopFakeRealm() {
    realm.stop(0);
  }

  /**
   * Overrides the issuer of application-test.yml (supplier called lazily, once the realm is up).
   */
  @DynamicPropertySource
  static void issuer(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer);
  }

  @Test
  void validTokenIsAccepted() {
    String token = sign(KEYS, claims -> {});

    var response =
        assertThat(mvc.get().uri("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .hasStatusOk()
            .bodyJson();
    response.extractingPath("$.id").isEqualTo(ALICE_ID);
    response.extractingPath("$.name").isEqualTo("Alice Martin");
    response.extractingPath("$.roles").isEqualTo(List.of("EMPLOYEE"));
  }

  @Test
  void tokenForAnotherAudienceIsRejected() {
    assertRejected(sign(KEYS, claims -> claims.audience(List.of("account"))));
  }

  @Test
  void tokenFromAnotherIssuerIsRejected() {
    assertRejected(sign(KEYS, claims -> claims.issuer(issuer.replace("spendly", "other"))));
  }

  @Test
  void expiredTokenIsRejected() {
    Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
    assertRejected(sign(KEYS, claims -> claims.issuedAt(past).expiresAt(past.plusSeconds(300))));
  }

  @Test
  void tokenSignedWithAnotherKeyIsRejected() {
    assertRejected(sign(OTHER_KEYS, claims -> {}));
  }

  @Test
  void malformedTokenIsRejected() {
    assertRejected("not-a-jwt");
  }

  private void assertRejected(String token) {
    assertThat(mvc.get().uri("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .hasStatus(HttpStatus.UNAUTHORIZED)
        .headers()
        .hasHeaderSatisfying(
            HttpHeaders.WWW_AUTHENTICATE,
            values -> assertThat(values).singleElement().asString().contains("invalid_token"));
  }

  /** Builds a token shaped like a Keycloak access token for Alice, then lets the test alter it. */
  private static String sign(KeyPair keys, Consumer<JwtClaimsSet.Builder> customizer) {
    Instant now = Instant.now();
    JwtClaimsSet.Builder claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(ALICE_ID)
            .audience(List.of("spendly-api", "account"))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .claim("name", "Alice Martin")
            .claim("preferred_username", "alice")
            .claim("realm_access", Map.of("roles", List.of("EMPLOYEE", "offline_access")));
    customizer.accept(claims);
    RSAKey jwk =
        new RSAKey.Builder((RSAPublicKey) keys.getPublic())
            .privateKey((RSAPrivateKey) keys.getPrivate())
            .build();
    NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
  }

  private static KeyPair generateRsaKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static void serveJson(String path, String body) {
    realm.createContext(
        path,
        exchange -> {
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (var out = exchange.getResponseBody()) {
            out.write(bytes);
          }
        });
  }
}
