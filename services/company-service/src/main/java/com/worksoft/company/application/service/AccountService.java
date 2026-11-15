package com.worksoft.company.application.service;


import java.time.Clock;
import java.util.UUID;
import com.worksoft.company.domain.accounting.Account;
import com.worksoft.company.domain.model.CompanyId;
import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.application.port.in.*;
import com.worksoft.company.application.port.out.*;
import static com.worksoft.company.domain.exception.BusinessException.Code.*;
public final class AccountService implements AccountUseCases {
    private final CompanyStore companies;
    private final AccountStore accounts;
    private final Clock clock;
    public AccountService(CompanyStore companies,AccountStore accounts,Clock clock) { this.companies=companies;this.accounts=accounts;this.clock=clock; }
    private void accessible(Actor actor,UUID companyId) {
        companies.findAccessible(actor,new CompanyId(companyId)).orElseThrow(()->new BusinessException(COMPANY_NOT_FOUND));
    }
    @Override public AccountPage list(Actor actor,UUID companyId,String query,int page,int size) {
        accessible(actor,companyId);
        if(query==null || query.length()>100 || page<0 || page>100000 || size<1 || size>100) throw new BusinessException(VALIDATION_ERROR);
        return accounts.list(companyId,query.strip(),page,size);
    }
    private Account value(UUID id,UUID companyId,Values v,long version) {
        return new Account(id,companyId,v.code(),v.name(),v.nature(),v.active(),v.allowsPosting(),v.notes(),version,clock.instant());
    }
    @Override public Account get(Actor actor,UUID companyId,UUID id) { accessible(actor,companyId);return accounts.get(companyId,id); }
    @Override public Account create(Actor actor,UUID companyId,Values values) {
        accessible(actor,companyId);return accounts.create(value(UUID.randomUUID(),companyId,values,0));
    }
    @Override public Account update(Actor actor,UUID companyId,UUID id,Values values) {
        accessible(actor,companyId);return accounts.update(id,value(id,companyId,values,values.version()));
    }
}
