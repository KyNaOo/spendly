package com.spendly.domain.identity;

import java.util.Optional;

/**
 * Application roles, carried by the Keycloak realm roles of the same name ({@code
 * realm_access.roles} claim of the access token).
 */
public enum Role {
  EMPLOYEE,
  MANAGER,
  ACCOUNTANT,
  ADMIN;

  /**
   * Resolves a role from its exact name, e.g. {@code "MANAGER"}. Unknown names (such as the
   * technical Keycloak roles {@code offline_access} or {@code default-roles-spendly}) give an empty
   * result instead of an exception.
   */
  public static Optional<Role> fromName(String name) {
    for (Role role : values()) {
      if (role.name().equals(name)) {
        return Optional.of(role);
      }
    }
    return Optional.empty();
  }
}
