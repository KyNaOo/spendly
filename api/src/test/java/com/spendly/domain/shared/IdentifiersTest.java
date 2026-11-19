package com.spendly.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Plain unit test: the domain is tested without Spring. */
class IdentifiersTest {

  private static final String UUID_TEXT = "00000000-0000-4000-8000-000000000001";

  @Test
  void reportIdWrapsAUuid() {
    ReportId id = ReportId.of(UUID_TEXT);

    assertThat(id.value()).isEqualTo(UUID.fromString(UUID_TEXT));
    assertThat(id).isEqualTo(new ReportId(UUID.fromString(UUID_TEXT))).hasToString(UUID_TEXT);
    assertThat(ReportId.newId()).isNotEqualTo(ReportId.newId());
    assertThat(ReportId.newId().value().version()).isEqualTo(4);
  }

  @Test
  void reportIdRejectsInvalidValues() {
    assertThatNullPointerException().isThrownBy(() -> new ReportId(null));
    assertThatNullPointerException().isThrownBy(() -> ReportId.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> ReportId.of("not-a-uuid"));
  }

  @Test
  void lineIdWrapsAUuid() {
    LineId id = LineId.of(UUID_TEXT);

    assertThat(id.value()).isEqualTo(UUID.fromString(UUID_TEXT));
    assertThat(id).isEqualTo(new LineId(UUID.fromString(UUID_TEXT))).hasToString(UUID_TEXT);
    assertThat(LineId.newId()).isNotEqualTo(LineId.newId());
    assertThat(LineId.newId().value().version()).isEqualTo(4);
  }

  @Test
  void lineIdRejectsInvalidValues() {
    assertThatNullPointerException().isThrownBy(() -> new LineId(null));
    assertThatNullPointerException().isThrownBy(() -> LineId.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> LineId.of("not-a-uuid"));
  }

  @Test
  void identifiersOfDifferentKindsAreNeverEqual() {
    UUID uuid = UUID.randomUUID();

    assertThat((Object) new ReportId(uuid)).isNotEqualTo(new LineId(uuid));
  }

  @Test
  void userIdWrapsTheKeycloakSubject() {
    UserId id = UserId.of(UUID_TEXT);

    assertThat(id.value()).isEqualTo(UUID_TEXT);
    assertThat(id).isEqualTo(new UserId(UUID_TEXT)).hasToString(UUID_TEXT);
  }

  @Test
  void userIdRejectsMissingOrBlankValues() {
    assertThatNullPointerException().isThrownBy(() -> new UserId(null));
    assertThatIllegalArgumentException().isThrownBy(() -> UserId.of(" "));
  }
}
