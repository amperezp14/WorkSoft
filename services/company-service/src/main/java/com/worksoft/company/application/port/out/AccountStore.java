package com.worksoft.company.application.port.out;


import java.util.UUID;
import com.worksoft.company.domain.accounting.Account;
import com.worksoft.company.application.port.in.AccountPage;
public interface AccountStore {
    AccountPage list(UUID companyId,String query,int page,int size);
    Account get(UUID companyId,UUID id);
    Account create(Account account);
    Account update(UUID id,Account values);
}
