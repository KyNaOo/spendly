package com.spendly.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * An amount of money in a given currency. Immutable value object: every operation returns a new
 * instance.
 *
 * <p>The amount is a {@link BigDecimal}, never a {@code double}: binary floating point cannot
 * represent {@code 0.10} exactly ({@code 0.1 + 0.2 != 0.3}), which is unacceptable for money. The
 * amount is always normalised to {@value #SCALE} decimals with {@link RoundingMode#HALF_EVEN}
 * (banker's rounding, which does not bias sums upwards). Thanks to this normalisation, {@code 0.1}
 * and {@code 0.10} give equal {@code Money} instances, whereas {@link BigDecimal#equals} alone
 * would consider them different (it compares the scale too).
 *
 * <p>Amounts may be negative (result of a subtraction, a credit note...): the business rules that
 * forbid a negative expense belong to the aggregates, not to this value object.
 *
 * @param amount the amount, normalised to {@value #SCALE} decimals
 * @param currency the ISO 4217 currency
 */
public record Money(BigDecimal amount, Currency currency) {

  /** Number of decimals kept for every amount. */
  public static final int SCALE = 2;

  /**
   * Rounding applied when an amount or a computation result has more decimals than {@link #SCALE}.
   */
  public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

  public Money {
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(currency, "currency");
    amount = amount.setScale(SCALE, ROUNDING);
  }

  /** Builds an amount from its decimal representation, e.g. {@code Money.of("12.50", "EUR")}. */
  public static Money of(String amount, String currencyCode) {
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(currencyCode, "currencyCode");
    return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
  }

  public static Money of(BigDecimal amount, Currency currency) {
    return new Money(amount, currency);
  }

  public static Money zero(Currency currency) {
    return new Money(BigDecimal.ZERO, currency);
  }

  /**
   * Sum of the two amounts.
   *
   * @throws CurrencyMismatchException if {@code other} is in another currency
   */
  public Money add(Money other) {
    requireSameCurrency(other);
    return new Money(amount.add(other.amount), currency);
  }

  /**
   * Difference of the two amounts (may be negative).
   *
   * @throws CurrencyMismatchException if {@code other} is in another currency
   */
  public Money subtract(Money other) {
    requireSameCurrency(other);
    return new Money(amount.subtract(other.amount), currency);
  }

  /**
   * Multiplies the amount by a factor (quantity, VAT rate, exchange rate...). The exact product is
   * computed first, then rounded once to {@value #SCALE} decimals with {@link #ROUNDING}.
   */
  public Money multiply(BigDecimal factor) {
    Objects.requireNonNull(factor, "factor");
    return new Money(amount.multiply(factor), currency);
  }

  /**
   * Whether this amount is strictly greater than {@code other} (e.g. a line above its category
   * ceiling).
   *
   * @throws CurrencyMismatchException if {@code other} is in another currency
   */
  public boolean isGreaterThan(Money other) {
    requireSameCurrency(other);
    return amount.compareTo(other.amount) > 0;
  }

  public boolean isZero() {
    return amount.signum() == 0;
  }

  public boolean isNegative() {
    return amount.signum() < 0;
  }

  /** E.g. {@code 12.50 EUR}. */
  @Override
  public String toString() {
    return amount.toPlainString() + " " + currency.getCurrencyCode();
  }

  private void requireSameCurrency(Money other) {
    Objects.requireNonNull(other, "other");
    if (!currency.equals(other.currency)) {
      throw new CurrencyMismatchException(currency, other.currency);
    }
  }
}
