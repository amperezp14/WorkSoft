package com.worksoft.company.application.port.in;

public record CreateCompanyCommand(String legalName, String nit, String verificationDigit, String idempotencyKey) { }
