package com.spendly.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Checks the binding and validation of {@link SpendlyProperties}, and how each profile file fills
 * it. {@link ApplicationContextRunner} starts a tiny context (only this configuration, no database,
 * no web server): much faster than {@code @SpringBootTest}.
 */
class SpendlyPropertiesTest {

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(SpendlyProperties.class)
  static class PropertiesConfiguration {}

  /** Context with only explicitly given properties (application*.yml files are not read). */
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class);

  /** Context that also loads application.yml and application-{profile}.yml, like the real app. */
  private final ApplicationContextRunner withConfigFiles =
      runner.withInitializer(new ConfigDataApplicationContextInitializer());

  @Test
  void bindsTypedValues() {
    runner
        .withPropertyValues(
            "spendly.base-currency=USD", "spendly.frontend-url=https://spendly.example.com")
        .run(context -> {
          SpendlyProperties properties = context.getBean(SpendlyProperties.class);
          assertThat(properties.baseCurrency()).isEqualTo(Currency.getInstance("USD"));
          assertThat(properties.frontendUrl()).isEqualTo(URI.create("https://spendly.example.com"));
        });
  }

  @Test
  void missingRequiredPropertyFailsStartupWithPropertyName() {
    runner
        .withPropertyValues("spendly.base-currency=EUR")
        .run(context ->
            assertThat(context)
                .hasFailed()
                .getFailure()
                .rootCause()
                .isInstanceOf(BindValidationException.class)
                .hasMessageContaining("frontendUrl"));
  }

  @Test
  void invalidCurrencyFailsStartup() {
    runner
        .withPropertyValues(
            "spendly.base-currency=NOT-A-CURRENCY", "spendly.frontend-url=http://localhost:4200")
        .run(context ->
            assertThat(context)
                .hasFailed()
                .getFailure()
                .hasStackTraceContaining(
                    "Failed to bind properties under 'spendly.base-currency'"));
  }

  @Test
  void withoutProfileTheApplicationRefusesToStart() {
    withConfigFiles.run(context ->
        assertThat(context)
            .hasFailed()
            .getFailure()
            .rootCause()
            .isInstanceOf(BindValidationException.class)
            .hasMessageContaining("frontendUrl"));
  }

  @Test
  void devProfileTargetsLocalServices() {
    withConfigFiles
        .withPropertyValues("spring.profiles.active=dev")
        .run(context -> {
          assertThat(context.getBean(SpendlyProperties.class).frontendUrl())
              .isEqualTo(URI.create("http://localhost:4200"));
          assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
              .isEqualTo("jdbc:postgresql://localhost:5432/spendly");
          assertThat(
                  context
                      .getEnvironment()
                      .getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
              .isEqualTo("http://localhost:8180/realms/spendly");
        });
  }

  @Test
  void testProfileBindsWithoutDatasource() {
    withConfigFiles
        .withPropertyValues("spring.profiles.active=test")
        .run(context -> {
          assertThat(context.getBean(SpendlyProperties.class).baseCurrency())
              .isEqualTo(Currency.getInstance("EUR"));
          // The datasource comes from Testcontainers, never from a hard-coded URL.
          assertThat(context.getEnvironment().containsProperty("spring.datasource.url")).isFalse();
        });
  }

  @Test
  void prodProfileReadsEverythingFromEnvironmentVariables() {
    withConfigFiles
        .withPropertyValues(
            "spring.profiles.active=prod",
            "SPENDLY_FRONTEND_URL=https://spendly.example.com",
            "SPENDLY_BASE_CURRENCY=CHF",
            "SPENDLY_DB_URL=jdbc:postgresql://db:5432/spendly",
            "SPENDLY_DB_USERNAME=app",
            "SPENDLY_DB_PASSWORD=from-secret",
            "SPENDLY_OIDC_ISSUER_URI=https://sso.example.com/realms/spendly")
        .run(context -> {
          SpendlyProperties properties = context.getBean(SpendlyProperties.class);
          assertThat(properties.frontendUrl()).isEqualTo(URI.create("https://spendly.example.com"));
          assertThat(properties.baseCurrency()).isEqualTo(Currency.getInstance("CHF"));
          assertThat(context.getEnvironment().getProperty("spring.datasource.password"))
              .isEqualTo("from-secret");
          assertThat(
                  context
                      .getEnvironment()
                      .getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
              .isEqualTo("https://sso.example.com/realms/spendly");
        });
  }
}
