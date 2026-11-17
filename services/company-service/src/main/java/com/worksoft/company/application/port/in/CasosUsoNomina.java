package com.worksoft.company.application.port.in;

import com.worksoft.company.domain.nomina.*;
import java.time.LocalDate;
import java.util.*;

public interface CasosUsoNomina {
  record ValoresEmpleado(
      UUID terceroId, String codigo, String cargo, boolean activo, long version) {}

  record ValoresContrato(
      UUID empleadoId,
      ContratoNomina.Tipo tipo,
      LocalDate inicio,
      LocalDate fin,
      String salarioMensual,
      boolean auxilioTransporte,
      long version) {}

  record ValoresParametros(
      LocalDate desde,
      LocalDate hasta,
      String salarioMinimo,
      String auxilioTransporte,
      String saludTrabajador,
      String pensionTrabajador,
      String fuente,
      boolean revisado,
      String responsableRevision,
      long version) {}

  record ValoresPeriodo(
      UUID id,
      int anio,
      int mes,
      String descripcion,
      String elaboradoPor,
      List<PeriodoNomina.Seleccion> seleccion) {}

  record ValoresNovedad(
      UUID id,
      UUID empleadoId,
      NovedadNomina.Tipo tipo,
      String descripcion,
      String importe,
      String soporte,
      long version) {}

  record Detalle(
      PeriodoNomina periodo,
      PeriodoNomina.Totales totales,
      List<ContratoNomina> contratosSeleccionados) {}

  record Resumen(
      UUID id,
      int anio,
      int mes,
      String descripcion,
      PeriodoNomina.Estado estado,
      PeriodoNomina.Totales totales,
      long version) {}

  Page<EmpleadoNomina> empleados(Actor actor, UUID empresa, SearchPage pagina);

  EmpleadoNomina guardarEmpleado(Actor actor, UUID empresa, UUID id, ValoresEmpleado valores);

  Page<ContratoNomina> contratos(Actor actor, UUID empresa, SearchPage pagina);

  ContratoNomina guardarContrato(Actor actor, UUID empresa, UUID id, ValoresContrato valores);

  List<ParametrosNomina> parametros(Actor actor, UUID empresa);

  ParametrosNomina guardarParametros(Actor actor, UUID empresa, UUID id, ValoresParametros valores);

  Page<Resumen> periodos(Actor actor, UUID empresa, SearchPage pagina);

  Detalle crear(Actor actor, UUID empresa, ValoresPeriodo valores);

  Detalle consultar(Actor actor, UUID empresa, UUID id);

  Detalle guardarNovedad(Actor actor, UUID empresa, UUID periodo, ValoresNovedad valores);

  Detalle eliminarNovedad(Actor actor, UUID empresa, UUID periodo, UUID novedad, long version);

  Detalle calcular(Actor actor, UUID empresa, UUID periodo, long version);

  Detalle aprobar(Actor actor, UUID empresa, UUID periodo, long version);

  Detalle anular(Actor actor, UUID empresa, UUID periodo, long version, String motivo);

  Detalle registrarPago(
      Actor actor, UUID empresa, UUID periodo, long version, LocalDate fecha, String referencia);
}
