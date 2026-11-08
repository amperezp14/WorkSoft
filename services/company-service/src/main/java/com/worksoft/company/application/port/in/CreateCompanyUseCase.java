package com.worksoft.company.application.port.in;

import com.worksoft.company.domain.model.Company;

public interface CreateCompanyUseCase {
    Company create(Actor actor, CreateCompanyCommand command);
}
