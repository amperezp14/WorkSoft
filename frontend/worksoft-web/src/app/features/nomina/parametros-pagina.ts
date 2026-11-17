import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CompanyContext } from '../workspace/company-context';
import { NominaApi, ParametrosNomina, ValoresParametros } from './nomina.api';
import { NavegacionNomina } from './navegacion-nomina';
import { apiErrorMessage } from '../../core/api-error';
import { normalizeMoney, formatAmount } from '../journals/money';
@Component({
  selector: 'app-parametros-nomina',
  imports: [FormsModule, NavegacionNomina],
  templateUrl: './parametros-pagina.html',
})
export class ParametrosPagina {
  readonly contexto = inject(CompanyContext);
  private readonly api = inject(NominaApi);
  private readonly destruir = inject(DestroyRef);
  readonly registros = signal<ParametrosNomina[]>([]);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly importe = formatAmount;
  editado: ParametrosNomina | null = null;
  valores = this.inicial();
  private inicial(): ValoresParametros {
    return {
      desde: '',
      hasta: '',
      salarioMinimo: '',
      auxilioTransporte: '',
      saludTrabajador: '',
      pensionTrabajador: '',
      fuente: '',
      revisado: false,
      responsableRevision: '',
      version: 0,
    };
  }
  constructor() {
    effect(() => {
      if (this.contexto.company())
        untracked(
          () =>
            void this.ejecutar(async () =>
              this.registros.set(
                await this.obtener(this.api.parametros(this.contexto.company()!.id)),
              ),
            ),
        );
    });
  }
  private obtener<T>(peticion: Observable<T>) {
    return firstValueFrom(peticion.pipe(takeUntilDestroyed(this.destruir)));
  }
  private async ejecutar(accion: () => Promise<void>) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    try {
      await accion();
    } catch (e) {
      this.error.set(apiErrorMessage(e));
    } finally {
      this.ocupado.set(false);
    }
  }
  nuevo() {
    if (this.ocupado()) return;
    this.editado = null;
    this.valores = this.inicial();
  }
  referencia() {
    if (this.ocupado()) return;
    this.nuevo();
    this.valores = {
      ...this.valores,
      desde: '2026-01-01',
      hasta: '2026-12-31',
      salarioMinimo: '1750905.00',
      auxilioTransporte: '249095.00',
      saludTrabajador: '4.00',
      pensionTrabajador: '4.00',
      fuente:
        'Referencia 2026: Decreto 0159/2026 (SMMLV transitorio), Decreto 1470/2025 (auxilio); Ley 100/1993 y Decreto 3771/2007. Verificar vigencia y régimen aplicable antes de revisar.',
    };
  }
  editar(p: ParametrosNomina) {
    if (this.ocupado()) return;
    this.editado = p;
    this.valores = { ...p };
  }
  guardar() {
    let valores: ValoresParametros;
    try {
      valores = {
        ...this.valores,
        salarioMinimo: normalizeMoney(this.valores.salarioMinimo),
        auxilioTransporte: normalizeMoney(this.valores.auxilioTransporte),
        saludTrabajador: normalizeMoney(this.valores.saludTrabajador),
        pensionTrabajador: normalizeMoney(this.valores.pensionTrabajador),
      };
    } catch {
      this.error.set('Revisa importes y porcentajes con hasta dos decimales.');
      return;
    }
    void this.ejecutar(async () => {
      const p = await this.obtener(
        this.api.guardarParametros(this.contexto.company()!.id, this.editado?.id ?? null, valores),
      );
      this.editado = p;
      this.valores = { ...p };
      this.registros.set(await this.obtener(this.api.parametros(this.contexto.company()!.id)));
      this.mensaje.set('Vigencia guardada. Las liquidaciones aprobadas conservan sus parámetros.');
    });
  }
}
