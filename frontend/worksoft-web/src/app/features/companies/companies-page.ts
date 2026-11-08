import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { apiErrorMessage } from '../../core/api-error';
import { Icon } from '../../shared/icon';
import { CompaniesApi, Company } from './companies.api';
@Component({
  selector: 'app-companies-page',
  imports: [RouterLink, Icon],
  template: ` <div class="page-heading">
      <div>
        <span class="eyebrow">ESPACIO DE TRABAJO</span>
        <h1>Mis empresas</h1>
        <p>Todo empieza con una buena organización.</p>
      </div>
      @if (auth.canProvision()) {
        <a class="button primary" routerLink="/empresas/nueva"
          ><app-icon name="plus" /> Registrar empresa</a
        >
      }
    </div>
    <div class="summary-grid">
      <section class="summary-card">
        <span class="summary-icon"><app-icon /></span>
        <div>
          <span>Empresas cargadas</span><strong>{{ companies().length }}</strong>
        </div>
      </section>
      <section class="summary-card">
        <span class="summary-icon mint"><app-icon name="lock" /></span>
        <div>
          <span>Tu acceso</span
          ><strong class="text-value">{{
            auth.canProvision() ? 'Operador de plataforma' : 'Miembro de empresa'
          }}</strong>
        </div>
      </section>
      <section class="summary-card">
        <span class="colombia-flag large" aria-hidden="true"></span>
        <div>
          <span>Entorno empresarial</span
          ><strong class="text-value">Colombia <small>· COP</small></strong>
        </div>
      </section>
    </div>
    <section class="panel" aria-labelledby="companies-title">
      <div class="panel-toolbar">
        <div>
          <h2 id="companies-title">Tus organizaciones</h2>
          <p>Solo verás empresas a las que tienes acceso.</p>
        </div>
        <label class="search"
          ><app-icon name="search" /><span class="sr-only">Buscar en empresas cargadas</span
          ><input
            type="search"
            placeholder="Buscar por nombre o NIT"
            [value]="search()"
            (input)="search.set($any($event.target).value)"
        /></label>
      </div>
      @if (error()) {
        <div class="alert error panel-alert" role="alert">
          {{ error()
          }}<button class="text-button" (click)="load()" [disabled]="loading()">Reintentar</button>
        </div>
      }
      @if (loading() && !companies().length) {
        <div class="empty-state" role="status">
          <span class="spinner"></span>
          <h3>Cargando tus empresas…</h3>
          <p>Estamos consultando tus accesos.</p>
        </div>
      } @else if (!error() && !companies().length) {
        <div class="empty-state">
          <span class="empty-icon"><app-icon /></span>
          <h3>Tu próxima etapa empieza aquí</h3>
          <p>
            {{
              auth.canProvision()
                ? 'Registra tu primera empresa para comenzar a organizar su información.'
                : 'Aún no tienes empresas asignadas. Solicita acceso al administrador de tu organización.'
            }}
          </p>
          @if (auth.canProvision()) {
            <a class="button secondary" routerLink="/empresas/nueva"
              ><app-icon name="plus" /> Registrar mi primera empresa</a
            >
          }
        </div>
      } @else if (companies().length) {
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Empresa</th>
                <th>Identificación tributaria</th>
                <th>Estado</th>
                <th><span class="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (company of filtered(); track company.id) {
                <tr>
                  <td>
                    <div class="company-cell">
                      <span class="company-avatar">{{
                        company.legalName.slice(0, 2).toUpperCase()
                      }}</span
                      ><a [routerLink]="['/empresas', company.id]">{{ company.legalName }}</a>
                    </div>
                  </td>
                  <td>
                    <span class="nit-label">NIT</span> {{ company.nit }}-{{
                      company.verificationDigit
                    }}
                  </td>
                  <td>
                    <span class="status"><span></span> Activa</span>
                  </td>
                  <td>
                    <a
                      class="row-link"
                      [routerLink]="['/empresas', company.id]"
                      [attr.aria-label]="'Ver empresa ' + company.legalName"
                      >Ver empresa <app-icon name="arrow"
                    /></a>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="4" class="no-results">
                    No hay coincidencias entre las empresas cargadas.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <div class="table-footer">
          <span>{{ filtered().length }} de {{ companies().length }} empresas cargadas</span>
          @if (nextCursor()) {
            <button class="button secondary small" (click)="load()" [disabled]="loading()">
              {{ loading() ? 'Cargando…' : 'Cargar más' }}
            </button>
          }
        </div>
      }
    </section>
    <div class="workspace-note">
      <app-icon name="lock" /><span
        >La información de cada empresa se mantiene separada. Tus permisos determinan a qué puedes
        acceder.</span
      ><button class="text-button" (click)="load(true)" [disabled]="loading()">
        Actualizar listado
      </button>
    </div>`,
})
export class CompaniesPage {
  readonly auth = inject(AuthService);
  private readonly api = inject(CompaniesApi);
  private readonly destroy = inject(DestroyRef);
  readonly companies = signal<Company[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly search = signal('');
  readonly nextCursor = signal<string | null>(null);
  readonly filtered = computed(() => {
    const query = this.search().trim().toLocaleLowerCase('es-CO');
    return this.companies().filter(
      (c) =>
        c.legalName.toLocaleLowerCase('es-CO').includes(query) ||
        (c.nit + '-' + c.verificationDigit).includes(query),
    );
  });
  constructor() {
    this.load(true);
  }
  load(reset = false): void {
    if (this.loading()) return;
    if (reset) {
      this.nextCursor.set(null);
      this.companies.set([]);
    }
    this.loading.set(true);
    this.error.set('');
    this.api
      .list(this.nextCursor())
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (page) => {
          this.companies.update((items) => [...items, ...page.items]);
          this.nextCursor.set(page.nextCursor);
        },
        error: (err) => this.error.set(apiErrorMessage(err)),
      });
  }
}
