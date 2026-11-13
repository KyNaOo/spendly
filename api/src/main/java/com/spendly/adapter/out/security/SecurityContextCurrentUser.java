package com.spendly.adapter.out.security;

import com.spendly.application.port.out.CurrentUser;
import com.spendly.domain.identity.AuthenticatedUser;
import com.spendly.domain.identity.Role;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * {@link CurrentUser} backed by the Spring Security context, filled for each request by the
 * resource server from the validated JWT (thread-bound, like Symfony's {@code
 * Security::getUser()}).
 */
@Component
class SecurityContextCurrentUser implements CurrentUser {

  private static final String ROLE_PREFIX = "ROLE_";

  @Override
  public AuthenticatedUser get() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
      throw new IllegalStateException("No authenticated user in the current security context");
    }
    String id = token.getName();
    return new AuthenticatedUser(id, displayName(token, id), roles(token));
  }

  /** Keycloak fills {@code name} (profile scope); fall back to the login, then to the id. */
  private static String displayName(JwtAuthenticationToken token, String id) {
    for (String claim : new String[] {"name", "preferred_username"}) {
      String value = token.getToken().getClaimAsString(claim);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return id;
  }

  /** Application roles only: technical Keycloak roles (offline_access...) are ignored. */
  private static Set<Role> roles(JwtAuthenticationToken token) {
    Set<Role> roles = EnumSet.noneOf(Role.class);
    for (GrantedAuthority authority : token.getAuthorities()) {
      String name = authority.getAuthority();
      if (name != null && name.startsWith(ROLE_PREFIX)) {
        Role.fromName(name.substring(ROLE_PREFIX.length())).ifPresent(roles::add);
      }
    }
    return roles;
  }
}
