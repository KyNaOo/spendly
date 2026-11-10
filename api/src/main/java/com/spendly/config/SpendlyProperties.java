package com.spendly.config;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.Currency;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed application settings bound from the {@code spendly.*} keys of {@code application*.yml}.
 *
 * <p>Being a record, the instance is immutable and built once at startup through its constructor.
 * {@link Validated} triggers Bean Validation on binding: a missing required property stops the
 * startup with a report naming the property, instead of failing later at first use.
 *
 * @param baseCurrency currency in which reports are reimbursed and exported ({@code
 *     spendly.base-currency}, ISO 4217 code)
 * @param frontendUrl public URL of the Angular front-end, used for CORS and links in e-mails
 *     ({@code spendly.frontend-url})
 */
@Validated
@ConfigurationProperties(prefix = "spendly")
public record SpendlyProperties(@NotNull Currency baseCurrency, @NotNull URI frontendUrl) {}
