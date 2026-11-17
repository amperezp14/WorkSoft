import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';
import { CompanyContext } from '../workspace/company-context';
import {
  JournalsApi,
  JournalDocument,
  JournalHeader,
  JournalValues,
  JournalSummary,
  AccountOption,
  PartyOption,
} from './journals.api';
import { normalizeMoney, cents, formatCents, formatAmount, bogotaToday } from './money';
import { apiErrorMessage } from '../../core/api-error';
@Component({
  selector: 'app-journals-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './journals-page.html',
})
export class JournalsPage {
  readonly context = inject(CompanyContext);
  private readonly api = inject(JournalsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy = inject(DestroyRef);
  private readonly fb = inject(FormBuilder).nonNullable;
  readonly items = signal<JournalSummary[]>([]);
  readonly page = signal(0);
  readonly total = signal(0);
  readonly query = signal('');
  private searched = '';
  readonly opening = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly optionsLoading = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly formError = signal('');
  readonly selected = signal<JournalDocument | null>(null);
  readonly accounts = signal<AccountOption[]>([]);
  readonly parties = signal<PartyOption[]>([]);
  readonly accountQuery = signal('');
  readonly partyQuery = signal('');
  readonly moreOptions = signal(false);
  readonly form = this.fb.group({
    date: [bogotaToday(), Validators.required],
    type: ['GENERAL' as JournalHeader['type'], Validators.required],
    origin: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.required, Validators.maxLength(1000)]],
    supportReference: ['', [Validators.required, Validators.maxLength(500)]],
    preparedBy: ['', [Validators.required, Validators.maxLength(100)]],
    lines: this.fb.array([this.line(), this.line()]),
  });
  private readonly changes = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });
  readonly totals = computed(() => {
    this.changes();
    let debit = 0n,
      credit = 0n,
      valid = true;
    for (const row of this.lines.getRawValue())
      try {
        const d = cents(normalizeMoney(row.debit)),
          c = cents(normalizeMoney(row.credit));
        debit += d;
        credit += c;
        if (d > 0n === c > 0n) valid = false;
      } catch {
        valid = false;
      }
    return {
      debit,
      credit,
      difference: debit - credit,
      balanced: valid && this.lines.length >= 2 && debit > 0n && debit === credit,
    };
  });
  readonly amount = formatAmount;
  readonly money = formatCents;
  private readonly requestKeys = new Map<string, string>();
  get lines() {
    return this.form.controls.lines;
  }
  constructor() {
    effect(() => {
      if (this.context.company())
        untracked(() => {
          this.newVoucher(true);
          this.searched = '';
          this.query.set('');
          this.loadList(0);
          this.loadOptions();
          const id = this.route.snapshot.queryParamMap.get('comprobante');
          if (id) this.open(id);
        });
    });
  }
  private line(
    v?: Partial<{
      accountId: string;
      partyId: string;
      description: string;
      debit: string;
      credit: string;
    }>,
  ) {
    return this.fb.group({
      accountId: [v?.accountId ?? '', Validators.required],
      partyId: [v?.partyId ?? '', Validators.required],
      description: [v?.description ?? '', Validators.maxLength(300)],
      debit: [v?.debit ?? '0.00'],
      credit: [v?.credit ?? '0.00'],
    });
  }
  canLeave(): boolean {
    return (
      !this.form.dirty ||
      this.selected()?.status === 'POSTED' ||
      window.confirm('Hay cambios sin guardar en el comprobante. ¿Quieres descartarlos?')
    );
  }
  newVoucher(force = false): void {
    if (this.saving() || this.opening() || (!force && !this.canLeave())) return;
    this.selected.set(null);
    this.requestKeys.clear();
    this.form.enable();
    this.lines.clear();
    this.lines.push(this.line());
    this.lines.push(this.line());
    this.form.patchValue({
      date: bogotaToday(),
      type: 'GENERAL',
      origin: '',
      description: '',
      supportReference: '',
      preparedBy: '',
    });
    this.form.markAsPristine();
    this.error.set('');
    this.formError.set('');
    this.success.set('');
  }
  addLine(): void {
    if (this.lines.length < 200) {
      this.lines.push(this.line());
      this.form.markAsDirty();
    }
  }
  removeLine(index: number): void {
    if (this.lines.length > 1) {
      this.lines.removeAt(index);
      this.form.markAsDirty();
    }
  }
  loadList(page = 0): void {
    if (this.loading()) return;
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
    this.loadList(0);
  }
  loadOptions(): void {
    if (this.optionsLoading()) return;
    this.optionsLoading.set(true);
    this.api
      .options(this.context.company()!.id, this.accountQuery().trim(), this.partyQuery().trim())
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.optionsLoading.set(false)),
      )
      .subscribe({
        next: (r) => {
          const rows = this.lines.getRawValue();
          const a = new Map(
            this.accounts()
              .filter((a) => rows.some((l) => l.accountId === a.id))
              .map((a) => [a.id, a]),
          );
          for (const v of r.accounts) a.set(v.id, v);
          this.accounts.set([...a.values()]);
          const p = new Map(
            this.parties()
              .filter((p) => rows.some((l) => l.partyId === p.id))
              .map((p) => [p.id, p]),
          );
          for (const v of r.parties) p.set(v.id, v);
          this.parties.set([...p.values()]);
          this.moreOptions.set(r.moreAccounts || r.moreParties);
        },
        error: (e) => this.error.set(apiErrorMessage(e)),
      });
  }
  open(id: string): void {
    if (this.saving() || this.opening() || !this.canLeave()) return;
    this.error.set('');
    this.opening.set(true);
    this.form.disable();
    this.api
      .get(this.context.company()!.id, id)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => {
          this.opening.set(false);
          if (this.selected()?.status !== 'POSTED') this.form.enable();
        }),
      )
      .subscribe({
        next: (doc) => {
          this.apply(doc);
          this.success.set('');
        },
        error: (error) => this.error.set(apiErrorMessage(error)),
      });
  }
  private apply(d: JournalDocument): void {
    this.form.enable();
    this.selected.set(d);
    this.lines.clear();
    for (const row of d.lines) this.lines.push(this.line(row));
    this.form.patchValue(d.header);
    const accounts = new Map(this.accounts().map((a) => [a.id, a]));
    const parties = new Map(this.parties().map((p) => [p.id, p]));
    for (const row of d.lines) {
      accounts.set(row.accountId, {
        id: row.accountId,
        code: row.accountCode,
        name: row.accountName,
      });
      parties.set(row.partyId, {
        id: row.partyId,
        documentType: row.partyDocumentType,
        documentNumber: row.partyDocumentNumber,
        verificationDigit: row.partyVerificationDigit,
        name: row.partyName,
      });
    }
    this.accounts.set([...accounts.values()]);
    this.parties.set([...parties.values()]);
    this.form.markAsPristine();
    this.formError.set('');
    if (d.status === 'POSTED') this.form.disable();
  }
  save(): void {
    if (this.saving() || this.opening() || this.selected()?.status === 'POSTED') return;
    this.formError.set('');
    this.error.set('');
    this.success.set('');
    const raw = this.form.getRawValue();
    if (
      this.form.invalid ||
      !raw.origin.trim() ||
      !raw.description.trim() ||
      !raw.supportReference.trim() ||
      !raw.preparedBy.trim()
    ) {
      this.form.markAllAsTouched();
      this.formError.set(
        'Completa la fecha, origen, descripción, soporte, elaborado por, cuentas y terceros.',
      );
      return;
    }
    let values: JournalValues;
    try {
      values = {
        header: {
          date: raw.date,
          type: raw.type,
          origin: raw.origin.trim(),
          description: raw.description.trim(),
          supportReference: raw.supportReference.trim(),
          preparedBy: raw.preparedBy.trim(),
        },
        lines: raw.lines.map((row) => {
          const debit = normalizeMoney(row.debit),
            credit = normalizeMoney(row.credit);
          if (cents(debit) > 0n === cents(credit) > 0n) throw new Error();
          return { ...row, description: row.description.trim(), debit, credit };
        }),
        version: this.selected()?.version ?? 0,
      };
    } catch {
      this.formError.set(
        'Cada línea necesita un débito o crédito positivo, nunca ambos. Usa máximo dos decimales y sin separadores de miles.',
      );
      return;
    }
    const current = this.selected();
    const fingerprint = JSON.stringify(values);
    if (!this.requestKeys.has(fingerprint)) this.requestKeys.set(fingerprint, crypto.randomUUID());
    const request = current
      ? this.api.update(this.context.company()!.id, current.id, values)
      : this.api.create(this.context.company()!.id, values, this.requestKeys.get(fingerprint)!);
    this.saving.set(true);
    request
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (d) => {
          this.apply(d);
          this.success.set('Borrador guardado en PostgreSQL.');
          this.loadList(this.page());
        },
        error: (e) => this.error.set(apiErrorMessage(e)),
      });
  }
  post(): void {
    const doc = this.selected();
    if (!doc || doc.status !== 'DRAFT' || this.saving() || this.opening()) return;
    if (this.form.dirty) {
      this.formError.set('Guarda los cambios antes de contabilizar.');
      return;
    }
    if (!this.totals().balanced) {
      this.formError.set(
        'El comprobante debe tener al menos dos líneas y débitos y créditos iguales.',
      );
      return;
    }
    this.error.set('');
    this.success.set('');
    this.saving.set(true);
    this.api
      .post(this.context.company()!.id, doc.id, doc.version)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (d) => {
          this.apply(d);
          this.success.set('Comprobante contabilizado: ' + d.displayNumber);
          this.loadList(this.page());
        },
        error: (e) => this.error.set(apiErrorMessage(e)),
      });
  }
  print(): void {
    window.print();
  }
}
