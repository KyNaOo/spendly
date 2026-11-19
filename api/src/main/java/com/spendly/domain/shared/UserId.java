package com.spendly.domain.shared;

import java.util.Objects;

/**
 * Typed identifier of a user (employee, manager, accountant...). Its value is the Keycloak subject
 * ({@code sub} claim), also the primary key of the {@code employee} table (ADR 0003). It is kept as
 * an opaque string: the application never relies on its format.
 *
 * @param value the Keycloak subject, never blank
 */
public record UserId(String value) {

  public UserId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("value must not be blank");
    }
  }

  public static UserId of(String value) {
    return new UserId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
