package com.spendly;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway PostgreSQL in Docker for tests. {@code @ServiceConnection} makes Spring Boot
 * point the datasource (and Flyway) at the container, overriding {@code spring.datasource.*}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18-alpine");

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(POSTGRES_IMAGE);
  }
}
