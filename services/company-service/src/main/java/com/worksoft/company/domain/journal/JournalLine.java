package com.worksoft.company.domain.journal;

import static com.worksoft.company.domain.exception.BusinessException.Code.INVALID_JOURNAL_LINE;

import com.worksoft.company.domain.exception.BusinessException;
import java.math.BigDecimal;
import java.util.UUID;

public record JournalLine(
    UUID accountId, UUID partyId, String description, BigDecimal debit, BigDecimal credit) {
  public JournalLine {
    if (accountId == null || partyId == null) throw new BusinessException(INVALID_JOURNAL_LINE);
    description = description == null ? "" : description.strip();
    if (description.length() > 300) throw new BusinessException(INVALID_JOURNAL_LINE);
    debit = Money.require(debit);
    credit = Money.require(credit);
    if ((debit.signum() > 0) == (credit.signum() > 0))
      throw new BusinessException(INVALID_JOURNAL_LINE);
  }
}
