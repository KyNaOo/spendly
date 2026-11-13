package com.spendly.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns the Keycloak realm roles of an access token into Spring Security authorities.
 *
 * <p>Keycloak puts them in a nested claim: {@code "realm_access": {"roles": ["EMPLOYEE",
 * "MANAGER"]}}. Each role becomes a {@code ROLE_<name>} authority, the prefix expected by {@code
 * hasRole('MANAGER')}. Spring's default converter only reads the {@code scope} claim ({@code
 * SCOPE_*} authorities), hence this class.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  static final String REALM_ACCESS_CLAIM = "realm_access";
  static final String ROLES = "roles";
  static final String ROLE_PREFIX = "ROLE_";

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    if (!(jwt.getClaims().get(REALM_ACCESS_CLAIM) instanceof Map<?, ?> realmAccess)
        || !(realmAccess.get(ROLES) instanceof Collection<?> roles)) {
      return List.of();
    }
    return roles.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .filter(role -> !role.isBlank())
        .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
        .toList();
  }
}
