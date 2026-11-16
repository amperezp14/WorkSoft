package com.worksoft.company;

import static org.assertj.core.api.Assertions.*;

import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.parties.ThirdParty;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ThirdPartyDomainTests {
  private ThirdParty create(ThirdParty.DocumentType type, String number, String dv) {
    return new ThirdParty(
        UUID.randomUUID(),
        UUID.randomUUID(),
        type,
        number,
        dv,
        " Ejemplo ",
        ThirdParty.Category.OTHER,
        "",
        "",
        "",
        true,
        0,
        Instant.EPOCH);
  }

  @Test
  void preservesTextAndSupportsNaturalPersonAndPassport() {
    var natural = create(ThirdParty.DocumentType.CC, "000123", null);
    assertThat(natural.documentNumber()).isEqualTo("000123");
    assertThat(natural.verificationDigit()).isNull();
    assertThat(create(ThirdParty.DocumentType.PASSPORT, "ab-123", null).documentNumber())
        .isEqualTo("AB-123");
  }

  @Test
  void nitRequiresSeparateSingleDigitAndOtherTypesDoNotTakeDv() {
    assertThatThrownBy(() -> create(ThirdParty.DocumentType.NIT, "123", null))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> create(ThirdParty.DocumentType.NIT, "12.3", "0"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> create(ThirdParty.DocumentType.CC, "123", "0"))
        .isInstanceOf(BusinessException.class);
  }
}
