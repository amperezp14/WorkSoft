import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CompanyContext } from '../workspace/company-context';
import {
  NominaApi,
  ContratoNomina,
  DetalleNomina,
  ResumenNomina,
  TipoNovedad,
  NovedadNomina,
} from './nomina.api';
import { NavegacionNomina } from './navegacion-nomina';
import { apiErrorMessage } from '../../core/api-error';
import { normalizeMoney, formatAmount, bogotaToday } from '../journals/money';
@Component({
  selector: 'app-periodos-nomina',
  imports: [FormsModule, RouterLink, NavegacionNomina],
  templateUrl: './periodos-pagina.html',
})
export class PeriodosPagina {
  readonly contexto = inject(CompanyContext);
  private readonly api = inject(NominaApi);
  private readonly destruir = inject(DestroyRef);
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly registros = signal<ResumenNomina[]>([]);
  readonly total = signal(0);
  readonly pagina = signal(0);
  readonly detalle = signal<DetalleNomina | null>(null);
  readonly contratos = signal<ContratoNomina[]>([]);
  readonly totalContratos = signal(0);
  readonly paginaContratos = signal(0);
  readonly importe = formatAmount;
  readonly tipos: { valor: TipoNovedad; nombre: string }[] = [
    { valor: 'BONIFICACION_SALARIAL', nombre: 'Bonificación salarial' },
    { valor: 'COMISION_SALARIAL', nombre: 'Comisión salarial' },
    { valor: 'DEDUCCION_AUTORIZADA', nombre: 'Deducción autorizada' },
    { valor: 'RETENCION_FUENTE', nombre: 'Retención previamente calculada' },
  ];
  busqueda = '';
  busquedaContrato = '';
  private consulta = '';
  private consultaContratos = '';
  seleccionados = new Map<string, { contrato: ContratoNomina; diasAuxilio: number }>();
  formulario = this.inicial();
  novedad = this.novedadInicial();
  pago = { fecha: bogotaToday(), referencia: '' };
  motivoAnulacion = '';
  empleadoImpresion = '';
  modificado = false;
  private inicial() {
    const [anio, mes] = bogotaToday().split('-');
    return {
      id: crypto.randomUUID(),
      anio: Number(anio),
      mes: Number(mes),
      descripcion: 'Nómina mensual',
      elaboradoPor: '',
    };
  }
  private novedadInicial() {
    return {
      id: crypto.randomUUID(),
      empleadoId: '',
      tipo: 'BONIFICACION_SALARIAL' as TipoNovedad,
      descripcion: '',
      importe: '',
      soporte: '',
    };
  }
  constructor() {
    effect(() => {
      if (this.contexto.company())
        untracked(
          () =>
            void this.ejecutar(async () => {
              await this.listar();
              await this.listarContratos();
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
  get editable() {
    return (
      this.detalle()?.periodo.estado === 'BORRADOR' ||
      this.detalle()?.periodo.estado === 'CALCULADA'
    );
  }
  get seleccion() {
    return [...this.seleccionados.values()];
  }
  canLeave() {
    return (
      !this.ocupado() &&
      (!this.modificado || window.confirm('Hay datos del formulario sin guardar. ¿Deseas salir?'))
    );
  }
  nuevo() {
    if (!this.canLeave()) return;
    this.detalle.set(null);
    this.formulario = this.inicial();
    this.novedad = this.novedadInicial();
    this.seleccionados.clear();
    this.modificado = false;
    this.empleadoImpresion = '';
    this.motivoAnulacion = '';
  }
  private async listar(pagina = 0) {
    const r = await this.obtener(this.api.periodos(this.empresa, this.consulta, pagina));
    this.registros.set(r.items);
    this.total.set(r.total);
    this.pagina.set(r.page);
  }
  buscar() {
    this.consulta = this.busqueda;
    void this.ejecutar(() => this.listar());
  }
  cambiarPagina(pagina: number) {
    void this.ejecutar(() => this.listar(pagina));
  }
  private async listarContratos(pagina = 0) {
    const r = await this.obtener(this.api.contratos(this.empresa, this.consultaContratos, pagina));
    this.contratos.set(r.items);
    this.totalContratos.set(r.total);
    this.paginaContratos.set(r.page);
  }
  buscarContratos() {
    this.consultaContratos = this.busquedaContrato;
    void this.ejecutar(() => this.listarContratos());
  }
  cambiarPaginaContratos(pagina: number) {
    void this.ejecutar(() => this.listarContratos(pagina));
  }
  seleccionar(contrato: ContratoNomina, marcado: boolean) {
    if (marcado)
      this.seleccionados.set(contrato.id, {
        contrato,
        diasAuxilio: contrato.auxilioTransporte ? 30 : 0,
      });
    else this.seleccionados.delete(contrato.id);
    this.modificado = true;
  }
  cambiarDias(id: string, valor: string) {
    const seleccionado = this.seleccionados.get(id);
    if (seleccionado) seleccionado.diasAuxilio = Number(valor);
    this.modificado = true;
  }
  private mostrar(detalle: DetalleNomina) {
    this.detalle.set(detalle);
    this.modificado = false;
    this.novedad = this.novedadInicial();
    this.pago = { fecha: bogotaToday(), referencia: '' };
    this.motivoAnulacion = '';
    this.empleadoImpresion = '';
  }
  crear() {
    if (!this.seleccionados.size) {
      this.error.set('Selecciona al menos un contrato.');
      return;
    }
    void this.ejecutar(async () => {
      const valores = {
        ...this.formulario,
        seleccion: [...this.seleccionados.entries()].map(([contratoId, seleccionado]) => ({
          contratoId,
          diasAuxilio: seleccionado.diasAuxilio,
        })),
      };
      this.mostrar(await this.obtener(this.api.crear(this.empresa, valores)));
      this.mensaje.set('Periodo guardado. Agrega novedades y calcula antes de aprobar.');
      await this.listar();
    });
  }
  abrir(id: string) {
    if (!this.canLeave()) return;
    void this.ejecutar(async () =>
      this.mostrar(await this.obtener(this.api.consultar(this.empresa, id))),
    );
  }
  nombreEmpleado(id: string) {
    return (
      this.detalle()?.contratosSeleccionados.find((c) => c.empleadoId === id)?.empleadoNombre ??
      'Empleado'
    );
  }
  nombreTipo(tipo: TipoNovedad) {
    return this.tipos.find((t) => t.valor === tipo)?.nombre ?? tipo;
  }
  agregarNovedad() {
    const periodo = this.detalle()?.periodo;
    if (!periodo) return;
    let importe: string;
    try {
      importe = normalizeMoney(this.novedad.importe);
    } catch {
      this.error.set('Revisa el importe de la novedad.');
      return;
    }
    void this.ejecutar(async () => {
      this.mostrar(
        await this.obtener(
          this.api.novedad(this.empresa, periodo.id, { ...this.novedad, importe }, periodo.version),
        ),
      );
      this.mensaje.set('Novedad guardada. Se debe recalcular el periodo.');
      await this.listar(this.pagina());
    });
  }
  quitarNovedad(novedad: NovedadNomina) {
    const periodo = this.detalle()?.periodo;
    if (!periodo || !window.confirm('¿Quitar esta novedad del borrador?')) return;
    void this.ejecutar(async () => {
      this.mostrar(
        await this.obtener(
          this.api.eliminarNovedad(this.empresa, periodo.id, novedad.id, periodo.version),
        ),
      );
      await this.listar(this.pagina());
    });
  }
  calcular() {
    const periodo = this.detalle()?.periodo;
    if (!periodo || this.modificado) return;
    void this.ejecutar(async () => {
      this.mostrar(
        await this.obtener(this.api.calcular(this.empresa, periodo.id, periodo.version)),
      );
      this.mensaje.set('Nómina calculada. Revisa cada desprendible antes de aprobar.');
      await this.listar(this.pagina());
    });
  }
  aprobar() {
    const periodo = this.detalle()?.periodo;
    if (
      !periodo ||
      this.modificado ||
      !window.confirm(
        '¿Aprobar la nómina? Los cálculos y datos históricos quedarán protegidos contra edición.',
      )
    )
      return;
    void this.ejecutar(async () => {
      this.mostrar(await this.obtener(this.api.aprobar(this.empresa, periodo.id, periodo.version)));
      this.mensaje.set(
        'Nómina aprobada. No se han transferido fondos ni enviado documentos a DIAN o PILA.',
      );
      await this.listar(this.pagina());
    });
  }
  anular() {
    const periodo = this.detalle()?.periodo;
    if (
      !periodo ||
      !this.motivoAnulacion.trim() ||
      !window.confirm(
        '¿Anular este periodo? Se conservará el historial y podrás crear otro para el mismo mes.',
      )
    )
      return;
    void this.ejecutar(async () => {
      this.mostrar(
        await this.obtener(
          this.api.anular(this.empresa, periodo.id, periodo.version, this.motivoAnulacion),
        ),
      );
      this.mensaje.set('Periodo anulado; historial conservado.');
      await this.listar(this.pagina());
    });
  }
  registrarPago() {
    const periodo = this.detalle()?.periodo;
    if (
      !periodo ||
      !window.confirm(
        'Se registrará que pagaste la nómina por un medio externo. Esta acción no transfiere dinero y cierra el periodo. ¿Continuar?',
      )
    )
      return;
    void this.ejecutar(async () => {
      this.mostrar(
        await this.obtener(
          this.api.pago(
            this.empresa,
            periodo.id,
            periodo.version,
            this.pago.fecha,
            this.pago.referencia,
          ),
        ),
      );
      this.mensaje.set('Pago externo registrado.');
      await this.listar(this.pagina());
    });
  }
  imprimir(empleado = '') {
    this.empleadoImpresion = empleado;
    setTimeout(() => window.print(), 0);
  }
}
