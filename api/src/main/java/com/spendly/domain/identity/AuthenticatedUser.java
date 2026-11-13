package com.spendly.domain.identity;

import java.util.Objects;
import java.util.Set;

/**
 * The user behind the current request.
 *
 * @param id stable identifier: the Keycloak subject ({@code sub}), also the primary key of the
 *     {@code employee} table (ADR 0003)
 * @param name display name
 * @param roles application roles held by the user (possibly empty)
 */
public record AuthenticatedUser(String id, String name, Set<Role> roles) {

  public AuthenticatedUser {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    if (id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    // Defensive, immutable copy: the record stays immutable whatever set the caller passed.
    roles = roles == null ? Set.of() : Set.copyOf(roles);
  }

  public boolean hasRole(Role role) {
    return roles.contains(role);
  }
}
