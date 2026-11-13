package com.spendly.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spendly.domain.identity.AuthenticatedUser;
import com.spendly.domain.identity.Role;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityContextCurrentUserTest {

  private final SecurityContextCurrentUser currentUser = new SecurityContextCurrentUser();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  private static void authenticate(Jwt jwt, String... authorities) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList(authorities)));
  }

  private static Jwt.Builder token() {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("00000000-0000-4000-8000-000000000003");
  }

  @Test
  void readsIdentityNameAndApplicationRolesFromTheToken() {
    authenticate(
        token().claim("name", "Marc Lefèvre").claim("preferred_username", "marc").build(),
        "ROLE_EMPLOYEE",
        "ROLE_MANAGER",
        "ROLE_offline_access",
        "SCOPE_openid");

    AuthenticatedUser user = currentUser.get();

    assertThat(user.id()).isEqualTo("00000000-0000-4000-8000-000000000003");
    assertThat(user.name()).isEqualTo("Marc Lefèvre");
    assertThat(user.roles()).containsExactlyInAnyOrder(Role.EMPLOYEE, Role.MANAGER);
  }

  @Test
  void fallsBackToTheLoginThenToTheIdentifier() {
    authenticate(token().claim("preferred_username", "marc").build());
    assertThat(currentUser.get().name()).isEqualTo("marc");

    authenticate(token().build());
    assertThat(currentUser.get().name()).isEqualTo("00000000-0000-4000-8000-000000000003");
    assertThat(currentUser.get().roles()).isEmpty();
  }

  @Test
  void failsWithoutJwtAuthentication() {
    assertThatThrownBy(currentUser::get).isInstanceOf(IllegalStateException.class);

    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user", "pwd", List.of()));
    assertThatThrownBy(currentUser::get).isInstanceOf(IllegalStateException.class);
  }
}
