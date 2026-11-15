package com.worksoft.company.application.port.in;


import java.util.UUID;
import com.worksoft.company.domain.accounting.Account;
public interface AccountUseCases {
    record Values(String code,String name,Account.Nature nature,boolean active,boolean allowsPosting,String notes,long version) { }
    AccountPage list(Actor actor,UUID companyId,String query,int page,int size);
    Account get(Actor actor,UUID companyId,UUID id);
    Account create(Actor actor,UUID companyId,Values values);
    Account update(Actor actor,UUID companyId,UUID id,Values values);
}
