package com.spendly.application.port.in;

import com.spendly.domain.identity.AuthenticatedUser;

/** Returns who the caller is (identifier, name, roles), e.g. for the front-end menus. */
public interface GetCurrentUserUseCase {

  AuthenticatedUser getCurrentUser();
}
