package com.spendly.domain.identity;

import java.util.Objects;

/**
 * A business access rule refused the operation: the user is authenticated and holds the right role,
 * but is not allowed to act on <em>this</em> resource (not its owner, not the manager of its
 * author...). Translated into HTTP 403 by the web adapter.
 */
public final class ForbiddenAccessException extends RuntimeException {

  /** Which rule refused the access. */
  public enum Reason {
    /** The resource belongs to another employee. */
    NOT_OWNER,
    /** The user is not the manager of the resource's author. */
    NOT_MANAGER_OF_AUTHOR
  }

  private final Reason reason;

  public ForbiddenAccessException(Reason reason, String message) {
    super(message);
    this.reason = Objects.requireNonNull(reason, "reason");
  }

  public Reason reason() {
    return reason;
  }
}
