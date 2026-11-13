package com.spendly.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * HTTP security: the API is an OAuth2 <em>resource server</em> that accepts only JWT access tokens
 * issued by Keycloak ({@code Authorization: Bearer ...}).
 *
 * <p>Token validation (signature against the Keycloak public keys, expiration, issuer and audience
 * {@code spendly-api}) is configured by Spring Boot from the {@code
 * spring.security.oauth2.resourceserver.jwt.*} properties (issuer per profile, audience in {@code
 * application.yml}).
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

  /** Routes reachable without a token: health probe and API documentation. */
  static final String[] PUBLIC_ROUTES = {
    "/actuator/health",
    "/actuator/health/**",
    "/swagger-ui.html",
    "/swagger-ui/**",
    "/v3/api-docs/**"
  };

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
    http
        // No server-side session: every request carries its token (no JSESSIONID cookie).
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // CSRF targets cookie-based authentication; a Bearer token is never sent automatically by
        // the browser, so the protection is useless here.
        .csrf(csrf -> csrf.disable())
        // Uses the CorsConfigurationSource bean below (answers the browser preflight requests).
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_ROUTES).permitAll().anyRequest().authenticated())
        // Missing/invalid token -> 401 with a WWW-Authenticate: Bearer header.
        .oauth2ResourceServer(oauth2 ->
            oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
    return http.build();
  }

  /**
   * {@code Converter<Jwt, AbstractAuthenticationToken>}: builds the authentication of a request
   * from its validated token. The principal name is the {@code sub} claim, the authorities come
   * from the Keycloak realm roles.
   */
  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
    converter.setPrincipalClaimName("sub");
    return converter;
  }

  /** CORS: only the Angular front-end ({@code spendly.frontend-url}) may call the API. */
  @Bean
  CorsConfigurationSource corsConfigurationSource(SpendlyProperties properties) {
    CorsConfiguration cors = new CorsConfiguration();
    // An origin has no trailing slash nor path: "http://localhost:4200".
    cors.setAllowedOrigins(List.of(origin(properties)));
    cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(
        List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.IF_MATCH));
    cors.setExposedHeaders(List.of(HttpHeaders.LOCATION, HttpHeaders.ETAG));
    cors.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors);
    return source;
  }

  private static String origin(SpendlyProperties properties) {
    var url = properties.frontendUrl();
    return url.getScheme() + "://" + url.getRawAuthority();
  }
}
