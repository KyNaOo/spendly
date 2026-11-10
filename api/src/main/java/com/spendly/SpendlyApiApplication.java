package com.spendly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpendlyApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpendlyApiApplication.class, args);
  }
}
