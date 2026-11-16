package com.worksoft.company.application.port.in;

import static com.worksoft.company.domain.exception.BusinessException.Code.VALIDATION_ERROR;

import com.worksoft.company.domain.exception.BusinessException;

public record SearchPage(String query, int page, int size) {
  public SearchPage {
    if (query == null
        || query.length() > 100
        || page < 0
        || page > 100000
        || size < 1
        || size > 100) throw new BusinessException(VALIDATION_ERROR);
    query = query.strip();
  }
}
