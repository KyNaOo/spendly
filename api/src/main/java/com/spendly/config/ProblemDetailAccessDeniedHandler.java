package com.spendly.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 403 raised by the filter chain itself (URL rules, before any controller is reached): delegates to
 * Spring MVC's exception resolution, so the response is the very same {@code ProblemDetail} as the
 * one produced by the {@code @RestControllerAdvice} for {@code @PreAuthorize} refusals (single
 * source of truth for the error format).
 */
class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

  private final HandlerExceptionResolver resolver;

  ProblemDetailAccessDeniedHandler(HandlerExceptionResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    if (resolver.resolveException(request, response, null, accessDeniedException) == null) {
      // No handler took it (should not happen): plain 403, like Spring's default handler.
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}
