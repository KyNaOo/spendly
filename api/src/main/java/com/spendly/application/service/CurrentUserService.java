package com.spendly.application.service;

import com.spendly.application.port.in.GetCurrentUserUseCase;
import com.spendly.application.port.out.CurrentUser;
import com.spendly.domain.identity.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Implements {@link GetCurrentUserUseCase}. No {@code @Transactional}: the identity comes from the
 * token, nothing is read from the database (department and manager will come from the {@code
 * employee} table later, ADR 0003).
 *
 * <p>Authorization (ADR 0004): every application service declares who may call it with
 * {@code @PreAuthorize}, e.g. {@code hasRole('MANAGER')} for the approval queue. Here any
 * authenticated user, even without an application role, may ask who they are (the front-end needs
 * it to show an "access denied" page rather than a menu).
 */
@Service
class CurrentUserService implements GetCurrentUserUseCase {

  private final CurrentUser currentUser;

  CurrentUserService(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public AuthenticatedUser getCurrentUser() {
    return currentUser.get();
  }
}
