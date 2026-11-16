package com.worksoft.company.domain.journal;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JournalDocument(
    JournalDraft draft,
    Status status,
    Long number,
    CompanySnapshot company,
    List<ResolvedLine> lines,
    Instant createdAt,
    Instant updatedAt,
    Instant postedAt) {
  public enum Status {
    DRAFT,
    POSTED
  }

  public record CompanySnapshot(String name, String nit, String verificationDigit) {}

  public record AccountSnapshot(UUID id, String code, String name) {}

  public record PartySnapshot(
      UUID id, String documentType, String documentNumber, String verificationDigit, String name) {}

  public record ResolvedLine(JournalLine entry, AccountSnapshot account, PartySnapshot party) {}

  public JournalDocument {
    lines = List.copyOf(lines);
    if (status == Status.POSTED) draft.requireBalanced();
  }

  public void requireEditable(long expectedVersion) {
    if (status == Status.POSTED) throw new BusinessException(VOUCHER_POSTED);
    if (draft.version() != expectedVersion) throw new BusinessException(VERSION_CONFLICT);
  }

  public String displayNumber() {
    return number == null
        ? "Borrador"
        : "CC-%04d-%06d".formatted(draft.header().date().getYear(), number);
  }
}
