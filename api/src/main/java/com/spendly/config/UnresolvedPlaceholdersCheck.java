package com.spendly.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

/**
 * Fails the startup, before any bean is created, when a value of an {@code application*.yml} file
 * references an environment variable that is not defined (e.g. {@code ${SPENDLY_DB_URL}} in the
 * {@code prod} profile), and lists every missing one at once.
 *
 * <p>Why: Spring Boot binds configuration leniently, so an unresolved {@code ${VAR}} would
 * otherwise be passed literally to the component (e.g. Hikari) and fail later with an obscure
 * message such as {@code 'url' must start with "jdbc"}.
 *
 * <p>Registered in {@code META-INF/spring.factories}; runs after the config files are loaded.
 */
public class UnresolvedPlaceholdersCheck implements EnvironmentPostProcessor, Ordered {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    List<String> problems = new ArrayList<>();
    for (PropertySource<?> source : environment.getPropertySources()) {
      if (source instanceof OriginTrackedMapPropertySource configFile) {
        for (String name : configFile.getPropertyNames()) {
          if (isEffectiveSource(environment, configFile, name)) {
            check(environment, configFile, name).ifPresent(problems::add);
          }
        }
      }
    }
    if (!problems.isEmpty()) {
      throw new IllegalStateException(
          "Missing configuration (define the environment variables below):\n  - "
              + String.join("\n  - ", problems));
    }
  }

  /** Only the source that wins for this key matters: a higher-priority source may override it. */
  private static boolean isEffectiveSource(
      ConfigurableEnvironment environment, PropertySource<?> candidate, String name) {
    for (PropertySource<?> source : environment.getPropertySources()) {
      // Skip Spring Boot's "configurationProperties" view, which aggregates all other sources.
      if (ConfigurationPropertySources.isAttachedConfigurationPropertySource(source)) {
        continue;
      }
      if (source.containsProperty(name)) {
        return source == candidate;
      }
    }
    return false;
  }

  private static Optional<String> check(
      ConfigurableEnvironment environment, OriginTrackedMapPropertySource source, String name) {
    if (!(source.getProperty(name) instanceof String value) || !value.contains("${")) {
      return Optional.empty();
    }
    try {
      environment.resolveRequiredPlaceholders(value);
      return Optional.empty();
    } catch (IllegalArgumentException ex) {
      Origin origin = source.getOrigin(name);
      return Optional.of(
          name + " (" + (origin != null ? origin : source.getName()) + "): " + ex.getMessage());
    }
  }

  @Override
  public int getOrder() {
    // After ConfigDataEnvironmentPostProcessor, which loads application*.yml.
    return Ordered.LOWEST_PRECEDENCE;
  }
}
