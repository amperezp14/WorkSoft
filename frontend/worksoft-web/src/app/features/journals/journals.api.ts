import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RuntimeConfigService } from '../../core/runtime-config';
import type { Page } from '../parties/parties.api';
export interface JournalHeader {
  date: string;
  type: 'GENERAL' | 'INGRESO' | 'EGRESO' | 'AJUSTE';
  origin: string;
  description: string;
  supportReference: string;
  preparedBy: string;
}
export interface JournalLine {
  accountId: string;
  partyId: string;
  description: string;
  debit: string;
  credit: string;
}
export interface JournalValues {
  header: JournalHeader;
  lines: JournalLine[];
  version: number;
}
export interface AccountOption {
  id: string;
  code: string;
  name: string;
}
export interface PartyOption {
  id: string;
  documentType: string;
  documentNumber: string;
  verificationDigit: string | null;
  name: string;
}
export interface ResolvedLine extends JournalLine {
  accountCode: string;
  accountName: string;
  partyDocumentType: string;
  partyDocumentNumber: string;
  partyVerificationDigit: string | null;
  partyName: string;
}
export interface JournalDocument extends Omit<JournalValues, 'lines'> {
  id: string;
  companyId: string;
  status: 'DRAFT' | 'POSTED';
  number: number | null;
  displayNumber: string;
  company: { name: string; nit: string; verificationDigit: string };
  lines: ResolvedLine[];
  totalDebit: string;
  totalCredit: string;
  createdAt: string;
  updatedAt: string;
  postedAt: string | null;
}
export interface JournalSummary {
  id: string;
  date: string;
  type: string;
  status: 'DRAFT' | 'POSTED';
  displayNumber: string;
  description: string;
  totalDebit: string;
  totalCredit: string;
  version: number;
}
export interface PostingOptions {
  accounts: AccountOption[];
  parties: PartyOption[];
  moreAccounts: boolean;
  moreParties: boolean;
}
@Injectable({ providedIn: 'root' })
export class JournalsApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);
  private base(company: string) {
    return (
      this.config.config.apiBaseUrl + '/companies/' + encodeURIComponent(company) + '/journals'
    );
  }
  list(company: string, q: string, page: number) {
    return this.http.get<Page<JournalSummary>>(this.base(company), {
      params: new HttpParams().set('q', q).set('page', page).set('size', 25),
    });
  }
  get(company: string, id: string) {
    return this.http.get<JournalDocument>(this.base(company) + '/' + encodeURIComponent(id));
  }
  create(company: string, values: JournalValues, key: string) {
    return this.http.post<JournalDocument>(this.base(company), values, {
      headers: { 'Idempotency-Key': key },
    });
  }
  update(company: string, id: string, values: JournalValues) {
    return this.http.put<JournalDocument>(
      this.base(company) + '/' + encodeURIComponent(id),
      values,
    );
  }
  post(company: string, id: string, version: number) {
    return this.http.post<JournalDocument>(
      this.base(company) + '/' + encodeURIComponent(id) + '/post',
      { version },
    );
  }
  options(company: string, a: string, p: string) {
    return this.http.get<PostingOptions>(this.base(company) + '/options', {
      params: new HttpParams().set('accountQuery', a).set('partyQuery', p),
    });
  }
}
