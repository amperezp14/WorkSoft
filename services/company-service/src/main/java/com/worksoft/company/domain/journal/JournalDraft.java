package com.worksoft.company.domain.journal;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record JournalDraft(
    UUID id, UUID companyId, JournalHeader header, List<JournalLine> lines, long version) {
  public JournalDraft {
    if (id == null
        || companyId == null
        || header == null
        || lines == null
        || lines.isEmpty()
        || lines.size() > 200
        || lines.stream().anyMatch(java.util.Objects::isNull)
        || version < 0) throw new BusinessException(VALIDATION_ERROR);
    lines = List.copyOf(lines);
  }

  public BigDecimal totalDebit() {
    return lines.stream().map(JournalLine::debit).reduce(new BigDecimal("0.00"), BigDecimal::add);
  }

  public BigDecimal totalCredit() {
    return lines.stream().map(JournalLine::credit).reduce(new BigDecimal("0.00"), BigDecimal::add);
  }

  public void requireBalanced() {
    if (lines.size() < 2
        || totalDebit().signum() <= 0
        || totalDebit().compareTo(totalCredit()) != 0)
      throw new BusinessException(VOUCHER_UNBALANCED);
  }
}
