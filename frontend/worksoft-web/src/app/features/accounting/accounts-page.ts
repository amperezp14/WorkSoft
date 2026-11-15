import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { CompanyContext } from '../workspace/company-context';
import { Account, AccountsApi, Nature } from './accounts.api';
import { Icon } from '../../shared/icon';
import { apiErrorMessage } from '../../core/api-error';
@Component({
  selector: 'app-accounts-page',
  imports: [ReactiveFormsModule, Icon],
  template: ` <div class="page-heading">
      <div>
        <span class="eyebrow">CONTABILIDAD</span>
        <h2>Plan de cuentas</h2>
        <p>Consulta y configura las cuentas de {{ context.company()?.legalName }}.</p>
      </div>
      <button class="button primary" (click)="newAccount()">
        <app-icon name="plus" /> Nueva cuenta
      </button>
    </div>
    <div class="reference-note">
      <strong>Referencia para comerciantes · 2026</strong>
      <p>
        Catálogo inicial parcial basado en el Decreto 2650 de 1993. Adáptalo al marco contable y a
        las necesidades de tu empresa.
      </p>
      <a
        href="https://gestornormativo.creg.gov.co/gestor/entorno/docs/decreto_2650_1993.htm"
        target="_blank"
        rel="noopener noreferrer"
        >Consultar fuente normativa ↗</a
      >
    </div>
    @if (success()) {
      <div class="alert success" role="status">{{ success() }}</div>
    }
    @if (error()) {
      <div class="alert error" role="alert">{{ error() }}</div>
    }
    <div class="accounts-layout">
      <section class="panel" aria-labelledby="accounts-table-title">
        <div class="panel-toolbar">
          <div>
            <h3 id="accounts-table-title">Cuentas de la empresa</h3>
            <p>{{ total() }} {{ total() === 1 ? 'cuenta encontrada' : 'cuentas encontradas' }}</p>
          </div>
          <button class="text-button" (click)="refresh()" [disabled]="loading() || saving()">
            Actualizar tabla
          </button>
        </div>
        <form class="account-search" (submit)="$event.preventDefault(); search()">
          <label for="account-query" class="sr-only">Buscar cuentas</label
          ><input
            id="account-query"
            placeholder="Buscar por código o nombre"
            [value]="query()"
            (input)="query.set($any($event.target).value)"
            maxlength="100"
          /><button class="button secondary small" type="submit" [disabled]="loading()">
            Buscar
          </button>
        </form>
        @if (loading()) {
          <div class="empty-state" role="status">
            <span class="spinner"></span>
            <p>Consultando cuentas…</p>
          </div>
        } @else {
          <div class="table-scroll accounts-table">
            <table>
              <thead>
                <tr>
                  <th>Código / cuenta</th>
                  <th>Nivel</th>
                  <th>Naturaleza</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (account of accounts(); track account.id) {
                  <tr>
                    <td>
                      <strong class="account-code">{{ account.code }}</strong
                      ><span class="account-name">{{ account.name }}</span>
                    </td>
                    <td>{{ level(account.code) }}</td>
                    <td>{{ account.nature === 'DEBIT' ? 'Débito' : 'Crédito' }}</td>
                    <td>
                      <span class="status" [class.inactive]="!account.active">{{
                        account.active ? 'Activa' : 'Inactiva'
                      }}</span>
                    </td>
                    <td>
                      <button
                        class="text-button"
                        [attr.aria-label]="'Editar cuenta ' + account.code"
                        (click)="edit(account)"
                        [disabled]="saving()"
                      >
                        Editar
                      </button>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="5" class="no-results">
                      {{
                        error()
                          ? 'No se pudieron cargar las cuentas.'
                          : 'No hay cuentas que coincidan con la búsqueda.'
                      }}
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <div class="table-footer">
            <span>Página {{ page() + 1 }} · {{ total() }} cuentas</span>
            <div class="pagination-actions">
              <button
                class="button secondary small"
                (click)="load(page() - 1)"
                [disabled]="page() === 0 || saving()"
              >
                Anterior</button
              ><button
                class="button secondary small"
                (click)="load(page() + 1)"
                [disabled]="(page() + 1) * 25 >= total() || saving()"
              >
                Siguiente
              </button>
            </div>
          </div>
        }
      </section>
      <section class="panel account-form" aria-labelledby="account-form-title">
        <h3 id="account-form-title">{{ editing() ? 'Editar cuenta' : 'Nueva cuenta' }}</h3>
        <p class="field-help">Cambios exclusivos de esta empresa.</p>
        <form [formGroup]="form" (ngSubmit)="save()" novalidate>
          <div class="field">
            <label for="account-code">Código *</label
            ><input
              id="account-code"
              formControlName="code"
              inputmode="numeric"
              maxlength="10"
              placeholder="Ej. 11050501"
            />
            <p class="field-help">
              {{
                editing()
                  ? 'El código no cambia al editar.'
                  : '1, 2, 4, 6, 8 o 10 dígitos. Debe existir la cuenta superior.'
              }}
            </p>
            @if (parent()) {
              <p class="field-help">Cuenta superior: {{ parent() }}</p>
            }
          </div>
          <div class="field">
            <label for="account-name">Nombre *</label
            ><input id="account-name" formControlName="name" maxlength="200" />
          </div>
          <div class="field">
            <label for="account-nature">Naturaleza *</label
            ><select id="account-nature" formControlName="nature">
              <option value="DEBIT">Débito</option>
              <option value="CREDIT">Crédito</option>
            </select>
          </div>
          <label class="checkbox-field"
            ><input type="checkbox" formControlName="active" /> Cuenta activa</label
          >
          <label class="checkbox-field"
            ><input type="checkbox" formControlName="allowsPosting" /> Permite movimientos</label
          >
          <p class="field-help">
            Solo subcuentas y auxiliares sin cuentas hijas. El catálogo inicial es de clasificación.
          </p>
          <div class="field">
            <label for="account-notes">Observaciones</label
            ><textarea
              id="account-notes"
              formControlName="notes"
              maxlength="1000"
              rows="3"
            ></textarea>
          </div>
          @if (formError()) {
            <p class="field-error" role="alert">{{ formError() }}</p>
          }
          <div class="form-actions">
            <button
              class="button secondary"
              type="button"
              (click)="newAccount()"
              [disabled]="saving()"
            >
              Cancelar</button
            ><button class="button primary" type="submit" [disabled]="saving()">
              {{ saving() ? 'Guardando…' : editing() ? 'Guardar cambios' : 'Guardar cuenta' }}
            </button>
          </div>
        </form>
      </section>
    </div>`,
})
export class AccountsPage {
  readonly context = inject(CompanyContext);
  private readonly api = inject(AccountsApi);
  private readonly destroy = inject(DestroyRef);
  readonly accounts = signal<Account[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly query = signal('');
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly formError = signal('');
  readonly editing = signal<Account | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    code: [
      '',
      [Validators.required, Validators.pattern(/^[1-9][0-9]*$/), Validators.maxLength(10)],
    ],
    name: ['', [Validators.required, Validators.maxLength(200), Validators.pattern(/\S/)]],
    nature: ['DEBIT' as Nature, Validators.required],
    active: [true],
    allowsPosting: [true],
    notes: ['', Validators.maxLength(1000)],
  });
  private readonly code = toSignal(this.form.controls.code.valueChanges, { initialValue: '' });
  readonly parent = computed(() =>
    this.code().length < 2 ? '' : this.code().slice(0, this.code().length === 2 ? 1 : -2),
  );
  private searched = '';
  constructor() {
    effect(() => {
      const company = this.context.company();
      if (company)
        untracked(() => {
          this.newAccount();
          this.searched = '';
          this.query.set('');
          this.load(0);
        });
    });
  }
  level(code: string): string {
    return (
      ({ 1: 'Clase', 2: 'Grupo', 4: 'Cuenta', 6: 'Subcuenta' } as Record<number, string>)[
        code.length
      ] ?? 'Auxiliar'
    );
  }
  load(page: number): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.api
      .list(this.context.company()!.id, this.searched, page)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (result) => {
          this.accounts.set(result.items);
          this.total.set(result.total);
          this.page.set(result.page);
        },
        error: (err) => this.error.set(apiErrorMessage(err)),
      });
  }
  search(): void {
    this.searched = this.query().trim();
    this.load(0);
  }
  refresh(): void {
    this.newAccount();
    this.load(this.page());
  }
  newAccount(): void {
    if (this.saving()) return;
    this.editing.set(null);
    this.form.controls.code.enable();
    this.form.reset({
      code: '',
      name: '',
      nature: 'DEBIT',
      active: true,
      allowsPosting: true,
      notes: '',
    });
    this.formError.set('');
  }
  edit(account: Account): void {
    this.editing.set(account);
    this.form.reset({
      code: account.code,
      name: account.name,
      nature: account.nature,
      active: account.active,
      allowsPosting: account.allowsPosting,
      notes: account.notes,
    });
    this.form.controls.code.disable();
    this.formError.set('');
    this.error.set('');
    this.success.set('');
  }
  save(): void {
    if (this.saving()) return;
    this.formError.set('');
    this.error.set('');
    this.success.set('');
    const raw = this.form.getRawValue();
    if (this.form.invalid || ![1, 2, 4, 6, 8, 10].includes(raw.code.length)) {
      this.form.markAllAsTouched();
      this.formError.set(
        'Revisa el código y el nombre. El código debe tener 1, 2, 4, 6, 8 o 10 dígitos.',
      );
      return;
    }
    if (raw.allowsPosting && raw.code.length < 6) {
      this.formError.set('Solo las subcuentas y auxiliares pueden permitir movimientos.');
      return;
    }
    const current = this.editing();
    const values = {
      ...raw,
      name: raw.name.trim(),
      notes: raw.notes.trim(),
      version: current?.version ?? 0,
    };
    const request = current
      ? this.api.update(this.context.company()!.id, current.id, values)
      : this.api.create(this.context.company()!.id, values);
    this.saving.set(true);
    request
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.newAccount();
          this.success.set(
            current
              ? 'Los cambios se guardaron en PostgreSQL.'
              : 'La cuenta se guardó en PostgreSQL.',
          );
          this.load(this.page());
        },
        error: (err) => this.error.set(apiErrorMessage(err)),
      });
  }
}
