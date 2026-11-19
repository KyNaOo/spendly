package com.spendly.adapter.in.web;

import com.spendly.domain.identity.ForbiddenAccessException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns every authorization refusal into an HTTP 403 {@link ProblemDetail} (RFC 7807, {@code
 * application/problem+json}).
 *
 * <ul>
 *   <li>{@link AccessDeniedException}: a {@code @PreAuthorize} rule failed (missing role). Spring
 *       Security throws {@code AuthorizationDeniedException}, a subclass. The detail stays generic:
 *       the expression is never disclosed.
 *   <li>{@link ForbiddenAccessException}: a business access rule of the domain refused the
 *       operation (not the owner, not the manager of the author).
 * </ul>
 *
 * <p>Highest precedence so that a future catch-all handler ({@code @ExceptionHandler(Exception)})
 * can never turn a 403 into a 500. The filter chain reuses this advice for the refusals raised
 * before reaching a controller ({@code config.ProblemDetailAccessDeniedHandler}).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class AccessDeniedExceptionHandler {

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(HttpServletRequest request) {
    return forbidden("You are not allowed to perform this operation", request);
  }

  @ExceptionHandler(ForbiddenAccessException.class)
  ProblemDetail handleForbiddenAccess(ForbiddenAccessException e, HttpServletRequest request) {
    ProblemDetail problem = forbidden(e.getMessage(), request);
    problem.setProperty("reason", e.reason().name());
    return problem;
  }

  private static ProblemDetail forbidden(String detail, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
    problem.setInstance(URI.create(request.getRequestURI()));
    return problem;
  }
}
