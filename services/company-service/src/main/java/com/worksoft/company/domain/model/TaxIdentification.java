package com.worksoft.company.domain.model;

import com.worksoft.company.domain.exception.BusinessException;
import static com.worksoft.company.domain.exception.BusinessException.Code.VALIDATION_ERROR;

/** Validación estructural local; no consulta ni acredita habilitación DIAN. */
public record TaxIdentification(String nit, String verificationDigit) {
    public TaxIdentification {
        if (nit == null || !nit.matches("[0-9]{1,15}") || verificationDigit == null
                || !verificationDigit.matches("[0-9]")) throw new BusinessException(VALIDATION_ERROR);
    }
}
