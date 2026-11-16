package com.worksoft.company.application.service;

import static com.worksoft.company.domain.exception.BusinessException.Code.COMPANY_NOT_FOUND;

import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.application.port.out.CompanyStore;
import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.model.CompanyId;
import java.util.UUID;

public final class CompanyAccess {
  private final CompanyStore companies;

  public CompanyAccess(CompanyStore companies) {
    this.companies = companies;
  }

  public void require(Actor actor, UUID companyId) {
    companies
        .findAccessible(actor, new CompanyId(companyId))
        .orElseThrow(() -> new BusinessException(COMPANY_NOT_FOUND));
  }
}
