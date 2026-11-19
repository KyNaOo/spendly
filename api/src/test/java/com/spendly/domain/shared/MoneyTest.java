package com.spendly.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Plain unit test: the domain is tested without Spring. */
class MoneyTest {

  private static final Currency EUR = Currency.getInstance("EUR");
  private static final Currency USD = Currency.getInstance("USD");

  @Nested
  class Creation {

    @Test
    void normalisesTheAmountToTwoDecimals() {
      Money money = new Money(new BigDecimal("12.5"), EUR);

      assertThat(money.amount()).isEqualByComparingTo("12.50");
      assertThat(money.amount().scale()).isEqualTo(Money.SCALE);
      assertThat(money.currency()).isEqualTo(EUR);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
      "0.125, 0.12", // tie: rounds towards the even neighbour
      "0.135, 0.14", // tie: rounds towards the even neighbour
      "0.126, 0.13",
      "0.124, 0.12",
      "-0.125, -0.12",
      "-0.135, -0.14",
      "2.5, 2.50",
      "1E+1, 10.00"
    })
    void roundsHalfEven(String raw, String expected) {
      assertThat(Money.of(raw, "EUR").amount()).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void zeroIsZeroWithTwoDecimals() {
      Money zero = Money.zero(EUR);

      assertThat(zero.amount()).isEqualTo(new BigDecimal("0.00"));
      assertThat(zero.isZero()).isTrue();
      assertThat(zero.isNegative()).isFalse();
    }

    @Test
    void factoriesBuildTheSameValue() {
      assertThat(Money.of("3.20", "EUR")).isEqualTo(Money.of(new BigDecimal("3.2"), EUR));
    }

    @Test
    void rejectsMissingOrInvalidValues() {
      assertThatNullPointerException().isThrownBy(() -> new Money(null, EUR));
      assertThatNullPointerException().isThrownBy(() -> new Money(BigDecimal.ONE, null));
      assertThatNullPointerException().isThrownBy(() -> Money.of(null, "EUR"));
      assertThatNullPointerException().isThrownBy(() -> Money.of("1", null));
      assertThatThrownBy(() -> Money.of("abc", "EUR")).isInstanceOf(NumberFormatException.class);
      assertThatThrownBy(() -> Money.of("1", "XYZ")).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class Equality {

    @Test
    void zeroPointTenEqualsZeroPointOne() {
      // BigDecimal alone says no: equals() compares the scale too.
      assertThat(new BigDecimal("0.10")).isNotEqualTo(new BigDecimal("0.1"));

      Money a = Money.of("0.10", "EUR");
      Money b = Money.of("0.1", "EUR");
      assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void sameAmountInAnotherCurrencyIsDifferent() {
      assertThat(Money.of("10", "EUR")).isNotEqualTo(Money.of("10", "USD"));
    }

    @Test
    void avoidsTheDoubleRoundingTrap() {
      assertThat(0.1 + 0.2).isNotEqualTo(0.3);
      assertThat(Money.of("0.1", "EUR").add(Money.of("0.2", "EUR")))
          .isEqualTo(Money.of("0.3", "EUR"));
    }

    @Test
    void printsAmountAndCurrencyCode() {
      assertThat(Money.of("1234.5", "EUR")).hasToString("1234.50 EUR");
      assertThat(Money.of("1E+3", "USD")).hasToString("1000.00 USD");
    }
  }

  @Nested
  class Arithmetic {

    @Test
    void addsAmountsOfTheSameCurrency() {
      assertThat(Money.of("10.25", "EUR").add(Money.of("4.80", "EUR")))
          .isEqualTo(Money.of("15.05", "EUR"));
    }

    @Test
    void subtractsAndMayBecomeNegative() {
      Money result = Money.of("4.80", "EUR").subtract(Money.of("10.25", "EUR"));

      assertThat(result).isEqualTo(Money.of("-5.45", "EUR"));
      assertThat(result.isNegative()).isTrue();
      assertThat(result.isZero()).isFalse();
    }

    @Test
    void operationsDoNotMutateTheOperands() {
      Money ten = Money.of("10", "EUR");

      ten.add(Money.of("5", "EUR"));
      ten.subtract(Money.of("5", "EUR"));
      ten.multiply(new BigDecimal("3"));

      assertThat(ten).isEqualTo(Money.of("10.00", "EUR"));
    }

    @Test
    void multipliesThenRoundsOnceHalfEven() {
      // 100.00 x 0.055 = 5.5 exactly
      assertThat(Money.of("100", "EUR").multiply(new BigDecimal("0.055")))
          .isEqualTo(Money.of("5.50", "EUR"));
      // 10.05 x 0.5 = 5.025 -> tie, even neighbour 5.02
      assertThat(Money.of("10.05", "EUR").multiply(new BigDecimal("0.5")))
          .isEqualTo(Money.of("5.02", "EUR"));
      // 10.15 x 0.5 = 5.075 -> tie, even neighbour 5.08
      assertThat(Money.of("10.15", "EUR").multiply(new BigDecimal("0.5")))
          .isEqualTo(Money.of("5.08", "EUR"));
      // Exchange rate with many decimals: 12.34 x 1.0837 = 13.372858 -> 13.37
      assertThat(Money.of("12.34", "USD").multiply(new BigDecimal("1.0837")))
          .isEqualTo(Money.of("13.37", "USD"));
    }

    @Test
    void multiplicationKeepsTheCurrency() {
      assertThat(Money.of("2", "USD").multiply(BigDecimal.TEN).currency()).isEqualTo(USD);
    }

    @Test
    void rejectsNullOperands() {
      Money ten = Money.of("10", "EUR");

      assertThatNullPointerException().isThrownBy(() -> ten.add(null));
      assertThatNullPointerException().isThrownBy(() -> ten.subtract(null));
      assertThatNullPointerException().isThrownBy(() -> ten.multiply(null));
      assertThatNullPointerException().isThrownBy(() -> ten.isGreaterThan(null));
    }
  }

  @Nested
  class Comparison {

    @Test
    void isGreaterThanIsStrict() {
      Money ceiling = Money.of("25.00", "EUR");

      assertThat(Money.of("25.01", "EUR").isGreaterThan(ceiling)).isTrue();
      assertThat(Money.of("25", "EUR").isGreaterThan(ceiling)).isFalse();
      assertThat(Money.of("24.99", "EUR").isGreaterThan(ceiling)).isFalse();
    }
  }

  @Nested
  class Currencies {

    @Test
    void refusesToAddDifferentCurrencies() {
      assertThatThrownBy(() -> Money.of("10", "EUR").add(Money.of("10", "USD")))
          .isInstanceOfSatisfying(
              CurrencyMismatchException.class,
              e -> {
                assertThat(e.expected()).isEqualTo(EUR);
                assertThat(e.actual()).isEqualTo(USD);
                assertThat(e).hasMessage("Currency mismatch: expected EUR but got USD");
              });
    }

    @Test
    void refusesToSubtractDifferentCurrencies() {
      assertThatThrownBy(() -> Money.of("10", "EUR").subtract(Money.of("1", "USD")))
          .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void refusesToCompareDifferentCurrencies() {
      assertThatThrownBy(() -> Money.of("10", "EUR").isGreaterThan(Money.of("1", "USD")))
          .isInstanceOf(CurrencyMismatchException.class);
    }
  }
}
