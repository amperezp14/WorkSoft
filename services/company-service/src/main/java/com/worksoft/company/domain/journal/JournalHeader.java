package com.worksoft.company.domain.journal;

import static com.worksoft.company.domain.exception.BusinessException.Code.VALIDATION_ERROR;

import com.worksoft.company.domain.exception.BusinessException;
import java.time.LocalDate;

public record JournalHeader(
    LocalDate date,
    Type type,
    String origin,
    String description,
    String supportReference,
    String preparedBy) {
  public enum Type {
    GENERAL,
    INGRESO,
    EGRESO,
    AJUSTE
  }

  public JournalHeader {
    if (date == null || date.getYear() < 1 || date.getYear() > 9999 || type == null)
      throw new BusinessException(VALIDATION_ERROR);
    origin = required(origin, 200);
    description = required(description, 1000);
    supportReference = required(supportReference, 500);
    preparedBy = required(preparedBy, 100);
  }

  private static String required(String value, int max) {
    if (value == null || value.isBlank() || value.strip().length() > max)
      throw new BusinessException(VALIDATION_ERROR);
    return value.strip();
  }
}
