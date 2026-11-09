package com.spendly;

import org.springframework.boot.SpringApplication;

/**
 * Runs the application locally against a Testcontainers PostgreSQL ({@code ./mvnw
 * spring-boot:test-run}), without any manually started database.
 */
public class TestSpendlyApiApplication {

  public static void main(String[] args) {
    SpringApplication.from(SpendlyApiApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
