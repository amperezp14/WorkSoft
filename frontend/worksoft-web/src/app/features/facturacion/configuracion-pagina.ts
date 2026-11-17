import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { AccountsApi } from '../accounting/accounts.api';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { CompanyContext } from '../workspace/company-context';
import { FacturacionApi, ConfiguracionFacturacion, CuentaFacturacion } from './facturacion.api';
import { NavegacionFacturacion } from './navegacion-facturacion';
import { apiErrorMessage } from '../../core/api-error';
@Component({
  selector: 'app-configuracion-facturacion',
  imports: [FormsModule, NavegacionFacturacion],
  templateUrl: './configuracion-pagina.html',
})
export class ConfiguracionPagina {
  readonly contexto = inject(CompanyContext);
  private readonly api = inject(FacturacionApi);
  private readonly catalogo = inject(AccountsApi);
  private readonly destruir = inject(DestroyRef);
  readonly cuentas = signal<CuentaFacturacion[]>([]);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly ocupado = signal(false);
  readonly cargando = signal(true);
  valores: ConfiguracionFacturacion | null = null;
  busqueda = '';
  readonly campos = [
    { clave: 'cuentaCartera', nombre: 'Cartera / clientes' },
    { clave: 'cuentaIngreso', nombre: 'Ingresos por ventas' },
    { clave: 'cuentaIva', nombre: 'IVA generado' },
    { clave: 'cuentaRecaudo', nombre: 'Caja / bancos para recaudos' },
  ] as const;
  constructor() {
    effect(() => {
      if (this.contexto.company())
        untracked(() => {
          this.cargar();
          this.buscar();
        });
    });
  }
  cargar() {
    this.cargando.set(true);
    this.api
      .configuracionEmpresa(this.contexto.company()!.id)
      .pipe(
        takeUntilDestroyed(this.destruir),
        finalize(() => this.cargando.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.valores = r;
          for (const campo of this.campos) {
            const id = r[campo.clave];
            if (id)
              this.catalogo
                .get(this.contexto.company()!.id, id)
                .pipe(takeUntilDestroyed(this.destruir))
                .subscribe({
                  next: (cuenta) =>
                    this.cuentas.update((c) =>
                      c.some((v) => v.id === id)
                        ? c
                        : [{ id: cuenta.id, codigo: cuenta.code, nombre: cuenta.name }, ...c],
                    ),
                  error: (e) => this.error.set(apiErrorMessage(e)),
                });
          }
        },
        error: (e) => this.error.set(apiErrorMessage(e)),
      });
  }
  buscar() {
    this.api
      .cuentas(this.contexto.company()!.id, this.busqueda)
      .pipe(takeUntilDestroyed(this.destruir))
      .subscribe({
        next: (r) => {
          const anteriores = this.cuentas();
          const elegidas = this.valores ? this.campos.map((c) => this.valores![c.clave]) : [];
          this.cuentas.set([
            ...r,
            ...anteriores.filter((c) => elegidas.includes(c.id) && !r.some((n) => n.id === c.id)),
          ]);
        },
        error: (e) => this.error.set(apiErrorMessage(e)),
      });
  }
  tieneCuenta(id: string | null): boolean {
    return this.cuentas().some((c) => c.id === id);
  }
  guardar() {
    if (!this.valores || this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.api
      .configurar(this.contexto.company()!.id, this.valores)
      .pipe(
        takeUntilDestroyed(this.destruir),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.valores = r;
          this.mensaje.set(
            'Configuración guardada. Los documentos confirmados conservan su configuración histórica.',
          );
        },
        error: (e) => this.error.set(apiErrorMessage(e)),
      });
  }
}
