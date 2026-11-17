import { Component, DestroyRef, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CompanyContext } from '../workspace/company-context';
import {
  FacturacionApi,
  ClienteVenta,
  ProductoVenta,
  DetalleVenta,
  ResumenVenta,
  TipoDocumento,
  ValoresDocumento,
  EntradaVenta,
  PagoVenta,
} from './facturacion.api';
import { NavegacionFacturacion } from './navegacion-facturacion';
import { apiErrorMessage } from '../../core/api-error';
import {
  calcularLinea,
  decimalNormalizado,
  importeNormalizado,
  formatoCentavos,
  formatoImporte,
  fechaBogota,
} from './calculos-venta';
interface LineaFormulario extends EntradaVenta {
  codigo: string;
  tarifaIva: string;
  tratamientoIva: string;
}
@Component({
  selector: 'app-documentos-venta',
  imports: [FormsModule, RouterLink, NavegacionFacturacion],
  templateUrl: './documentos-pagina.html',
})
export class DocumentosPagina {
  readonly contexto = inject(CompanyContext);
  private readonly api = inject(FacturacionApi);
  private readonly ruta = inject(ActivatedRoute);
  private readonly destruir = inject(DestroyRef);
  readonly notas = !!this.ruta.snapshot.data['notas'];
  readonly ocupado = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly listado = signal<ResumenVenta[]>([]);
  readonly total = signal(0);
  readonly pagina = signal(0);
  readonly seleccionado = signal<DetalleVenta | null>(null);
  readonly clientes = signal<ClienteVenta[]>([]);
  readonly productos = signal<ProductoVenta[]>([]);
  readonly buscando = signal(false);
  readonly importe = formatoImporte;
  readonly centavos = formatoCentavos;
  readonly nombres: Record<TipoDocumento, string> = {
    FACTURA: 'Venta interna',
    NOTA_CREDITO: 'Nota crédito interna',
    NOTA_DEBITO: 'Nota débito interna',
  };
  filtros = {
    q: '',
    estado: '',
    tipo: this.notas ? 'NOTA_CREDITO' : 'FACTURA',
    desde: '',
    hasta: '',
  };
  private filtrosAplicados = { ...this.filtros };
  busquedaCliente = '';
  busquedaProducto = '';
  modificado = false;
  formulario = this.inicial();
  lineas: LineaFormulario[] = [this.linea()];
  private claves = new Map<string, string>();
  private secuenciaBusqueda = 0;
  formularioPago = {
    fecha: fechaBogota(),
    importe: '',
    medio: 'TRANSFERENCIA',
    referencia: '',
    elaboradoPor: '',
  };
  reversion = { fecha: fechaBogota(), motivo: '', elaboradoPor: '' };
  accionReversion: 'credito' | 'pago' | null = null;
  pagoReversion: PagoVenta | null = null;
  private inicial() {
    return {
      clienteId: '',
      tipo: 'FACTURA' as TipoDocumento,
      documentoOrigenId: null as string | null,
      fecha: fechaBogota(),
      vencimiento: fechaBogota(),
      observaciones: '',
      referencia: '',
      elaboradoPor: '',
    };
  }
  private linea(): LineaFormulario {
    return {
      productoId: '',
      descripcion: '',
      cantidad: '1',
      precio: '0.00',
      descuento: '0.00',
      codigo: '',
      tarifaIva: '0.00',
      tratamientoIva: 'EXCLUIDO',
    };
  }
  constructor() {
    effect(() => {
      if (this.contexto.company())
        untracked(() => {
          void this.ejecutar(async () => {
            this.nuevo(true);
            await this.cargarListado();
            await this.cargarOpciones();
            const id = this.ruta.snapshot.queryParamMap.get('documento');
            if (id) this.mostrar(await this.obtener(this.api.consultar(this.empresa, id)));
          });
        });
    });
  }
  get empresa() {
    return this.contexto.company()!.id;
  }
  get editable() {
    return !this.seleccionado() || this.seleccionado()!.documento.estado === 'BORRADOR';
  }
  get documento() {
    return this.seleccionado()?.documento ?? null;
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
      this.error.set(
        e instanceof Error && e.message.startsWith('FORMULARIO:')
          ? e.message.slice(11)
          : apiErrorMessage(e),
      );
    } finally {
      this.ocupado.set(false);
    }
  }
  canLeave() {
    return (
      !this.ocupado() &&
      (!this.modificado || window.confirm('Hay cambios sin guardar. ¿Deseas salir?'))
    );
  }
  nuevo(forzar = false) {
    if (!forzar && (this.ocupado() || !this.canLeave())) return;
    this.seleccionado.set(null);
    this.formulario = this.inicial();
    this.lineas = [this.linea()];
    this.modificado = false;
    this.claves.clear();
    this.accionReversion = null;
    this.mensaje.set('');
  }
  async buscar() {
    this.filtrosAplicados = { ...this.filtros };
    await this.ejecutar(() => this.cargarListado());
  }
  async cambiarPagina(pagina: number) {
    await this.ejecutar(() => this.cargarListado(pagina));
  }
  private async cargarListado(pagina = 0) {
    const parametros: Record<string, string | number> = {
      page: pagina,
      size: 25,
      ...this.filtrosAplicados,
    };
    if (!parametros['desde']) delete parametros['desde'];
    if (!parametros['hasta']) delete parametros['hasta'];
    const r = await this.obtener(this.api.documentos(this.empresa, parametros));
    this.listado.set(r.items);
    this.total.set(r.total);
    this.pagina.set(r.page);
  }
  async buscarOpciones() {
    this.error.set('');
    this.buscando.set(true);
    try {
      await this.cargarOpciones();
    } catch (e) {
      this.error.set(apiErrorMessage(e));
    } finally {
      this.buscando.set(false);
    }
  }
  private async cargarOpciones() {
    const secuencia = ++this.secuenciaBusqueda;
    const [clientes, productos] = await Promise.all([
      this.obtener(this.api.clientes(this.empresa, this.busquedaCliente)),
      this.obtener(this.api.productos(this.empresa, this.busquedaProducto, 0, 50)),
    ]);
    if (secuencia !== this.secuenciaBusqueda) return;
    const actual = this.documento;
    const elegido =
      this.clientes().find((cliente) => cliente.id === this.formulario.clienteId) ??
      (actual?.clienteId === this.formulario.clienteId ? actual.cliente : null);
    this.clientes.set([
      ...clientes,
      ...(elegido && !clientes.some((cliente) => cliente.id === elegido.id) ? [elegido] : []),
    ]);
    const preservados = this.productos().filter(
      (p) =>
        this.lineas.some((l) => l.productoId === p.id) &&
        !productos.items.some((n) => n.id === p.id),
    );
    this.productos.set([...productos.items.filter((p) => p.activo), ...preservados]);
  }
  cambiarProducto(indice: number) {
    const linea = this.lineas[indice],
      producto = this.productos().find((p) => p.id === linea.productoId);
    if (producto) {
      Object.assign(linea, {
        descripcion: producto.nombre,
        codigo: producto.codigo,
        precio: producto.precio,
        tarifaIva: producto.tarifaIva,
        tratamientoIva: producto.tratamientoIva,
      });
      this.modificado = true;
    }
  }
  agregarLinea() {
    if (this.lineas.length < 100) {
      this.lineas.push(this.linea());
      this.modificado = true;
    }
  }
  quitarLinea(indice: number) {
    if (this.lineas.length > 1) {
      this.lineas.splice(indice, 1);
      this.modificado = true;
    }
  }
  totales() {
    let bruto = 0n,
      descuento = 0n,
      base = 0n,
      iva = 0n,
      total = 0n;
    try {
      for (const l of this.lineas) {
        const calculo = calcularLinea(l.cantidad, l.precio, l.descuento, l.tarifaIva);
        bruto += calculo.bruto;
        descuento += calculo.descuento;
        base += calculo.base;
        iva += calculo.iva;
        total += calculo.total;
      }
      return { bruto, descuento, base, iva, total, valido: true };
    } catch {
      return { bruto, descuento, base, iva, total, valido: false };
    }
  }
  totalLinea(linea: LineaFormulario) {
    try {
      return formatoCentavos(
        calcularLinea(linea.cantidad, linea.precio, linea.descuento, linea.tarifaIva).total,
      );
    } catch {
      return 'Revisa importes';
    }
  }
  private mostrar(detalle: DetalleVenta) {
    this.seleccionado.set(detalle);
    const d = detalle.documento;
    this.formulario = {
      clienteId: d.clienteId,
      tipo: d.tipo,
      documentoOrigenId: d.documentoOrigenId,
      fecha: d.fecha,
      vencimiento: d.vencimiento,
      observaciones: d.observaciones,
      referencia: d.referencia,
      elaboradoPor: d.elaboradoPor,
    };
    this.lineas = d.lineas.map((l) => ({
      productoId: l.productoId,
      descripcion: l.descripcion,
      cantidad: l.cantidad,
      precio: l.precio,
      descuento: l.porcentajeDescuento,
      codigo: l.codigo,
      tarifaIva: l.tarifaIva,
      tratamientoIva: l.tratamientoIva,
    }));
    if (!this.clientes().some((c) => c.id === d.clienteId))
      this.clientes.update((c) => [d.cliente, ...c]);
    this.modificado = false;
    this.accionReversion = null;
    this.formularioPago = {
      fecha: fechaBogota(),
      importe: detalle.saldo,
      medio: 'TRANSFERENCIA',
      referencia: '',
      elaboradoPor: d.elaboradoPor,
    };
  }
  async abrir(id: string) {
    if (!this.canLeave()) return;
    await this.ejecutar(async () =>
      this.mostrar(await this.obtener(this.api.consultar(this.empresa, id))),
    );
  }
  async guardar() {
    await this.ejecutar(async () => {
      let valores: ValoresDocumento;
      try {
        valores = {
          ...this.formulario,
          version: this.documento?.version ?? 0,
          lineas: this.lineas.map((l) => ({
            productoId: l.productoId,
            descripcion: l.descripcion,
            cantidad: decimalNormalizado(l.cantidad, 3),
            precio: importeNormalizado(l.precio),
            descuento: importeNormalizado(l.descuento),
          })),
        };
        if (
          !this.totales().valido ||
          !valores.clienteId ||
          valores.lineas.some((l) => !l.productoId)
        )
          throw new Error();
      } catch {
        throw new Error(
          'FORMULARIO:Revisa cliente, conceptos e importes. Cantidad hasta tres decimales; precios y porcentajes hasta dos.',
        );
      }
      const contenido = JSON.stringify(valores);
      if (!this.claves.has(contenido)) this.claves.set(contenido, crypto.randomUUID());
      const detalle = await this.obtener(
        this.api.guardar(
          this.empresa,
          this.documento?.id ?? null,
          valores,
          this.claves.get(contenido)!,
        ),
      );
      this.mostrar(detalle);
      this.mensaje.set(
        'Borrador guardado. Se recalcularon los impuestos con el catálogo vigente al guardar.',
      );
      await this.cargarListado(this.pagina());
    });
  }
  async confirmar() {
    const documento = this.documento;
    if (
      !documento ||
      this.modificado ||
      !window.confirm(
        '¿Confirmar este documento interno? Sus importes quedarán protegidos contra edición.',
      )
    )
      return;
    await this.ejecutar(async () => {
      const detalle = await this.obtener(
        this.api.confirmar(this.empresa, documento.id, documento.version),
      );
      this.mostrar(detalle);
      this.mensaje.set('Documento confirmado: ' + detalle.documento.numero);
      await this.cargarListado(this.pagina());
    });
  }
  nuevaNotaDebito() {
    const original = this.documento;
    if (
      !original ||
      original.tipo !== 'FACTURA' ||
      original.estado !== 'CONFIRMADO' ||
      this.ocupado()
    )
      return;
    this.nuevo(true);
    this.formulario = {
      ...this.formulario,
      tipo: 'NOTA_DEBITO',
      documentoOrigenId: original.id,
      clienteId: original.clienteId,
      elaboradoPor: original.elaboradoPor,
      referencia: original.numero ?? '',
    };
    if (!this.clientes().some((c) => c.id === original.clienteId))
      this.clientes.update((c) => [original.cliente, ...c]);
    this.modificado = true;
  }
  prepararCredito() {
    if (!this.documento) return;
    this.accionReversion = 'credito';
    this.pagoReversion = null;
    this.reversion = {
      fecha: fechaBogota(),
      motivo: '',
      elaboradoPor: this.documento.elaboradoPor,
    };
  }
  prepararAnulacion(pago: PagoVenta) {
    this.accionReversion = 'pago';
    this.pagoReversion = pago;
    this.reversion = {
      fecha: fechaBogota(),
      motivo: '',
      elaboradoPor: this.documento?.elaboradoPor ?? '',
    };
  }
  async revertir() {
    const documento = this.documento;
    if (
      !documento ||
      !this.accionReversion ||
      !window.confirm(
        this.accionReversion === 'credito'
          ? 'Se creará una nota crédito por el total y se anulará el documento. ¿Continuar?'
          : 'Se anulará el recaudo y se restablecerá el saldo. ¿Continuar?',
      )
    )
      return;
    const accion = this.accionReversion,
      pago = this.pagoReversion;
    await this.ejecutar(async () => {
      const valores = { ...this.reversion, version: documento.version };
      const detalle = await this.obtener(
        accion === 'credito'
          ? this.api.acreditar(this.empresa, documento.id, valores)
          : this.api.anularPago(this.empresa, documento.id, pago!.id, valores),
      );
      this.mostrar(detalle);
      this.mensaje.set(
        accion === 'credito'
          ? 'Nota crédito creada; documento original anulado.'
          : 'Recaudo anulado.',
      );
      await this.cargarListado(this.pagina());
    });
  }
  async pagar() {
    const documento = this.documento;
    if (!documento) return;
    await this.ejecutar(async () => {
      let importe: string;
      try {
        importe = importeNormalizado(this.formularioPago.importe);
      } catch {
        throw new Error('FORMULARIO:Revisa el importe del pago.');
      }
      const valores = { ...this.formularioPago, importe, version: documento.version };
      const contenido = 'pago:' + documento.id + JSON.stringify(valores);
      if (!this.claves.has(contenido)) this.claves.set(contenido, crypto.randomUUID());
      this.mostrar(
        await this.obtener(
          this.api.pagar(this.empresa, documento.id, valores, this.claves.get(contenido)!),
        ),
      );
      this.claves.delete(contenido);
      this.mensaje.set('Recaudo registrado.');
      await this.cargarListado(this.pagina());
    });
  }
  imprimir() {
    if (this.modificado) {
      this.error.set('Guarda el borrador antes de imprimir.');
      return;
    }
    window.print();
  }
}
