package com.worksoft.company.domain.exception;

public final class BusinessException extends RuntimeException {
  public enum Code {
    VALIDATION_ERROR,
    FORBIDDEN,
    COMPANY_NOT_FOUND,
    COMPANY_CONFLICT,
    IDEMPOTENCY_CONFLICT,
    TEMPORARILY_UNAVAILABLE,
    ACCOUNT_NOT_FOUND,
    ACCOUNT_CONFLICT,
    ACCOUNT_VERSION_CONFLICT,
    ACCOUNT_HIERARCHY_CONFLICT,
    PARTY_NOT_FOUND,
    PARTY_CONFLICT,
    VERSION_CONFLICT,
    VOUCHER_NOT_FOUND,
    VOUCHER_POSTED,
    VOUCHER_UNBALANCED,
    INVALID_JOURNAL_LINE,
    POSTING_REFERENCE_INVALID
  }

  private final Code code;

  public BusinessException(Code code) {
    super(code.name());
    this.code = code;
  }

  public Code code() {
    return code;
  }
}
