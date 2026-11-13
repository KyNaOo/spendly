package com.spendly.adapter.in.web;

import com.spendly.application.port.in.GetCurrentUserUseCase;
import com.spendly.domain.identity.AuthenticatedUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Identity of the caller, used by the front-end (user name, role-based menus). */
@RestController
@RequestMapping("/api/me")
class MeController {

  private final GetCurrentUserUseCase getCurrentUser;

  MeController(GetCurrentUserUseCase getCurrentUser) {
    this.getCurrentUser = getCurrentUser;
  }

  @GetMapping
  MeResponse me() {
    return MeResponse.from(getCurrentUser.getCurrentUser());
  }

  /**
   * Response body of {@code GET /api/me}.
   *
   * @param id Keycloak subject ({@code sub})
   * @param name display name
   * @param roles application roles, in a stable order
   */
  record MeResponse(String id, String name, List<String> roles) {

    static MeResponse from(AuthenticatedUser user) {
      List<String> roles = user.roles().stream().sorted().map(Enum::name).toList();
      return new MeResponse(user.id(), user.name(), roles);
    }
  }
}
