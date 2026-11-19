package com.spendly.domain.identity;

import java.util.Objects;

/**
 * Fine-grained access rules that depend on the relationship between the user and a resource, as
 * opposed to the coarse role checks done declaratively with {@code @PreAuthorize} on the
 * application services (ADR 0004).
 *
 * <p>Pure functions without Spring: the application service loads the resource (and the manager of
 * its author, from the {@code employee} table, ADR 0003) within its transaction, then asks this
 * policy before changing anything. The {@code is*} methods answer, the {@code require*} methods
 * throw {@link ForbiddenAccessException}.
 */
public final class AccessPolicy {

  private AccessPolicy() {}

  /** The user is the author (owner) of the resource. */
  public static boolean isOwner(AuthenticatedUser user, String ownerId) {
    Objects.requireNonNull(user, "user");
    return user.id().equals(ownerId);
  }

  /**
   * The user is the manager of the author: holds the {@link Role#MANAGER} role and is recorded as
   * the author's manager. Never true for one's own resource (no self-approval), even if the data
   * were inconsistent.
   *
   * @param authorId identifier of the resource's author
   * @param authorManagerId identifier of the author's manager, {@code null} if the author has none
   */
  public static boolean isManagerOf(
      AuthenticatedUser user, String authorId, String authorManagerId) {
    Objects.requireNonNull(user, "user");
    return user.hasRole(Role.MANAGER)
        && authorManagerId != null
        && user.id().equals(authorManagerId)
        && !user.id().equals(authorId);
  }

  /**
   * Requires {@link #isOwner}.
   *
   * @throws ForbiddenAccessException with reason {@link ForbiddenAccessException.Reason#NOT_OWNER}
   */
  public static void requireOwner(AuthenticatedUser user, String ownerId) {
    if (!isOwner(user, ownerId)) {
      throw new ForbiddenAccessException(
          ForbiddenAccessException.Reason.NOT_OWNER, "Only the author can perform this operation");
    }
  }

  /**
   * Requires {@link #isManagerOf}.
   *
   * @throws ForbiddenAccessException with reason {@link
   *     ForbiddenAccessException.Reason#NOT_MANAGER_OF_AUTHOR}
   */
  public static void requireManagerOf(
      AuthenticatedUser user, String authorId, String authorManagerId) {
    if (!isManagerOf(user, authorId, authorManagerId)) {
      throw new ForbiddenAccessException(
          ForbiddenAccessException.Reason.NOT_MANAGER_OF_AUTHOR,
          "Only the manager of the author can perform this operation");
    }
  }
}
