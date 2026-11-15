import { Component, DestroyRef, inject } from '@angular/core';
import { ActivatedRoute, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, finalize, switchMap } from 'rxjs';
import { CompaniesApi } from '../companies/companies.api';
import { apiErrorMessage } from '../../core/api-error';
import { CompanyContext } from './company-context';
@Component({
  selector: 'app-company-workspace',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: ` <a class="back-link" routerLink="/empresas">← Volver a mis empresas</a>
    @if (context.loading()) {
      <section class="empty-state" role="status">
        <span class="spinner"></span>
        <h1>Abriendo empresa…</h1>
      </section>
    } @else if (context.error()) {
      <section class="panel empty-state">
        <h1>Empresa no disponible</h1>
        <p role="alert">{{ context.error() }}</p>
        <a class="button secondary" routerLink="/empresas">Seleccionar otra empresa</a>
      </section>
    } @else if (context.company(); as company) {
      <div class="page-heading company-workspace-heading">
        <div>
          <span class="eyebrow">ESTÁS TRABAJANDO EN</span>
          <h1>{{ company.legalName }}</h1>
          <p>
            NIT {{ company.nit }}-{{ company.verificationDigit }}
            <span class="context-separator">·</span> Colombia
            <span class="context-separator">·</span> COP
          </p>
        </div>
        <a class="button secondary" routerLink="/empresas">Cambiar empresa</a>
      </div>
      <nav class="company-tabs" aria-label="Secciones de la empresa">
        <a
          [routerLink]="['/empresas', company.id]"
          routerLinkActive="selected"
          [routerLinkActiveOptions]="{ exact: true }"
          ariaCurrentWhenActive="page"
          >Módulos</a
        >
        <a
          [routerLink]="['/empresas', company.id, 'contabilidad']"
          routerLinkActive="selected"
          ariaCurrentWhenActive="page"
          >Contabilidad</a
        >
        <a
          [routerLink]="['/empresas', company.id, 'facturacion']"
          routerLinkActive="selected"
          ariaCurrentWhenActive="page"
          >Facturación</a
        >
        <a
          [routerLink]="['/empresas', company.id, 'informacion']"
          routerLinkActive="selected"
          ariaCurrentWhenActive="page"
          >Datos de la empresa</a
        >
      </nav>
      <router-outlet />
    }`,
})
export class CompanyWorkspace {
  readonly context = inject(CompanyContext);
  private readonly api = inject(CompaniesApi);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy = inject(DestroyRef);
  constructor() {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          this.context.clear();
          this.context.loading.set(true);
          return this.api.get(params.get('id') ?? '').pipe(
            catchError((error) => {
              this.context.error.set(apiErrorMessage(error));
              return EMPTY;
            }),
            finalize(() => this.context.loading.set(false)),
          );
        }),
        takeUntilDestroyed(this.destroy),
      )
      .subscribe((company) => this.context.company.set(company));
    this.destroy.onDestroy(() => this.context.clear());
  }
}
