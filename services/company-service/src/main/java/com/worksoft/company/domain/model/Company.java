package com.worksoft.company.domain.model;

import java.time.Instant;
import java.util.Objects;
import com.worksoft.company.domain.exception.BusinessException;
import static com.worksoft.company.domain.exception.BusinessException.Code.VALIDATION_ERROR;

public record Company(CompanyId id, String legalName, TaxIdentification taxIdentification, Instant createdAt) {
    public Company {
        Objects.requireNonNull(id); Objects.requireNonNull(taxIdentification); Objects.requireNonNull(createdAt);
        if (legalName == null) throw new BusinessException(VALIDATION_ERROR);
        legalName = legalName.strip();
        if (legalName.isBlank() || legalName.length() > 200) throw new BusinessException(VALIDATION_ERROR);
    }
    public String country() { return "CO"; }
    public String currency() { return "COP"; }
    public String timeZone() { return "America/Bogota"; }
    public String status() { return "ACTIVE"; }
}
