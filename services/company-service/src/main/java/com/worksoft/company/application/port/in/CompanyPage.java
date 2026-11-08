package com.worksoft.company.application.port.in;

import java.util.List;
import com.worksoft.company.domain.model.Company;

public record CompanyPage(List<Company> items, String nextCursor) {
    public CompanyPage { items = List.copyOf(items); }
}
