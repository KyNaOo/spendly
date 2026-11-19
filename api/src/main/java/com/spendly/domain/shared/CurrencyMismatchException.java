package com.spendly.domain.shared;

import java.util.Currency;
import java.util.Objects;

/**
 * Two amounts in different currencies were combined or compared. Amounts must first be converted
 * (at the exchange rate of the expense date) before being added together.
 */
public final class CurrencyMismatchException extends RuntimeException {

  private final Currency expected;
  private final Currency actual;

  public CurrencyMismatchException(Currency expected, Currency actual) {
    super(
        "Currency mismatch: expected %s but got %s"
            .formatted(Objects.requireNonNull(expected), Objects.requireNonNull(actual)));
    this.expected = expected;
    this.actual = actual;
  }

  public Currency expected() {
    return expected;
  }

  public Currency actual() {
    return actual;
  }
}
