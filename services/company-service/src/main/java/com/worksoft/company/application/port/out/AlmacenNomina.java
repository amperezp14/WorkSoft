package com.worksoft.company.application.port.out;

import com.worksoft.company.application.port.in.*;
import com.worksoft.company.domain.nomina.*;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Supplier;

public interface AlmacenNomina {
  <T> T transaccion(UUID empresa, Supplier<T> operacion);

  Page<EmpleadoNomina> empleados(UUID empresa, SearchPage pagina);

  EmpleadoNomina empleado(UUID empresa, UUID id);

  void guardarEmpleado(EmpleadoNomina empleado, boolean nuevo);

  Page<ContratoNomina> contratos(UUID empresa, SearchPage pagina);

  ContratoNomina contrato(UUID empresa, UUID id);

  void guardarContrato(ContratoNomina contrato, boolean nuevo);

  List<ParametrosNomina> parametros(UUID empresa);

  ParametrosNomina parametros(UUID empresa, UUID id);

  ParametrosNomina parametrosAplicables(UUID empresa, YearMonth mes);

  void guardarParametros(ParametrosNomina parametros, boolean nuevo);

  Page<CasosUsoNomina.Resumen> periodos(UUID empresa, SearchPage pagina);

  PeriodoNomina periodo(UUID empresa, UUID id);

  Optional<PeriodoNomina> periodoOpcional(UUID empresa, UUID id);

  void guardarPeriodo(PeriodoNomina periodo, boolean nuevo);

  PeriodoNomina.Emisor emisor(UUID empresa);

  void auditar(Actor actor, UUID empresa, UUID entidad, String operacion);
}
