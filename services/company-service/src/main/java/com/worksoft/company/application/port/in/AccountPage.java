package com.worksoft.company.application.port.in;


import java.util.List;
import com.worksoft.company.domain.accounting.Account;
public record AccountPage(List<Account> items, long total, int page, int size) {
    public AccountPage { items=List.copyOf(items); }
}
