package com.spendly.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** Plain unit test: the fine-grained access rules are tested without Spring. */
class AccessPolicyTest {

  private static final String ALICE = "00000000-0000-4000-8000-000000000001";
  private static final String BOB = "00000000-0000-4000-8000-000000000002";
  private static final String MARC = "00000000-0000-4000-8000-000000000003";
  private static final String OTHER_MANAGER = "00000000-0000-4000-8000-000000000099";

  private final AuthenticatedUser alice = user(ALICE, Role.EMPLOYEE);
  private final AuthenticatedUser marc = user(MARC, Role.EMPLOYEE, Role.MANAGER);

  @Test
  void ownerIsTheAuthor() {
    assertThat(AccessPolicy.isOwner(alice, ALICE)).isTrue();
    assertThat(AccessPolicy.isOwner(alice, BOB)).isFalse();
    assertThat(AccessPolicy.isOwner(marc, ALICE)).isFalse();
  }

  @Test
  void requireOwnerRejectsAnotherEmployee() {
    assertThatCode(() -> AccessPolicy.requireOwner(alice, ALICE)).doesNotThrowAnyException();
    assertThatThrownBy(() -> AccessPolicy.requireOwner(alice, BOB))
        .isInstanceOfSatisfying(
            ForbiddenAccessException.class,
            e -> assertThat(e.reason()).isEqualTo(ForbiddenAccessException.Reason.NOT_OWNER));
  }

  @Test
  void managerOfTheAuthorIsAllowed() {
    assertThat(AccessPolicy.isManagerOf(marc, ALICE, MARC)).isTrue();
    assertThatCode(() -> AccessPolicy.requireManagerOf(marc, ALICE, MARC))
        .doesNotThrowAnyException();
  }

  @Test
  void managerOutsideTheTeamIsRejected() {
    assertThat(AccessPolicy.isManagerOf(marc, BOB, OTHER_MANAGER)).isFalse();
    assertThatThrownBy(() -> AccessPolicy.requireManagerOf(marc, BOB, OTHER_MANAGER))
        .isInstanceOfSatisfying(
            ForbiddenAccessException.class,
            e ->
                assertThat(e.reason())
                    .isEqualTo(ForbiddenAccessException.Reason.NOT_MANAGER_OF_AUTHOR));
  }

  @Test
  void authorWithoutManagerCannotBeManagedByAnyone() {
    assertThat(AccessPolicy.isManagerOf(marc, ALICE, null)).isFalse();
  }

  @Test
  void recordedManagerWithoutTheManagerRoleIsRejected() {
    AuthenticatedUser formerManager = user(MARC, Role.EMPLOYEE);
    assertThat(AccessPolicy.isManagerOf(formerManager, ALICE, MARC)).isFalse();
  }

  @Test
  void managerNeverManagesHimself() {
    // Inconsistent data (an employee recorded as his own manager) must not allow self-approval.
    assertThat(AccessPolicy.isManagerOf(marc, MARC, MARC)).isFalse();
  }

  private static AuthenticatedUser user(String id, Role... roles) {
    return new AuthenticatedUser(id, id, Set.of(roles));
  }
}
