package com.spendly.application.service;

import com.spendly.application.port.in.GetCurrentUserUseCase;
import com.spendly.application.port.out.CurrentUser;
import com.spendly.domain.identity.AuthenticatedUser;
import org.springframework.stereotype.Service;

/**
 * Implements {@link GetCurrentUserUseCase}. No {@code @Transactional}: the identity comes from the
 * token, nothing is read from the database (department and manager will come from the {@code
 * employee} table later, ADR 0003).
 */
@Service
class CurrentUserService implements GetCurrentUserUseCase {

  private final CurrentUser currentUser;

  CurrentUserService(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  public AuthenticatedUser getCurrentUser() {
    return currentUser.get();
  }
}
