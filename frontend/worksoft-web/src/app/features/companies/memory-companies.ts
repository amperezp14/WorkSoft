import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import type { Company, CompanyPage, CreateCompanyRequest } from './companies.api';
@Injectable({ providedIn: 'root' })
export class MemoryCompanies {
  private readonly companies = new Map<string, Company>();
  private readonly requests = new Map<string, { signature: string; id: string }>();
  private fail(status: number, code: string): never {
    throw new HttpErrorResponse({ status, error: { code } });
  }
  create(input: CreateCompanyRequest, key: string): Company {
    const body = { ...input, legalName: input.legalName.trim() };
    if (
      !body.legalName ||
      body.legalName.length > 200 ||
      !/^[0-9]{1,15}$/.test(body.nit) ||
      !/^[0-9]$/.test(body.verificationDigit)
    )
      this.fail(400, 'VALIDATION_ERROR');
    const signature = JSON.stringify([body.legalName, body.nit, body.verificationDigit]);
    const previous = this.requests.get(key);
    if (previous) {
      if (previous.signature !== signature) this.fail(409, 'IDEMPOTENCY_CONFLICT');
      return this.get(previous.id);
    }
    if ([...this.companies.values()].some((c) => c.nit === body.nit))
      this.fail(409, 'COMPANY_CONFLICT');
    const company: Company = {
      ...body,
      id: crypto.randomUUID(),
      createdAt: new Date().toISOString(),
      country: 'CO',
      currency: 'COP',
      timeZone: 'America/Bogota',
      status: 'ACTIVE',
    };
    this.companies.set(company.id, company);
    this.requests.set(key, { signature, id: company.id });
    return { ...company };
  }
  get(id: string): Company {
    const company = this.companies.get(id);
    if (!company) this.fail(404, 'COMPANY_NOT_FOUND');
    return { ...company };
  }
  list(cursor: string | null): CompanyPage {
    const offset = cursor === null ? 0 : Number(cursor);
    if (
      !Number.isSafeInteger(offset) ||
      offset < 0 ||
      (cursor !== null && !/^[0-9]+$/.test(cursor))
    )
      this.fail(400, 'VALIDATION_ERROR');
    const all = [...this.companies.values()];
    return {
      items: all.slice(offset, offset + 20).map((c) => ({ ...c })),
      nextCursor: offset + 20 < all.length ? String(offset + 20) : null,
    };
  }
}
