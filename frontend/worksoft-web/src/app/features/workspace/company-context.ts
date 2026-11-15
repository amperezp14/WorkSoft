import { Injectable, signal } from '@angular/core';
import type { Company } from '../companies/companies.api';
/** Empresa activa exclusivamente durante la navegación de su espacio. */
@Injectable({ providedIn: 'root' })
export class CompanyContext {
  readonly company = signal<Company | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  clear(): void {
    this.company.set(null);
    this.loading.set(false);
    this.error.set('');
  }
}
