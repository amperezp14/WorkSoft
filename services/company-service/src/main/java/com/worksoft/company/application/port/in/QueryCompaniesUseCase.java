package com.worksoft.company.application.port.in;

import com.worksoft.company.domain.model.Company;
import com.worksoft.company.domain.model.CompanyId;

public interface QueryCompaniesUseCase {
    Company get(Actor actor, CompanyId id);
    CompanyPage list(Actor actor, int limit, String cursor);
}
