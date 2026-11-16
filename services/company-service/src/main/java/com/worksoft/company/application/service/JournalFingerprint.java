package com.worksoft.company.application.service;

import com.worksoft.company.domain.journal.JournalDraft;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class JournalFingerprint {
  private JournalFingerprint() {}

  static String of(JournalDraft draft) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      var h = draft.header();
      for (String value :
          new String[] {
            h.date().toString(),
            h.type().name(),
            h.origin(),
            h.description(),
            h.supportReference(),
            h.preparedBy()
          }) add(digest, value);
      for (var line : draft.lines())
        for (String value :
            new String[] {
              line.accountId().toString(),
              line.partyId().toString(),
              line.description(),
              line.debit().toPlainString(),
              line.credit().toPlainString()
            }) add(digest, value);
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static void add(MessageDigest digest, String value) {
    digest.update((value.length() + ":" + value).getBytes(StandardCharsets.UTF_8));
  }
}
