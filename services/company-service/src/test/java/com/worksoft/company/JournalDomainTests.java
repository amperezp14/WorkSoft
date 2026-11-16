package com.worksoft.company;

import static org.assertj.core.api.Assertions.*;

import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.journal.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JournalDomainTests {
  private JournalHeader header() {
    return new JournalHeader(
        LocalDate.of(2026, 11, 16),
        JournalHeader.Type.GENERAL,
        "Operación",
        "Prueba",
        "Archivo externo A-1",
        "Responsable");
  }

  private JournalLine line(String d, String c) {
    return new JournalLine(
        UUID.randomUUID(), UUID.randomUUID(), "", Money.parse(d), Money.parse(c));
  }

  @Test
  void balancesDecimalAmountsExactly() {
    var draft =
        new JournalDraft(
            UUID.randomUUID(),
            UUID.randomUUID(),
            header(),
            List.of(line("0.10", "0"), line("0.20", "0"), line("0", "0.30")),
            0);
    draft.requireBalanced();
    assertThat(draft.totalDebit()).isEqualByComparingTo("0.30");
  }

  @Test
  void keepsLargeAmountPrecision() {
    assertThat(Money.parse("9007199254740993.01"))
        .isEqualByComparingTo(new BigDecimal("9007199254740993.01"));
  }

  @Test
  void rejectsAmbiguousOrRoundedAmounts() {
    for (String value : List.of("1.001", "-1", "1e3", "1,25", "10000000000000000"))
      assertThatThrownBy(() -> Money.parse(value)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> line("1", "1")).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> line("0", "0")).isInstanceOf(BusinessException.class);
  }

  @Test
  void acceptsUnbalancedDraftButRejectsPostingIt() {
    var draft =
        new JournalDraft(
            UUID.randomUUID(), UUID.randomUUID(), header(), List.of(line("10", "0")), 0);
    assertThatThrownBy(draft::requireBalanced).isInstanceOf(BusinessException.class);
  }
}
