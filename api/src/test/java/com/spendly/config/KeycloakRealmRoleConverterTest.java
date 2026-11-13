package com.spendly.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverterTest {

  private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

  private static Jwt.Builder token() {
    return Jwt.withTokenValue("token").header("alg", "RS256").subject("sub-1");
  }

  @Test
  void mapsRealmRolesToPrefixedAuthorities() {
    Jwt jwt =
        token()
            .claim("realm_access", Map.of("roles", List.of("EMPLOYEE", "MANAGER")))
            .claim("scope", "openid profile")
            .build();

    assertThat(converter.convert(jwt))
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_EMPLOYEE", "ROLE_MANAGER");
  }

  @Test
  void noRealmAccessClaimMeansNoAuthority() {
    assertThat(converter.convert(token().build())).isEmpty();
  }

  @Test
  void ignoresMalformedClaims() {
    assertThat(converter.convert(token().claim("realm_access", "EMPLOYEE").build())).isEmpty();
    assertThat(converter.convert(token().claim("realm_access", Map.of("roles", "ADMIN")).build()))
        .isEmpty();
    assertThat(
            converter.convert(
                token().claim("realm_access", Map.of("roles", List.of("", 42, "ADMIN"))).build()))
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_ADMIN");
  }
}
