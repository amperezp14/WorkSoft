import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { CompaniesApi, Company } from './companies.api';
import { apiErrorMessage, bogotaDate } from '../../core/api-error';
import { Icon } from '../../shared/icon';
@Component({
  selector: 'app-company-detail-page',
  imports: [RouterLink, Icon],
  template: ` <a class="back-link" routerLink="/empresas">← Volver a mis empresas</a>
    @if (loading()) {
      <div class="empty-state" role="status">
        <span class="spinner"></span>
        <h2>Cargando empresa…</h2>
      </div>
    } @else if (error()) {
      <section class="panel empty-state">
        <h1>Empresa no disponible</h1>
        <p role="alert">{{ error() }}</p>
        <button class="button secondary" (click)="load()">Volver a intentar</button>
      </section>
    } @else if (company(); as c) {
      <div class="page-heading">
        <div>
          <span class="eyebrow">INFORMACIÓN GENERAL</span>
          <h1>{{ c.legalName }}</h1>
          <p>NIT {{ c.nit }}-{{ c.verificationDigit }}</p>
        </div>
        <span class="status"><span></span> Activa en WorkSoft</span>
      </div>
      <div class="form-layout">
        <section class="panel detail-panel">
          <div class="form-intro">
            <span class="summary-icon"><app-icon /></span>
            <div>
              <h2>Datos de la empresa</h2>
              <p>La base de tu espacio de gestión.</p>
            </div>
          </div>
          <dl class="detail-list">
            <div>
              <dt>Razón social</dt>
              <dd>{{ c.legalName }}</dd>
            </div>
            <div>
              <dt>NIT</dt>
              <dd>{{ c.nit }}-{{ c.verificationDigit }}</dd>
            </div>
            <div>
              <dt>País</dt>
              <dd>Colombia</dd>
            </div>
            <div>
              <dt>Moneda</dt>
              <dd>Peso colombiano (COP)</dd>
            </div>
            <div>
              <dt>Zona horaria</dt>
              <dd>{{ c.timeZone }}</dd>
            </div>
            <div>
              <dt>Fecha de registro · Colombia</dt>
              <dd>{{ date(c.createdAt) }}</dd>
            </div>
          </dl>
        </section>
        <aside class="context-panel">
          <app-icon name="lock" />
          <h2>Un espacio independiente</h2>
          <p>Los datos de esta empresa están separados de los de tus otras organizaciones.</p>
          <p class="muted">
            “Activa” describe su estado dentro de WorkSoft y no acredita su situación jurídica o
            tributaria ante la DIAN.
          </p>
        </aside>
      </div>
    }`,
})
export class CompanyDetailPage {
  private readonly api = inject(CompaniesApi);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy = inject(DestroyRef);
  readonly company = signal<Company | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly date = bogotaDate;
  constructor() {
    this.load();
  }
  load(): void {
    if (this.loading()) return;
    this.error.set('');
    this.loading.set(true);
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.api
      .get(id)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (company) => this.company.set(company),
        error: (err) => this.error.set(apiErrorMessage(err)),
      });
  }
}
