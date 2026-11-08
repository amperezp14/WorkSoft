package com.worksoft.company;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.worksoft.company.domain.model.*;
import com.worksoft.company.domain.exception.BusinessException;
import static org.assertj.core.api.Assertions.*;

class DomainTests {
    @Test void preservesNitAndNormalizesName() {
        var c=new Company(new CompanyId(UUID.randomUUID()),"  Ejemplo  ",new TaxIdentification("000123","0"),Instant.EPOCH);
        assertThat(c.legalName()).isEqualTo("Ejemplo");assertThat(c.taxIdentification().nit()).isEqualTo("000123");
        assertThat(c.country()).isEqualTo("CO");
    }
    @Test void rejectsInvalidIdentificationAndBlankNames() {
        assertThatThrownBy(()->new TaxIdentification("1.234","0")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->new TaxIdentification("123","10")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->new Company(new CompanyId(UUID.randomUUID()),"   ",new TaxIdentification("1","0"),Instant.EPOCH))
            .isInstanceOf(BusinessException.class);
    }
}
