package com.worksoft.company.application.port.out;

import java.util.List;
import java.util.Optional;
import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.domain.model.Company;
import com.worksoft.company.domain.model.CompanyId;

public interface CompanyStore {
    /** Transacción local: empresa, membresía, idempotencia, auditoría y outbox, o rollback completo. */
    Company provision(Company candidate, Actor actor, String key, String requestHash);
    Optional<Company> findAccessible(Actor actor, CompanyId id);
    List<Company> listAccessible(Actor actor, PagePosition after, int limit);
}
