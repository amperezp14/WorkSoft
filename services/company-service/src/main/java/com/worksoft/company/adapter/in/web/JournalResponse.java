package com.worksoft.company.adapter.in.web;

import com.worksoft.company.domain.journal.JournalDocument;
import com.worksoft.company.domain.journal.JournalHeader;
import com.worksoft.company.domain.journal.JournalSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JournalResponse(
    UUID id,
    UUID companyId,
    JournalHeader header,
    String status,
    Long number,
    String displayNumber,
    JournalDocument.CompanySnapshot company,
    List<Line> lines,
    String totalDebit,
    String totalCredit,
    long version,
    Instant createdAt,
    Instant updatedAt,
    Instant postedAt) {
  public record Line(
      UUID accountId,
      String accountCode,
      String accountName,
      UUID partyId,
      String partyDocumentType,
      String partyDocumentNumber,
      String partyVerificationDigit,
      String partyName,
      String description,
      String debit,
      String credit) {}

  public record Summary(
      UUID id,
      LocalDate date,
      String type,
      String status,
      Long number,
      String displayNumber,
      String description,
      String totalDebit,
      String totalCredit,
      long version) {}

  public static JournalResponse from(JournalDocument doc) {
    var draft = doc.draft();
    var lines =
        doc.lines().stream()
            .map(
                row ->
                    new Line(
                        row.entry().accountId(),
                        row.account().code(),
                        row.account().name(),
                        row.entry().partyId(),
                        row.party().documentType(),
                        row.party().documentNumber(),
                        row.party().verificationDigit(),
                        row.party().name(),
                        row.entry().description(),
                        row.entry().debit().toPlainString(),
                        row.entry().credit().toPlainString()))
            .toList();
    return new JournalResponse(
        draft.id(),
        draft.companyId(),
        draft.header(),
        doc.status().name(),
        doc.number(),
        doc.displayNumber(),
        doc.company(),
        lines,
        draft.totalDebit().toPlainString(),
        draft.totalCredit().toPlainString(),
        draft.version(),
        doc.createdAt(),
        doc.updatedAt(),
        doc.postedAt());
  }

  public static Summary summary(JournalSummary s) {
    String number =
        s.number() == null ? "Borrador" : "CC-%04d-%06d".formatted(s.date().getYear(), s.number());
    return new Summary(
        s.id(),
        s.date(),
        s.type().name(),
        s.status().name(),
        s.number(),
        number,
        s.description(),
        s.totalDebit().toPlainString(),
        s.totalCredit().toPlainString(),
        s.version());
  }
}
