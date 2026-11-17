import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RuntimeConfigService } from '../../core/runtime-config';
import { Page } from '../parties/parties.api';
export interface EmpleadoNomina {
  id: string;
  empresaId: string;
  terceroId: string;
  codigo: string;
  nombre: string;
  documento: string;
  cargo: string;
  activo: boolean;
  version: number;
  actualizadoEn: string;
}
export interface ValoresEmpleado {
  terceroId: string;
  codigo: string;
  cargo: string;
  activo: boolean;
  version: number;
}
export interface ValoresContrato {
  empleadoId: string;
  tipo: 'INDEFINIDO' | 'FIJO';
  inicio: string;
  fin: string | null;
  salarioMensual: string;
  auxilioTransporte: boolean;
  version: number;
}
export interface ContratoNomina extends ValoresContrato {
  id: string;
  empresaId: string;
  empleadoNombre: string;
  actualizadoEn: string;
}
export interface ValoresParametros {
  desde: string;
  hasta: string;
  salarioMinimo: string;
  auxilioTransporte: string;
  saludTrabajador: string;
  pensionTrabajador: string;
  fuente: string;
  revisado: boolean;
  responsableRevision: string;
  version: number;
}
export interface ParametrosNomina extends ValoresParametros {
  id: string;
  empresaId: string;
  estrategia: string;
  actualizadoEn: string;
}
export type TipoNovedad =
  'BONIFICACION_SALARIAL' | 'COMISION_SALARIAL' | 'DEDUCCION_AUTORIZADA' | 'RETENCION_FUENTE';
export interface NovedadNomina {
  id: string;
  empleadoId: string;
  tipo: TipoNovedad;
  descripcion: string;
  importe: string;
  soporte: string;
}
export interface SeleccionNomina {
  contratoId: string;
  diasAuxilio: number;
}
export interface ValoresPeriodo {
  id: string;
  anio: number;
  mes: number;
  descripcion: string;
  elaboradoPor: string;
  seleccion: SeleccionNomina[];
}
export type EstadoPeriodo = 'BORRADOR' | 'CALCULADA' | 'APROBADA' | 'PAGADA' | 'ANULADA';
export interface LiquidacionNomina {
  empleadoId: string;
  codigo: string;
  nombre: string;
  tipoDocumento: string;
  documento: string;
  cargo: string;
  contrato: ContratoNomina;
  diasAuxilio: number;
  novedades: NovedadNomina[];
  salario: string;
  adicionalSalarial: string;
  auxilio: string;
  devengado: string;
  ibc: string;
  salud: string;
  pension: string;
  tasaSolidaridad: string;
  solidaridad: string;
  otrasDeducciones: string;
  retencion: string;
  deducciones: string;
  neto: string;
}
export interface PeriodoNomina extends ValoresPeriodo {
  empresaId: string;
  estado: EstadoPeriodo;
  novedades: NovedadNomina[];
  liquidaciones: LiquidacionNomina[];
  parametros: ParametrosNomina | null;
  emisor: { nombre: string; nit: string; digitoVerificacion: string };
  motivoAnulacion: string;
  fechaPago: string | null;
  referenciaPago: string;
  version: number;
  creadoEn: string;
  actualizadoEn: string;
}
export interface TotalesNomina {
  devengado: string;
  deducciones: string;
  neto: string;
  empleados: number;
}
export interface DetalleNomina {
  periodo: PeriodoNomina;
  totales: TotalesNomina;
  contratosSeleccionados: ContratoNomina[];
}
export interface ResumenNomina {
  id: string;
  anio: number;
  mes: number;
  descripcion: string;
  estado: EstadoPeriodo;
  totales: TotalesNomina;
  version: number;
}
@Injectable({ providedIn: 'root' })
export class NominaApi {
  private readonly http = inject(HttpClient);
  private readonly configuracion = inject(RuntimeConfigService);
  private base(empresa: string) {
    return (
      this.configuracion.config.apiBaseUrl + '/companies/' + encodeURIComponent(empresa) + '/nomina'
    );
  }
  empleados(empresa: string, q = '', page = 0, size = 25) {
    return this.http.get<Page<EmpleadoNomina>>(this.base(empresa) + '/empleados', {
      params: { q, page, size },
    });
  }
  guardarEmpleado(empresa: string, id: string | null, valores: ValoresEmpleado) {
    return id
      ? this.http.put<EmpleadoNomina>(
          this.base(empresa) + '/empleados/' + encodeURIComponent(id),
          valores,
        )
      : this.http.post<EmpleadoNomina>(this.base(empresa) + '/empleados', valores);
  }
  contratos(empresa: string, q = '', page = 0, size = 25) {
    return this.http.get<Page<ContratoNomina>>(this.base(empresa) + '/contratos', {
      params: { q, page, size },
    });
  }
  guardarContrato(empresa: string, id: string | null, valores: ValoresContrato) {
    return id
      ? this.http.put<ContratoNomina>(
          this.base(empresa) + '/contratos/' + encodeURIComponent(id),
          valores,
        )
      : this.http.post<ContratoNomina>(this.base(empresa) + '/contratos', valores);
  }
  parametros(empresa: string) {
    return this.http.get<ParametrosNomina[]>(this.base(empresa) + '/parametros');
  }
  guardarParametros(empresa: string, id: string | null, valores: ValoresParametros) {
    return id
      ? this.http.put<ParametrosNomina>(
          this.base(empresa) + '/parametros/' + encodeURIComponent(id),
          valores,
        )
      : this.http.post<ParametrosNomina>(this.base(empresa) + '/parametros', valores);
  }
  periodos(empresa: string, q = '', page = 0) {
    return this.http.get<Page<ResumenNomina>>(this.base(empresa) + '/periodos', {
      params: { q, page, size: 25 },
    });
  }
  crear(empresa: string, valores: ValoresPeriodo) {
    return this.http.post<DetalleNomina>(this.base(empresa) + '/periodos', valores);
  }
  consultar(empresa: string, id: string) {
    return this.http.get<DetalleNomina>(this.base(empresa) + '/periodos/' + encodeURIComponent(id));
  }
  novedad(empresa: string, id: string, valores: NovedadNomina, version: number) {
    return this.http.post<DetalleNomina>(
      this.base(empresa) + '/periodos/' + encodeURIComponent(id) + '/novedades',
      { ...valores, version },
    );
  }
  eliminarNovedad(empresa: string, id: string, novedad: string, version: number) {
    return this.http.delete<DetalleNomina>(
      this.base(empresa) +
        '/periodos/' +
        encodeURIComponent(id) +
        '/novedades/' +
        encodeURIComponent(novedad),
      { params: { version } },
    );
  }
  calcular(empresa: string, id: string, version: number) {
    return this.http.post<DetalleNomina>(
      this.base(empresa) + '/periodos/' + encodeURIComponent(id) + '/calcular',
      { version },
    );
  }
  aprobar(empresa: string, id: string, version: number) {
    return this.http.post<DetalleNomina>(
      this.base(empresa) + '/periodos/' + encodeURIComponent(id) + '/aprobar',
      { version },
    );
  }
  anular(empresa: string, id: string, version: number, motivo: string) {
    return this.http.post<DetalleNomina>(
      this.base(empresa) + '/periodos/' + encodeURIComponent(id) + '/anular',
      { version, motivo },
    );
  }
  pago(empresa: string, id: string, version: number, fecha: string, referencia: string) {
    return this.http.post<DetalleNomina>(
      this.base(empresa) + '/periodos/' + encodeURIComponent(id) + '/pago',
      { version, fecha, referencia },
    );
  }
}
