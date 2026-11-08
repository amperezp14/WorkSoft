import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { Notifications } from '../../core/notifications';
import { Icon } from '../../shared/icon';
import { CompaniesApi, IdempotencyKeys, normalizeCompany } from './companies.api';
@Component({
  selector: 'app-create-company-page',
  imports: [ReactiveFormsModule, RouterLink, Icon],
  template: ` <a class="back-link" routerLink="/empresas">← Volver a mis empresas</a>
    <div class="page-heading">
      <div>
        <span class="eyebrow">UN NUEVO COMIENZO</span>
        <h1>Registrar empresa</h1>
        <p>Crea un espacio de gestión para una empresa colombiana.</p>
      </div>
    </div>
    <div class="form-layout">
      <section class="panel form-panel" aria-labelledby="form-title">
        <div class="form-intro">
          <span class="summary-icon"><app-icon /></span>
          <div>
            <h2 id="form-title">Información de la empresa</h2>
            <p>Los campos marcados con * son obligatorios.</p>
          </div>
        </div>
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          @if (error()) {
            <div class="alert error" role="alert">{{ error() }}</div>
          }
          <div class="field">
            <label for="legalName">Razón social *</label
            ><input
              id="legalName"
              formControlName="legalName"
              maxlength="200"
              autocomplete="organization"
              placeholder="Nombre legal de la empresa"
              [attr.aria-invalid]="invalid('legalName')"
              aria-describedby="legalName-help legalName-error"
            />
            <p id="legalName-help" class="field-help">Escribe el nombre legal completo.</p>
            @if (invalid('legalName')) {
              <p id="legalName-error" class="field-error">
                Ingresa una razón social de hasta 200 caracteres.
              </p>
            }
          </div>
          <div class="identification-grid">
            <div class="field">
              <label for="nit">NIT *</label
              ><input
                id="nit"
                formControlName="nit"
                inputmode="numeric"
                maxlength="15"
                placeholder="Sin puntos ni guiones"
                [attr.aria-invalid]="invalid('nit')"
                aria-describedby="nit-help nit-error"
              />
              <p id="nit-help" class="field-help">
                Solo dígitos, sin incluir el dígito de verificación.
              </p>
              @if (invalid('nit')) {
                <p id="nit-error" class="field-error">
                  Ingresa entre 1 y 15 dígitos, sin separadores.
                </p>
              }
            </div>
            <div class="field">
              <label for="verificationDigit">Dígito de verificación *</label
              ><input
                id="verificationDigit"
                formControlName="verificationDigit"
                inputmode="numeric"
                maxlength="1"
                placeholder="0"
                [attr.aria-invalid]="invalid('verificationDigit')"
                aria-describedby="dv-help dv-error"
              />
              <p id="dv-help" class="field-help">Un solo dígito.</p>
              @if (invalid('verificationDigit')) {
                <p id="dv-error" class="field-error">Ingresa un dígito del 0 al 9.</p>
              }
            </div>
          </div>
          <div class="form-actions">
            <a class="button secondary" routerLink="/empresas">Cancelar</a
            ><button class="button primary" type="submit" [disabled]="submitting()">
              @if (submitting()) {
                <span class="spinner small-spinner"></span>
              } @else {
                <app-icon name="plus" />
              }
              {{ submitting() ? 'Registrando…' : 'Registrar empresa' }}
            </button>
          </div>
        </form>
      </section>
      <aside class="context-panel">
        <span class="eyebrow">CONFIGURACIÓN INICIAL</span>
        <h2>Pensada para Colombia</h2>
        <dl class="context-list">
          <div>
            <dt>País</dt>
            <dd>Colombia</dd>
          </div>
          <div>
            <dt>Moneda</dt>
            <dd>Peso colombiano · COP</dd>
          </div>
          <div>
            <dt>Zona horaria</dt>
            <dd>America/Bogota</dd>
          </div>
        </dl>
        <hr />
        <h3>Tu acceso inicial</h3>
        <p>Al registrar la empresa, recibirás acceso de administrador dentro de ella.</p>
        <p class="muted">
          El registro en WorkSoft no inscribe ni habilita a la empresa ante la DIAN. Verifica los
          datos con su documentación tributaria.
        </p>
      </aside>
    </div>`,
})
export class CreateCompanyPage {
  private readonly api = inject(CompaniesApi);
  private readonly router = inject(Router);
  private readonly notices = inject(Notifications);
  private readonly destroy = inject(DestroyRef);
  private readonly keys = new IdempotencyKeys();
  readonly submitting = signal(false);
  readonly error = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    legalName: ['', [Validators.required, Validators.maxLength(200), Validators.pattern(/\S/)]],
    nit: ['', [Validators.required, Validators.pattern(/^[0-9]{1,15}$/)]],
    verificationDigit: ['', [Validators.required, Validators.pattern(/^[0-9]$/)]],
  });
  invalid(field: 'legalName' | 'nit' | 'verificationDigit'): boolean {
    const control = this.form.controls[field];
    return control.invalid && control.touched;
  }
  submit(): void {
    if (this.submitting()) return;
    this.error.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const request = normalizeCompany(this.form.getRawValue());
    this.submitting.set(true);
    this.api
      .create(request, this.keys.for(request))
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: (company) => {
          this.notices.success('La empresa se registró correctamente.');
          void this.router.navigate(['/empresas', company.id]);
        },
        error: (err) => this.error.set(apiErrorMessage(err)),
      });
  }
}
