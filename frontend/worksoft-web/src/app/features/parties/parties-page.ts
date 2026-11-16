import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { RouterLink } from '@angular/router';
import { CompanyContext } from '../workspace/company-context';
import { PartiesApi, ThirdParty, DocumentType, PartyCategory } from './parties.api';
import { apiErrorMessage } from '../../core/api-error';
@Component({
  selector: 'app-parties-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './parties-page.html',
})
export class PartiesPage {
  readonly context = inject(CompanyContext);
  private readonly api = inject(PartiesApi);
  private readonly destroy = inject(DestroyRef);
  readonly items = signal<ThirdParty[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly query = signal('');
  private searched = '';
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly formError = signal('');
  readonly editing = signal<ThirdParty | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    documentType: ['NIT' as DocumentType, Validators.required],
    documentNumber: ['', [Validators.required, Validators.maxLength(30)]],
    verificationDigit: [''],
    name: ['', [Validators.required, Validators.maxLength(200), Validators.pattern(/\S/)]],
    category: ['OTHER' as PartyCategory, Validators.required],
    email: ['', [Validators.email, Validators.maxLength(200)]],
    phone: ['', Validators.maxLength(30)],
    address: ['', Validators.maxLength(250)],
    active: [true],
  });
  readonly documentType = toSignal(this.form.controls.documentType.valueChanges, {
    initialValue: 'NIT' as DocumentType,
  });
  constructor() {
    effect(() => {
      if (this.context.company())
        untracked(() => {
          this.newParty();
          this.query.set('');
          this.searched = '';
          this.load(0);
        });
    });
  }
  load(page = 0): void {
    if (this.loading()) return;
    this.error.set('');
    this.loading.set(true);
    this.api
      .list(this.context.company()!.id, this.searched, page)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.items.set(r.items);
          this.total.set(r.total);
          this.page.set(r.page);
        },
        error: (e) => this.error.set(apiErrorMessage(e)),
      });
  }
  search(): void {
    this.searched = this.query().trim();
    this.load(0);
  }
  newParty(): void {
    if (this.saving()) return;
    this.editing.set(null);
    this.form.reset({
      documentType: 'NIT',
      documentNumber: '',
      verificationDigit: '',
      name: '',
      category: 'OTHER',
      email: '',
      phone: '',
      address: '',
      active: true,
    });
    this.formError.set('');
  }
  edit(p: ThirdParty): void {
    this.editing.set(p);
    this.form.reset({ ...p, verificationDigit: p.verificationDigit ?? '' });
    this.error.set('');
    this.formError.set('');
    this.success.set('');
  }
  save(): void {
    if (this.saving()) return;
    this.error.set('');
    this.success.set('');
    this.formError.set('');
    const v = this.form.getRawValue();
    const number = v.documentNumber.trim().toUpperCase();
    if (
      this.form.invalid ||
      (v.documentType === 'NIT'
        ? !/^[0-9]{1,15}$/.test(number) || !/^[0-9]$/.test(v.verificationDigit)
        : !/^[A-Z0-9-]{1,30}$/.test(number))
    ) {
      this.form.markAllAsTouched();
      this.formError.set(
        'Revisa el nombre, la identificación, el dígito de verificación del NIT y el correo.',
      );
      return;
    }
    const current = this.editing();
    const values = {
      ...v,
      name: v.name.trim(),
      documentNumber: number,
      verificationDigit: v.documentType === 'NIT' ? v.verificationDigit : null,
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
          this.newParty();
          this.success.set('El tercero se guardó en PostgreSQL.');
          this.load(this.page());
        },
        error: (e) => this.error.set(apiErrorMessage(e)),
      });
  }
}
