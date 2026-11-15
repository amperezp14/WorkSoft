package com.worksoft.company.domain.accounting;


import java.time.Instant;
import java.util.UUID;
import java.util.Objects;
import com.worksoft.company.domain.exception.BusinessException;
import static com.worksoft.company.domain.exception.BusinessException.Code.VALIDATION_ERROR;
public record Account(UUID id, UUID companyId, String code, String name, Nature nature,
        boolean active, boolean allowsPosting, String notes, long version, Instant updatedAt) {
    public enum Nature { DEBIT, CREDIT }
    public Account {
        Objects.requireNonNull(id); Objects.requireNonNull(companyId); Objects.requireNonNull(updatedAt);
        if (code==null || !code.matches("[1-9][0-9]*") || !java.util.Set.of(1,2,4,6,8,10).contains(code.length()) || name==null || nature==null || version<0)
            throw new BusinessException(VALIDATION_ERROR);
        name=name.strip(); notes=notes==null ? "" : notes.strip();
        if (name.isBlank() || name.length()>200 || notes.length()>1000 || allowsPosting && code.length()<6)
            throw new BusinessException(VALIDATION_ERROR);
    }
    public String parentCode() { return code.length()==1 ? null : code.substring(0,code.length()==2 ? 1 : code.length()-2); }
    public String level() { return switch(code.length()) { case 1 -> "CLASS"; case 2 -> "GROUP"; case 4 -> "ACCOUNT"; case 6 -> "SUBACCOUNT"; default -> "AUXILIARY"; }; }
}
