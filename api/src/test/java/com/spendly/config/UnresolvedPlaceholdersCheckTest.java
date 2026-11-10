package com.spendly.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spendly.SpendlyApiApplication;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

class UnresolvedPlaceholdersCheckTest {

  private final UnresolvedPlaceholdersCheck check = new UnresolvedPlaceholdersCheck();

  @Test
  void listsEveryMissingEnvironmentVariable() {
    MockEnvironment environment = new MockEnvironment();
    environment
        .getPropertySources()
        .addLast(
            new OriginTrackedMapPropertySource(
                "application-prod.yml",
                Map.of(
                    "spring.datasource.url", "${SPENDLY_DB_URL}",
                    "spendly.frontend-url", "${SPENDLY_FRONTEND_URL}",
                    "spendly.base-currency", "${SPENDLY_BASE_CURRENCY:EUR}",
                    "server.port", 8080)));

    assertThatThrownBy(() -> check.postProcessEnvironment(environment, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("spring.datasource.url")
        .hasMessageContaining("SPENDLY_DB_URL")
        .hasMessageContaining("spendly.frontend-url")
        .hasMessageContaining("SPENDLY_FRONTEND_URL")
        .hasMessageNotContaining("SPENDLY_BASE_CURRENCY");
  }

  @Test
  void acceptsResolvedOrOverriddenValues() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("SPENDLY_DB_URL", "jdbc:postgresql://db/spendly");
    // A higher-priority source (e.g. a SPENDLY_FRONTEND_URL system property) overrides the key.
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "override", Map.of("spendly.frontend-url", "https://spendly.example.com")));
    environment
        .getPropertySources()
        .addLast(
            new OriginTrackedMapPropertySource(
                "application-prod.yml",
                Map.of(
                    "spring.datasource.url", "${SPENDLY_DB_URL}",
                    "spendly.frontend-url", "${SPENDLY_FRONTEND_URL}")));

    assertThatCode(() -> check.postProcessEnvironment(environment, null))
        .doesNotThrowAnyException();
  }

  @Test
  void prodProfileWithoutEnvironmentVariablesFailsBeforeCreatingAnyBean() {
    SpringApplicationBuilder application =
        new SpringApplicationBuilder(SpendlyApiApplication.class)
            .profiles("prod")
            .web(WebApplicationType.NONE);

    assertThatThrownBy(application::run)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing configuration")
        .hasMessageContaining("SPENDLY_DB_URL")
        .hasMessageContaining("SPENDLY_DB_USERNAME")
        .hasMessageContaining("SPENDLY_DB_PASSWORD")
        .hasMessageContaining("SPENDLY_FRONTEND_URL")
        .hasMessageContaining("application-prod.yml");
  }
}
