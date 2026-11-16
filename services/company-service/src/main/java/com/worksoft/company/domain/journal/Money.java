package com.worksoft.company.domain.journal;

import static com.worksoft.company.domain.exception.BusinessException.Code.INVALID_JOURNAL_LINE;

import com.worksoft.company.domain.exception.BusinessException;
import java.math.BigDecimal;

public final class Money {
  private Money() {}

  public static BigDecimal parse(String value) {
    if (value == null || !value.matches("[0-9]{1,16}(\\.[0-9]{1,2})?"))
      throw new BusinessException(INVALID_JOURNAL_LINE);
    return new BigDecimal(value).setScale(2);
  }

  public static BigDecimal require(BigDecimal value) {
    if (value == null
        || value.signum() < 0
        || value.scale() > 2
        || value.precision() - value.scale() > 16)
      throw new BusinessException(INVALID_JOURNAL_LINE);
    return value.setScale(2);
  }
}
