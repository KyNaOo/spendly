package com.spendly.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Plain unit test: the domain is tested without Spring. */
class AuthenticatedUserTest {

  @Test
  void keepsAnImmutableCopyOfTheRoles() {
    Set<Role> roles = EnumSet.of(Role.EMPLOYEE);
    AuthenticatedUser user = new AuthenticatedUser("sub-1", "Alice Martin", roles);

    roles.add(Role.ADMIN);

    assertThat(user.roles()).containsExactly(Role.EMPLOYEE);
    assertThat(user.hasRole(Role.EMPLOYEE)).isTrue();
    assertThat(user.hasRole(Role.ADMIN)).isFalse();
    assertThatThrownBy(() -> user.roles().add(Role.MANAGER))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void nullRolesMeansNoRole() {
    assertThat(new AuthenticatedUser("sub-1", "Alice", null).roles()).isEmpty();
  }

  @Test
  void requiresAnIdentifier() {
    assertThatThrownBy(() -> new AuthenticatedUser(" ", "Alice", Set.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new AuthenticatedUser(null, "Alice", Set.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void resolvesOnlyApplicationRoles() {
    assertThat(Role.fromName("MANAGER")).contains(Role.MANAGER);
    assertThat(Role.fromName("manager")).isEmpty();
    assertThat(Role.fromName("offline_access")).isEmpty();
    assertThat(Role.fromName(null)).isEmpty();
  }
}
