import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CompanyContext } from '../workspace/company-context';
import { PartiesApi, ThirdParty } from '../parties/parties.api';
import {
  NominaApi,
  EmpleadoNomina,
  ContratoNomina,
  ValoresEmpleado,
  ValoresContrato,
} from './nomina.api';
import { NavegacionNomina } from './navegacion-nomina';
import { apiErrorMessage } from '../../core/api-error';
import { normalizeMoney, formatAmount, bogotaToday } from '../journals/money';
@Component({
  selector: 'app-personal-nomina',
  imports: [FormsModule, RouterLink, NavegacionNomina],
  templateUrl: './personal-pagina.html',
})
export class PersonalPagina {
  readonly contexto = inject(CompanyContext);
  private readonly api = inject(NominaApi);
  private readonly tercerosApi = inject(PartiesApi);
  private readonly destruir = inject(DestroyRef);
  readonly esContratos = !!inject(ActivatedRoute).snapshot.data['contratos'];
  readonly empleados = signal<EmpleadoNomina[]>([]);
  readonly contratos = signal<ContratoNomina[]>([]);
  readonly opciones = signal<EmpleadoNomina[]>([]);
  readonly terceros = signal<ThirdParty[]>([]);
  readonly total = signal(0);
  readonly pagina = signal(0);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly importe = formatAmount;
  empleadoEditado: EmpleadoNomina | null = null;
  contratoEditado: ContratoNomina | null = null;
  busqueda = '';
  busquedaOpcion = '';
  private consulta = '';
  empleado = this.empleadoInicial();
  contrato = this.contratoInicial();
  private empleadoInicial(): ValoresEmpleado {
    return { terceroId: '', codigo: '', cargo: '', activo: true, version: 0 };
  }
  private contratoInicial() {
    return {
      empleadoId: '',
      tipo: 'INDEFINIDO' as ValoresContrato['tipo'],
      inicio: bogotaToday(),
      fin: '',
      salarioMensual: '',
      auxilioTransporte: false,
      version: 0,
    };
  }
  constructor() {
    effect(() => {
      if (this.contexto.company())
        untracked(
          () =>
            void this.ejecutar(async () => {
              await this.listar();
              await this.opcionesActuales();
            }),
        );
    });
  }
  private get empresa() {
    return this.contexto.company()!.id;
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
    this.empleadoEditado = null;
    this.contratoEditado = null;
    this.empleado = this.empleadoInicial();
    this.contrato = this.contratoInicial();
    this.error.set('');
  }
  editarEmpleado(e: EmpleadoNomina) {
    if (this.ocupado()) return;
    this.empleadoEditado = e;
    this.empleado = { ...e };
  }
  editarContrato(c: ContratoNomina) {
    if (this.ocupado()) return;
    this.contratoEditado = c;
    this.contrato = { ...c, fin: c.fin ?? '' };
    if (!this.opciones().some((e) => e.id === c.empleadoId))
      this.opciones.update((e) => [
        {
          id: c.empleadoId,
          nombre: c.empleadoNombre,
          codigo: '',
          terceroId: '',
          empresaId: this.empresa,
          documento: '',
          cargo: '',
          activo: true,
          version: 0,
          actualizadoEn: '',
        },
        ...e,
      ]);
  }
  private async listar(pagina = 0) {
    if (this.esContratos) {
      const r = await this.obtener(this.api.contratos(this.empresa, this.consulta, pagina));
      this.contratos.set(r.items);
      this.total.set(r.total);
      this.pagina.set(r.page);
    } else {
      const r = await this.obtener(this.api.empleados(this.empresa, this.consulta, pagina));
      this.empleados.set(r.items);
      this.total.set(r.total);
      this.pagina.set(r.page);
    }
  }
  buscar() {
    this.consulta = this.busqueda;
    void this.ejecutar(() => this.listar());
  }
  cambiarPagina(pagina: number) {
    void this.ejecutar(() => this.listar(pagina));
  }
  private async opcionesActuales() {
    if (this.esContratos) {
      const r = await this.obtener(this.api.empleados(this.empresa, this.busquedaOpcion, 0, 100));
      const elegido = this.opciones().find((e) => e.id === this.contrato.empleadoId);
      this.opciones.set([
        ...r.items.filter((e) => e.activo),
        ...(elegido && !r.items.some((e) => e.id === elegido.id) ? [elegido] : []),
      ]);
    } else {
      const r = await this.obtener(this.tercerosApi.list(this.empresa, this.busquedaOpcion, 0));
      const elegido = this.terceros().find((t) => t.id === this.empleado.terceroId);
      this.terceros.set([
        ...r.items.filter((t) => t.active && t.documentType !== 'NIT'),
        ...(elegido && !r.items.some((t) => t.id === elegido.id) ? [elegido] : []),
      ]);
    }
  }
  buscarOpciones() {
    void this.ejecutar(() => this.opcionesActuales());
  }
  guardarEmpleado() {
    void this.ejecutar(async () => {
      const e = await this.obtener(
        this.api.guardarEmpleado(this.empresa, this.empleadoEditado?.id ?? null, this.empleado),
      );
      this.empleadoEditado = e;
      this.empleado = { ...e };
      this.mensaje.set('Empleado guardado en PostgreSQL.');
      await this.listar(this.pagina());
    });
  }
  guardarContrato() {
    let salario: string;
    try {
      salario = normalizeMoney(this.contrato.salarioMensual);
    } catch {
      this.error.set('Revisa el salario: máximo dos decimales sin separadores de miles.');
      return;
    }
    void this.ejecutar(async () => {
      const c = await this.obtener(
        this.api.guardarContrato(this.empresa, this.contratoEditado?.id ?? null, {
          ...this.contrato,
          salarioMensual: salario,
          fin: this.contrato.fin || null,
        }),
      );
      this.contratoEditado = c;
      this.contrato = { ...c, fin: c.fin ?? '' };
      this.mensaje.set('Contrato guardado. Las nóminas aprobadas conservan sus datos históricos.');
      await this.listar(this.pagina());
    });
  }
}
