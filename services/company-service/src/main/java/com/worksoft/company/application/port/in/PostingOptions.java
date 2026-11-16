package com.worksoft.company.application.port.in;

import java.util.List;
import java.util.UUID;

public record PostingOptions(
    List<AccountOption> accounts,
    List<PartyOption> parties,
    boolean moreAccounts,
    boolean moreParties) {
  public record AccountOption(UUID id, String code, String name) {}

  public record PartyOption(
      UUID id, String documentType, String documentNumber, String verificationDigit, String name) {}

  public PostingOptions {
    accounts = List.copyOf(accounts);
    parties = List.copyOf(parties);
  }
}
