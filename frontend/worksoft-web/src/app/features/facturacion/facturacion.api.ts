import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RuntimeConfigService } from '../../core/runtime-config';
import { Page } from '../parties/parties.api';
export type TipoDocumento = 'FACTURA' | 'NOTA_DEBITO' | 'NOTA_CREDITO';
export type EstadoDocumento = 'BORRADOR' | 'CONFIRMADO' | 'ANULADO';
export type TratamientoIva = 'GRAVADO' | 'EXENTO' | 'EXCLUIDO';
export interface ValoresProducto {
  codigo: string;
  nombre: string;
  tipo: 'PRODUCTO' | 'SERVICIO';
  unidad: string;
  precio: string;
  tratamientoIva: TratamientoIva;
  tarifaIva: string;
  activo: boolean;
  version: number;
}
export interface ProductoVenta extends ValoresProducto {
  id: string;
  empresaId: string;
  actualizadoEn: string;
}
export interface ClienteVenta {
  id: string;
  tipoDocumento: string;
  documento: string;
  digitoVerificacion: string | null;
  nombre: string;
  direccion: string;
  correo: string;
}
export interface CuentaFacturacion {
  id: string;
  codigo: string;
  nombre: string;
}
export interface ConfiguracionFacturacion {
  prefijoFactura: string;
  prefijoCredito: string;
  prefijoDebito: string;
  contabilizar: boolean;
  cuentaCartera: string | null;
  cuentaIngreso: string | null;
  cuentaIva: string | null;
  cuentaRecaudo: string | null;
  condiciones: string;
  version: number;
}
export interface EntradaVenta {
  productoId: string;
  descripcion: string;
  cantidad: string;
  precio: string;
  descuento: string;
}
export interface LineaVenta {
  productoId: string;
  codigo: string;
  descripcion: string;
  unidad: string;
  cantidad: string;
  precio: string;
  porcentajeDescuento: string;
  tratamientoIva: TratamientoIva;
  tarifaIva: string;
  bruto: string;
  descuento: string;
  base: string;
  iva: string;
  total: string;
}
export interface TotalesVenta {
  bruto: string;
  descuento: string;
  base: string;
  iva: string;
  total: string;
}
export interface ValoresDocumento {
  clienteId: string;
  tipo: TipoDocumento;
  documentoOrigenId: string | null;
  fecha: string;
  vencimiento: string;
  observaciones: string;
  referencia: string;
  elaboradoPor: string;
  lineas: EntradaVenta[];
  version: number;
}
export interface DocumentoVenta {
  id: string;
  empresaId: string;
  clienteId: string;
  tipo: TipoDocumento;
  documentoOrigenId: string | null;
  fecha: string;
  vencimiento: string;
  observaciones: string;
  referencia: string;
  elaboradoPor: string;
  estado: EstadoDocumento;
  numero: string | null;
  lineas: LineaVenta[];
  totales: TotalesVenta;
  cliente: ClienteVenta;
  emisor: { nombre: string; nit: string; digitoVerificacion: string };
  configuracion: ConfiguracionFacturacion;
  comprobanteId: string | null;
  version: number;
  creadoEn: string;
  actualizadoEn: string;
}
export interface PagoVenta {
  id: string;
  fecha: string;
  importe: string;
  medio: string;
  referencia: string;
  elaboradoPor: string;
  anulado: boolean;
  fechaAnulacion: string | null;
  motivoAnulacion: string;
  anuladoPor: string;
  comprobanteId: string | null;
  comprobanteReversionId: string | null;
}
export interface DetalleVenta {
  documento: DocumentoVenta;
  pagos: PagoVenta[];
  recaudado: string;
  saldo: string;
}
export interface ResumenVenta {
  id: string;
  tipo: TipoDocumento;
  numero: string | null;
  fecha: string;
  vencimiento: string;
  cliente: string;
  estado: EstadoDocumento;
  total: string;
  recaudado: string;
  saldo: string;
  version: number;
}
export interface CarteraVenta {
  ventas: string;
  iva: string;
  recaudado: string;
  pendiente: string;
  vencido: string;
  documentosPendientes: number;
}
export interface ValoresPago {
  fecha: string;
  importe: string;
  medio: string;
  referencia: string;
  elaboradoPor: string;
  version: number;
}
export interface ReversionVenta {
  fecha: string;
  motivo: string;
  elaboradoPor: string;
  version: number;
}
@Injectable({ providedIn: 'root' })
export class FacturacionApi {
  private readonly http = inject(HttpClient);
  private readonly configuracion = inject(RuntimeConfigService);
  private base(empresa: string) {
    return (
      this.configuracion.config.apiBaseUrl +
      '/companies/' +
      encodeURIComponent(empresa) +
      '/facturacion'
    );
  }
  productos(empresa: string, q = '', page = 0, size = 25) {
    return this.http.get<Page<ProductoVenta>>(this.base(empresa) + '/productos', {
      params: { q, page, size },
    });
  }
  guardarProducto(empresa: string, id: string | null, valores: ValoresProducto) {
    return id
      ? this.http.put<ProductoVenta>(
          this.base(empresa) + '/productos/' + encodeURIComponent(id),
          valores,
        )
      : this.http.post<ProductoVenta>(this.base(empresa) + '/productos', valores);
  }
  clientes(empresa: string, q = '') {
    return this.http.get<ClienteVenta[]>(this.base(empresa) + '/clientes', { params: { q } });
  }
  cuentas(empresa: string, q = '') {
    return this.http.get<CuentaFacturacion[]>(this.base(empresa) + '/cuentas', { params: { q } });
  }
  configuracionEmpresa(empresa: string) {
    return this.http.get<ConfiguracionFacturacion>(this.base(empresa) + '/configuracion');
  }
  configurar(empresa: string, valores: ConfiguracionFacturacion) {
    return this.http.put<ConfiguracionFacturacion>(this.base(empresa) + '/configuracion', valores);
  }
  documentos(empresa: string, filtros: Record<string, string | number>) {
    return this.http.get<Page<ResumenVenta>>(this.base(empresa) + '/documentos', {
      params: filtros,
    });
  }
  consultar(empresa: string, id: string) {
    return this.http.get<DetalleVenta>(
      this.base(empresa) + '/documentos/' + encodeURIComponent(id),
    );
  }
  guardar(empresa: string, id: string | null, valores: ValoresDocumento, clave: string) {
    return id
      ? this.http.put<DetalleVenta>(
          this.base(empresa) + '/documentos/' + encodeURIComponent(id),
          valores,
        )
      : this.http.post<DetalleVenta>(this.base(empresa) + '/documentos', valores, {
          headers: { 'Idempotency-Key': clave },
        });
  }
  confirmar(empresa: string, id: string, version: number) {
    return this.http.post<DetalleVenta>(
      this.base(empresa) + '/documentos/' + encodeURIComponent(id) + '/confirmar',
      { version },
    );
  }
  acreditar(empresa: string, id: string, valores: ReversionVenta) {
    return this.http.post<DetalleVenta>(
      this.base(empresa) + '/documentos/' + encodeURIComponent(id) + '/nota-credito',
      valores,
    );
  }
  pagar(empresa: string, id: string, valores: ValoresPago, clave: string) {
    return this.http.post<DetalleVenta>(
      this.base(empresa) + '/documentos/' + encodeURIComponent(id) + '/pagos',
      valores,
      { headers: { 'Idempotency-Key': clave } },
    );
  }
  anularPago(empresa: string, id: string, pago: string, valores: ReversionVenta) {
    return this.http.post<DetalleVenta>(
      this.base(empresa) +
        '/documentos/' +
        encodeURIComponent(id) +
        '/pagos/' +
        encodeURIComponent(pago) +
        '/anular',
      valores,
    );
  }
  cartera(empresa: string, corte: string) {
    return this.http.get<CarteraVenta>(this.base(empresa) + '/cartera', { params: { corte } });
  }
}
