package com.worksoft.company.domain.parties;

import static com.worksoft.company.domain.exception.BusinessException.Code.VALIDATION_ERROR;

import com.worksoft.company.domain.exception.BusinessException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record ThirdParty(
    UUID id,
    UUID companyId,
    DocumentType documentType,
    String documentNumber,
    String verificationDigit,
    String name,
    Category category,
    String email,
    String phone,
    String address,
    boolean active,
    long version,
    Instant updatedAt) {
  public enum DocumentType {
    NIT,
    CC,
    CE,
    PASSPORT
  }

  public enum Category {
    CUSTOMER,
    SUPPLIER,
    BOTH,
    OTHER
  }

  public ThirdParty {
    if (id == null
        || companyId == null
        || documentType == null
        || category == null
        || updatedAt == null
        || version < 0) invalid();
    documentNumber = text(documentNumber, 30).toUpperCase(Locale.ROOT);
    name = text(name, 200);
    email = optional(email, 200);
    phone = optional(phone, 30);
    address = optional(address, 250);
    if (documentType == DocumentType.NIT) {
      if (!documentNumber.matches("[0-9]{1,15}")
          || verificationDigit == null
          || !verificationDigit.matches("[0-9]")) invalid();
    } else {
      if (!documentNumber.matches("[A-Z0-9-]{1,30}")
          || verificationDigit != null && !verificationDigit.isBlank()) invalid();
      verificationDigit = null;
    }
    if (!email.isEmpty() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) invalid();
  }

  private static String text(String value, int max) {
    String result = optional(value, max);
    if (result.isEmpty()) invalid();
    return result;
  }

  private static String optional(String value, int max) {
    String result = value == null ? "" : value.strip();
    if (result.length() > max) invalid();
    return result;
  }

  private static void invalid() {
    throw new BusinessException(VALIDATION_ERROR);
  }
}
