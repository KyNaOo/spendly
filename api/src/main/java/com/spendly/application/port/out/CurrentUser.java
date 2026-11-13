package com.spendly.application.port.out;

import com.spendly.domain.identity.AuthenticatedUser;

/**
 * Gives the application the identity of the user behind the current request, without making it
 * depend on Spring Security (the adapter reads the security context, the application only sees
 * {@link AuthenticatedUser}).
 */
public interface CurrentUser {

  /**
   * Returns the authenticated user of the current request.
   *
   * @throws IllegalStateException if no user is authenticated (never happens behind the security
   *     filter chain, which rejects anonymous requests with 401)
   */
  AuthenticatedUser get();
}
