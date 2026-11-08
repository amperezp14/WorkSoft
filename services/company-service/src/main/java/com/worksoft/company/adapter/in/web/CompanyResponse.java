package com.worksoft.company.adapter.in.web;

import java.time.Instant;
import java.util.UUID;
import com.worksoft.company.domain.model.Company;

public record CompanyResponse(UUID id, String legalName, String nit, String verificationDigit,
        String country, String currency, String timeZone, String status, Instant createdAt) {
    public static CompanyResponse from(Company c) {
        return new CompanyResponse(c.id().value(), c.legalName(), c.taxIdentification().nit(),
                c.taxIdentification().verificationDigit(), c.country(), c.currency(), c.timeZone(), c.status(), c.createdAt());
    }
}
