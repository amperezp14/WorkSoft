package com.worksoft.company.domain.journal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record JournalSummary(
    UUID id,
    LocalDate date,
    JournalHeader.Type type,
    JournalDocument.Status status,
    Long number,
    String description,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    long version) {}
