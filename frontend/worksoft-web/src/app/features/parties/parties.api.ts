import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RuntimeConfigService } from '../../core/runtime-config';
export type DocumentType = 'NIT' | 'CC' | 'CE' | 'PASSPORT';
export type PartyCategory = 'CUSTOMER' | 'SUPPLIER' | 'BOTH' | 'OTHER';
export interface PartyValues {
  documentType: DocumentType;
  documentNumber: string;
  verificationDigit: string | null;
  name: string;
  category: PartyCategory;
  email: string;
  phone: string;
  address: string;
  active: boolean;
  version: number;
}
export interface ThirdParty extends PartyValues {
  id: string;
  companyId: string;
  updatedAt: string;
}
export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}
@Injectable({ providedIn: 'root' })
export class PartiesApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RuntimeConfigService);
  private base(company: string): string {
    return (
      this.config.config.apiBaseUrl + '/companies/' + encodeURIComponent(company) + '/third-parties'
    );
  }
  list(company: string, q: string, page: number) {
    return this.http.get<Page<ThirdParty>>(this.base(company), {
      params: new HttpParams().set('q', q).set('page', page).set('size', 25),
    });
  }
  create(company: string, values: PartyValues) {
    return this.http.post<ThirdParty>(this.base(company), values);
  }
  update(company: string, id: string, values: PartyValues) {
    return this.http.put<ThirdParty>(this.base(company) + '/' + encodeURIComponent(id), values);
  }
}
