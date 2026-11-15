import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RuntimeConfigService } from '../../core/runtime-config';
export interface Company {
  id: string;
  legalName: string;
  nit: string;
  verificationDigit: string;
  country: 'CO';
  currency: 'COP';
  timeZone: 'America/Bogota';
  status: 'ACTIVE';
  createdAt: string;
}
export interface CreateCompanyRequest {
  legalName: string;
  nit: string;
  verificationDigit: string;
}
export interface CompanyPage {
  items: Company[];
  nextCursor: string | null;
}
@Injectable({ providedIn: 'root' })
export class CompaniesApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);
  private get base(): string {
    return this.config.config.apiBaseUrl + '/companies';
  }
  list(cursor: string | null = null): Observable<CompanyPage> {
    let params = new HttpParams().set('limit', 20);
    if (cursor) params = params.set('cursor', cursor);
    return this.http.get<CompanyPage>(this.base, { params });
  }
  get(id: string): Observable<Company> {
    return this.http.get<Company>(this.base + '/' + encodeURIComponent(id));
  }
  create(body: CreateCompanyRequest, key: string): Observable<Company> {
    return this.http.post<Company>(this.base, body, { headers: { 'Idempotency-Key': key } });
  }
}
export function normalizeCompany(request: CreateCompanyRequest): CreateCompanyRequest {
  return { ...request, legalName: request.legalName.trim() };
}
/** Se conserva por formulario: un reintento incierto no crea una operación distinta. */
export class IdempotencyKeys {
  private readonly keys = new Map<string, string>();
  for(request: CreateCompanyRequest): string {
    const signature = JSON.stringify([request.legalName, request.nit, request.verificationDigit]);
    if (!this.keys.has(signature)) this.keys.set(signature, crypto.randomUUID());
    return this.keys.get(signature)!;
  }
}
