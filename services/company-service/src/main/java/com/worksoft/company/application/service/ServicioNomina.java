package com.worksoft.company.application.service;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;
import static com.worksoft.company.domain.nomina.ReglasNomina.*;

import com.worksoft.company.application.port.in.*;
import com.worksoft.company.application.port.out.*;
import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.nomina.*;
import com.worksoft.company.domain.parties.ThirdParty;
import java.time.*;
import java.util.*;

public final class ServicioNomina implements CasosUsoNomina {
  private final CompanyAccess acceso;
  private final AlmacenNomina almacen;
  private final ThirdPartyStore terceros;
  private final Clock reloj;
  private final MotorNominaOrdinaria motor;

  public ServicioNomina(
      CompanyAccess acceso,
      AlmacenNomina almacen,
      ThirdPartyStore terceros,
      Clock reloj,
      MotorNominaOrdinaria motor) {
    this.acceso = acceso;
    this.almacen = almacen;
    this.terceros = terceros;
    this.reloj = reloj;
    this.motor = motor;
  }

  public Page<EmpleadoNomina> empleados(Actor actor, UUID empresa, SearchPage pagina) {
    acceso.require(actor, empresa);
    return almacen.empleados(empresa, pagina);
  }

  public EmpleadoNomina guardarEmpleado(
      Actor actor, UUID empresa, UUID id, ValoresEmpleado valores) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          exigir(valores != null && valores.terceroId() != null);
          var tercero = terceros.get(empresa, valores.terceroId());
          if (!tercero.active() || tercero.documentType() == ThirdParty.DocumentType.NIT)
            throw new BusinessException(NOMINA_REFERENCIA_INVALIDA);
          long siguiente = 0;
          if (id != null) {
            var anterior = almacen.empleado(empresa, id);
            version(anterior.version(), valores.version());
            if (!anterior.terceroId().equals(valores.terceroId()))
              throw new BusinessException(NOMINA_REFERENCIA_INVALIDA);
            siguiente = anterior.version() + 1;
          }
          var empleado =
              new EmpleadoNomina(
                  id == null ? UUID.randomUUID() : id,
                  empresa,
                  tercero.id(),
                  valores.codigo(),
                  tercero.name(),
                  tercero.documentNumber(),
                  valores.cargo(),
                  valores.activo(),
                  siguiente,
                  reloj.instant());
          almacen.guardarEmpleado(empleado, id == null);
          almacen.auditar(actor, empresa, empleado.id(), "GUARDAR_EMPLEADO");
          return empleado;
        });
  }

  public Page<ContratoNomina> contratos(Actor actor, UUID empresa, SearchPage pagina) {
    acceso.require(actor, empresa);
    return almacen.contratos(empresa, pagina);
  }

  public ContratoNomina guardarContrato(
      Actor actor, UUID empresa, UUID id, ValoresContrato valores) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          exigir(valores != null && valores.empleadoId() != null);
          var empleado = almacen.empleado(empresa, valores.empleadoId());
          if (!empleado.activo()) throw new BusinessException(NOMINA_REFERENCIA_INVALIDA);
          long siguiente = 0;
          if (id != null) {
            var anterior = almacen.contrato(empresa, id);
            version(anterior.version(), valores.version());
            if (!anterior.empleadoId().equals(valores.empleadoId()))
              throw new BusinessException(NOMINA_REFERENCIA_INVALIDA);
            siguiente = anterior.version() + 1;
          }
          var contrato =
              new ContratoNomina(
                  id == null ? UUID.randomUUID() : id,
                  empresa,
                  empleado.id(),
                  empleado.nombre(),
                  valores.tipo(),
                  valores.inicio(),
                  valores.fin(),
                  valores.salarioMensual(),
                  valores.auxilioTransporte(),
                  siguiente,
                  reloj.instant());
          almacen.guardarContrato(contrato, id == null);
          almacen.auditar(actor, empresa, contrato.id(), "GUARDAR_CONTRATO");
          return contrato;
        });
  }

  public List<ParametrosNomina> parametros(Actor actor, UUID empresa) {
    acceso.require(actor, empresa);
    return almacen.parametros(empresa);
  }

  public ParametrosNomina guardarParametros(
      Actor actor, UUID empresa, UUID id, ValoresParametros valores) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          exigir(valores != null);
          long siguiente = 0;
          if (id != null) {
            var anterior = almacen.parametros(empresa, id);
            version(anterior.version(), valores.version());
            siguiente = anterior.version() + 1;
          }
          var parametros =
              new ParametrosNomina(
                  id == null ? UUID.randomUUID() : id,
                  empresa,
                  valores.desde(),
                  valores.hasta(),
                  valores.salarioMinimo(),
                  valores.auxilioTransporte(),
                  valores.saludTrabajador(),
                  valores.pensionTrabajador(),
                  "ORDINARIA_LEY100_V1",
                  valores.fuente(),
                  valores.revisado(),
                  valores.responsableRevision(),
                  siguiente,
                  reloj.instant());
          almacen.guardarParametros(parametros, id == null);
          almacen.auditar(actor, empresa, parametros.id(), "GUARDAR_PARAMETROS_NOMINA");
          return parametros;
        });
  }

  public Page<Resumen> periodos(Actor actor, UUID empresa, SearchPage pagina) {
    acceso.require(actor, empresa);
    return almacen.periodos(empresa, pagina);
  }

  private Detalle detalle(PeriodoNomina periodo) {
    return new Detalle(
        periodo,
        periodo.totales(),
        periodo.liquidaciones().isEmpty()
            ? periodo.seleccion().stream()
                .map(seleccion -> almacen.contrato(periodo.empresaId(), seleccion.contratoId()))
                .toList()
            : periodo.liquidaciones().stream().map(LiquidacionNomina::contrato).toList());
  }

  public Detalle consultar(Actor actor, UUID empresa, UUID id) {
    acceso.require(actor, empresa);
    return almacen.transaccion(empresa, () -> detalle(almacen.periodo(empresa, id)));
  }

  public Detalle crear(Actor actor, UUID empresa, ValoresPeriodo valores) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          exigir(valores != null && valores.id() != null);
          var anterior = almacen.periodoOpcional(empresa, valores.id());
          if (anterior.isPresent()) {
            var existente = anterior.get();
            if (existente.anio() != valores.anio()
                || existente.mes() != valores.mes()
                || !existente.descripcion().equals(texto(valores.descripcion(), 200, true))
                || !existente.elaboradoPor().equals(texto(valores.elaboradoPor(), 100, true))
                || !existente.seleccion().equals(valores.seleccion()))
              throw new BusinessException(IDEMPOTENCY_CONFLICT);
            return detalle(existente);
          }
          var ahora = reloj.instant();
          var periodo =
              new PeriodoNomina(
                  valores.id(),
                  empresa,
                  valores.anio(),
                  valores.mes(),
                  valores.descripcion(),
                  valores.elaboradoPor(),
                  PeriodoNomina.Estado.BORRADOR,
                  valores.seleccion(),
                  List.of(),
                  List.of(),
                  null,
                  almacen.emisor(empresa),
                  "",
                  null,
                  "",
                  0,
                  ahora,
                  ahora);
          empleadosSeleccionados(periodo);
          almacen.guardarPeriodo(periodo, true);
          almacen.auditar(actor, empresa, periodo.id(), "CREAR_PERIODO_NOMINA");
          return detalle(periodo);
        });
  }

  private Set<UUID> empleadosSeleccionados(PeriodoNomina periodo) {
    var empleados = new HashSet<UUID>();
    for (var seleccion : periodo.seleccion()) {
      var contrato = almacen.contrato(periodo.empresaId(), seleccion.contratoId());
      if (!empleados.add(contrato.empleadoId()))
        throw new BusinessException(NOMINA_REFERENCIA_INVALIDA);
      if (!almacen.empleado(periodo.empresaId(), contrato.empleadoId()).activo())
        throw new BusinessException(NOMINA_REFERENCIA_INVALIDA);
    }
    return empleados;
  }

  public Detalle guardarNovedad(Actor actor, UUID empresa, UUID periodoId, ValoresNovedad valores) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          var periodo = almacen.periodo(empresa, periodoId);
          var novedad =
              new NovedadNomina(
                  valores.id(),
                  valores.empleadoId(),
                  valores.tipo(),
                  valores.descripcion(),
                  valores.importe(),
                  valores.soporte());
          var existente =
              periodo.novedades().stream().filter(n -> n.id().equals(novedad.id())).findFirst();
          if (existente.isPresent()) {
            if (!existente.get().equals(novedad)) throw new BusinessException(IDEMPOTENCY_CONFLICT);
            return detalle(periodo);
          }
          periodo.exigirEditable(valores.version());
          if (!empleadosSeleccionados(periodo).contains(novedad.empleadoId()))
            throw new BusinessException(NOMINA_REFERENCIA_INVALIDA);
          var novedades = new ArrayList<>(periodo.novedades());
          novedades.add(novedad);
          var siguiente =
              periodo.cambiar(
                  PeriodoNomina.Estado.BORRADOR,
                  novedades,
                  List.of(),
                  null,
                  almacen.emisor(empresa),
                  "",
                  null,
                  "",
                  reloj.instant());
          almacen.guardarPeriodo(siguiente, false);
          almacen.auditar(actor, empresa, periodoId, "AGREGAR_NOVEDAD_NOMINA");
          return detalle(siguiente);
        });
  }

  public Detalle eliminarNovedad(
      Actor actor, UUID empresa, UUID periodoId, UUID novedad, long esperada) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          var periodo = almacen.periodo(empresa, periodoId);
          periodo.exigirEditable(esperada);
          var novedades =
              periodo.novedades().stream().filter(n -> !n.id().equals(novedad)).toList();
          if (novedades.size() == periodo.novedades().size())
            throw new BusinessException(NOMINA_NO_ENCONTRADA);
          var siguiente =
              periodo.cambiar(
                  PeriodoNomina.Estado.BORRADOR,
                  novedades,
                  List.of(),
                  null,
                  almacen.emisor(empresa),
                  "",
                  null,
                  "",
                  reloj.instant());
          almacen.guardarPeriodo(siguiente, false);
          almacen.auditar(actor, empresa, periodoId, "QUITAR_NOVEDAD_NOMINA");
          return detalle(siguiente);
        });
  }

  private List<LiquidacionNomina> liquidar(PeriodoNomina periodo, ParametrosNomina parametros) {
    var mes = YearMonth.of(periodo.anio(), periodo.mes());
    parametros.exigirAplicable(mes);
    empleadosSeleccionados(periodo);
    return periodo.seleccion().stream()
        .map(
            seleccion -> {
              var contrato = almacen.contrato(periodo.empresaId(), seleccion.contratoId());
              var empleado = almacen.empleado(periodo.empresaId(), contrato.empleadoId());
              var tercero = terceros.get(periodo.empresaId(), empleado.terceroId());
              var novedades =
                  periodo.novedades().stream()
                      .filter(n -> n.empleadoId().equals(empleado.id()))
                      .toList();
              return motor.calcular(
                  mes, parametros, empleado, tercero, contrato, seleccion.diasAuxilio(), novedades);
            })
        .toList();
  }

  public Detalle calcular(Actor actor, UUID empresa, UUID id, long esperada) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          var periodo = almacen.periodo(empresa, id);
          periodo.exigirEditable(esperada);
          var parametros =
              almacen.parametrosAplicables(empresa, YearMonth.of(periodo.anio(), periodo.mes()));
          var siguiente =
              periodo.cambiar(
                  PeriodoNomina.Estado.CALCULADA,
                  periodo.novedades(),
                  liquidar(periodo, parametros),
                  parametros,
                  almacen.emisor(empresa),
                  "",
                  null,
                  "",
                  reloj.instant());
          almacen.guardarPeriodo(siguiente, false);
          almacen.auditar(actor, empresa, id, "CALCULAR_NOMINA");
          return detalle(siguiente);
        });
  }

  public Detalle aprobar(Actor actor, UUID empresa, UUID id, long esperada) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          var periodo = almacen.periodo(empresa, id);
          if (periodo.estado() == PeriodoNomina.Estado.APROBADA
              || periodo.estado() == PeriodoNomina.Estado.PAGADA) return detalle(periodo);
          periodo.exigirEditable(esperada);
          if (periodo.estado() != PeriodoNomina.Estado.CALCULADA)
            throw new BusinessException(NOMINA_RECALCULAR);
          var parametros =
              almacen.parametrosAplicables(empresa, YearMonth.of(periodo.anio(), periodo.mes()));
          if (!parametros.equals(periodo.parametros())
              || !liquidar(periodo, parametros).equals(periodo.liquidaciones())
              || !almacen.emisor(empresa).equals(periodo.emisor()))
            throw new BusinessException(NOMINA_RECALCULAR);
          var siguiente =
              periodo.cambiar(
                  PeriodoNomina.Estado.APROBADA,
                  periodo.novedades(),
                  periodo.liquidaciones(),
                  periodo.parametros(),
                  periodo.emisor(),
                  "",
                  null,
                  "",
                  reloj.instant());
          almacen.guardarPeriodo(siguiente, false);
          almacen.auditar(actor, empresa, id, "APROBAR_NOMINA");
          return detalle(siguiente);
        });
  }

  public Detalle anular(Actor actor, UUID empresa, UUID id, long esperada, String motivo) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          var periodo = almacen.periodo(empresa, id);
          version(periodo.version(), esperada);
          if (periodo.estado() == PeriodoNomina.Estado.PAGADA
              || periodo.estado() == PeriodoNomina.Estado.ANULADA)
            throw new BusinessException(NOMINA_PERIODO_CERRADO);
          var siguiente =
              periodo.cambiar(
                  PeriodoNomina.Estado.ANULADA,
                  periodo.novedades(),
                  periodo.liquidaciones(),
                  periodo.parametros(),
                  periodo.emisor(),
                  texto(motivo, 1000, true),
                  null,
                  "",
                  reloj.instant());
          almacen.guardarPeriodo(siguiente, false);
          almacen.auditar(actor, empresa, id, "ANULAR_NOMINA");
          return detalle(siguiente);
        });
  }

  public Detalle registrarPago(
      Actor actor, UUID empresa, UUID id, long esperada, LocalDate fecha, String referencia) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          var periodo = almacen.periodo(empresa, id);
          if (periodo.estado() == PeriodoNomina.Estado.PAGADA) {
            if (!periodo.fechaPago().equals(fecha)
                || !periodo.referenciaPago().equals(texto(referencia, 300, true)))
              throw new BusinessException(IDEMPOTENCY_CONFLICT);
            return detalle(periodo);
          }
          version(periodo.version(), esperada);
          if (periodo.estado() != PeriodoNomina.Estado.APROBADA)
            throw new BusinessException(NOMINA_PERIODO_CERRADO);
          var siguiente =
              periodo.cambiar(
                  PeriodoNomina.Estado.PAGADA,
                  periodo.novedades(),
                  periodo.liquidaciones(),
                  periodo.parametros(),
                  periodo.emisor(),
                  "",
                  fecha,
                  texto(referencia, 300, true),
                  reloj.instant());
          almacen.guardarPeriodo(siguiente, false);
          almacen.auditar(actor, empresa, id, "REGISTRAR_PAGO_NOMINA");
          return detalle(siguiente);
        });
  }
}
