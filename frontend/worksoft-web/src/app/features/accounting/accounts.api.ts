import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RuntimeConfigService } from '../../core/runtime-config';
export type Nature = 'DEBIT' | 'CREDIT';
export interface AccountValues {
  code: string;
  name: string;
  nature: Nature;
  active: boolean;
  allowsPosting: boolean;
  notes: string;
  version: number;
}
export interface Account extends AccountValues {
  id: string;
  companyId: string;
  updatedAt: string;
  level?: string;
  parentCode?: string | null;
}
export interface AccountPage {
  items: Account[];
  total: number;
  page: number;
  size: number;
}
@Injectable({ providedIn: 'root' })
export class AccountsApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);
  private url(company: string): string {
    return (
      this.config.config.apiBaseUrl + '/companies/' + encodeURIComponent(company) + '/accounts'
    );
  }
  list(company: string, query: string, page: number): Observable<AccountPage> {
    const params = new HttpParams().set('q', query).set('page', page).set('size', 25);
    return this.http.get<AccountPage>(this.url(company), { params });
  }
  get(company: string, id: string): Observable<Account> {
    return this.http.get<Account>(this.url(company) + '/' + encodeURIComponent(id));
  }
  create(company: string, values: AccountValues): Observable<Account> {
    return this.http.post<Account>(this.url(company), values);
  }
  update(company: string, id: string, values: AccountValues): Observable<Account> {
    return this.http.put<Account>(this.url(company) + '/' + encodeURIComponent(id), values);
  }
}
