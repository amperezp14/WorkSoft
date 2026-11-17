package com.worksoft.company.adapter.out.persistence;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;

import com.worksoft.company.application.port.in.*;
import com.worksoft.company.application.port.out.AlmacenNomina;
import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.nomina.*;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

public final class PostgresAlmacenNomina implements AlmacenNomina {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transacciones;
  private final JsonMapper json;
  private final JdbcCompanyLock bloqueo;

  public PostgresAlmacenNomina(
      JdbcTemplate jdbc, TransactionTemplate transacciones, JsonMapper json) {
    this.jdbc = jdbc;
    this.transacciones = transacciones;
    this.json = json;
    bloqueo = new JdbcCompanyLock(jdbc);
  }

  public <T> T transaccion(UUID empresa, Supplier<T> operacion) {
    return transacciones.execute(
        estado -> {
          bloqueo.acquire(empresa);
          return operacion.get();
        });
  }

  // Los nombres de tabla son constantes internas, nunca proceden de la solicitud HTTP.
  private <T> Page<T> listar(String tabla, UUID empresa, SearchPage pagina, Class<T> tipo) {
    var patron = SqlPatterns.contains(pagina.query());
    var total =
        jdbc.queryForObject(
            "SELECT count(*) FROM " + tabla + " WHERE empresa_id=? AND nombre ILIKE ? ESCAPE '\\'",
            Long.class,
            empresa,
            patron);
    var elementos =
        jdbc.query(
            "SELECT contenido::text FROM "
                + tabla
                + " WHERE empresa_id=? AND nombre ILIKE ? ESCAPE '\\' ORDER BY nombre,id LIMIT ?"
                + " OFFSET ?",
            (rs, n) -> json.readValue(rs.getString(1), tipo),
            empresa,
            patron,
            pagina.size(),
            pagina.page() * pagina.size());
    return new Page<>(elementos, total, pagina.page(), pagina.size());
  }

  private <T> Optional<T> buscar(String tabla, UUID empresa, UUID id, Class<T> tipo) {
    return jdbc
        .query(
            "SELECT contenido::text FROM " + tabla + " WHERE empresa_id=? AND id=?",
            (rs, n) -> json.readValue(rs.getString(1), tipo),
            empresa,
            id)
        .stream()
        .findFirst();
  }

  private <T> T exigir(Optional<T> valor) {
    return valor.orElseThrow(() -> new BusinessException(NOMINA_NO_ENCONTRADA));
  }

  public Page<EmpleadoNomina> empleados(UUID empresa, SearchPage pagina) {
    return listar("nomina_empleados", empresa, pagina, EmpleadoNomina.class);
  }

  public EmpleadoNomina empleado(UUID empresa, UUID id) {
    return exigir(buscar("nomina_empleados", empresa, id, EmpleadoNomina.class));
  }

  public void guardarEmpleado(EmpleadoNomina empleado, boolean nuevo) {
    try {
      if (nuevo)
        jdbc.update(
            "INSERT INTO nomina_empleados(id,empresa_id,tercero_id,codigo,nombre,contenido) VALUES"
                + " (?,?,?,?,?,?::jsonb)",
            empleado.id(),
            empleado.empresaId(),
            empleado.terceroId(),
            empleado.codigo(),
            empleado.codigo() + " · " + empleado.nombre() + " · " + empleado.documento(),
            json.writeValueAsString(empleado));
      else
        jdbc.update(
            "UPDATE nomina_empleados SET codigo=?,nombre=?,contenido=?::jsonb WHERE empresa_id=?"
                + " AND id=?",
            empleado.codigo(),
            empleado.codigo() + " · " + empleado.nombre() + " · " + empleado.documento(),
            json.writeValueAsString(empleado),
            empleado.empresaId(),
            empleado.id());
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(NOMINA_REGISTRO_DUPLICADO);
    }
  }

  public Page<ContratoNomina> contratos(UUID empresa, SearchPage pagina) {
    return listar("nomina_contratos", empresa, pagina, ContratoNomina.class);
  }

  public ContratoNomina contrato(UUID empresa, UUID id) {
    return exigir(buscar("nomina_contratos", empresa, id, ContratoNomina.class));
  }

  public void guardarContrato(ContratoNomina contrato, boolean nuevo) {
    var repetidos =
        jdbc.queryForObject(
            "SELECT count(*) FROM nomina_contratos WHERE empresa_id=? AND empleado_id=? AND id<>?"
                + " AND inicio<=coalesce(?::date,'9999-12-31'::date) AND"
                + " coalesce(fin,'9999-12-31'::date)>=?",
            Long.class,
            contrato.empresaId(),
            contrato.empleadoId(),
            contrato.id(),
            contrato.fin(),
            contrato.inicio());
    if (repetidos > 0) throw new BusinessException(NOMINA_SUPERPOSICION);
    if (nuevo)
      jdbc.update(
          "INSERT INTO nomina_contratos(id,empresa_id,empleado_id,inicio,fin,nombre,contenido)"
              + " VALUES (?,?,?,?,?,?,?::jsonb)",
          contrato.id(),
          contrato.empresaId(),
          contrato.empleadoId(),
          contrato.inicio(),
          contrato.fin(),
          contrato.empleadoNombre(),
          json.writeValueAsString(contrato));
    else
      jdbc.update(
          "UPDATE nomina_contratos SET inicio=?,fin=?,nombre=?,contenido=?::jsonb WHERE"
              + " empresa_id=? AND id=?",
          contrato.inicio(),
          contrato.fin(),
          contrato.empleadoNombre(),
          json.writeValueAsString(contrato),
          contrato.empresaId(),
          contrato.id());
  }

  public List<ParametrosNomina> parametros(UUID empresa) {
    return jdbc.query(
        "SELECT contenido::text FROM nomina_parametros WHERE empresa_id=? ORDER BY desde DESC,id",
        (rs, n) -> json.readValue(rs.getString(1), ParametrosNomina.class),
        empresa);
  }

  public ParametrosNomina parametros(UUID empresa, UUID id) {
    return exigir(buscar("nomina_parametros", empresa, id, ParametrosNomina.class));
  }

  public ParametrosNomina parametrosAplicables(UUID empresa, YearMonth mes) {
    var valores =
        jdbc.query(
            "SELECT contenido::text FROM nomina_parametros WHERE empresa_id=? AND desde<=? AND"
                + " hasta>=?",
            (rs, n) -> json.readValue(rs.getString(1), ParametrosNomina.class),
            empresa,
            mes.atDay(1),
            mes.atEndOfMonth());
    if (valores.size() != 1) throw new BusinessException(NOMINA_PARAMETROS_INVALIDOS);
    return valores.getFirst();
  }

  public void guardarParametros(ParametrosNomina parametros, boolean nuevo) {
    var superpuestos =
        jdbc.queryForObject(
            "SELECT count(*) FROM nomina_parametros WHERE empresa_id=? AND id<>? AND desde<=? AND"
                + " hasta>=?",
            Long.class,
            parametros.empresaId(),
            parametros.id(),
            parametros.hasta(),
            parametros.desde());
    if (superpuestos > 0) throw new BusinessException(NOMINA_SUPERPOSICION);
    if (nuevo)
      jdbc.update(
          "INSERT INTO nomina_parametros(id,empresa_id,desde,hasta,contenido) VALUES"
              + " (?,?,?,?,?::jsonb)",
          parametros.id(),
          parametros.empresaId(),
          parametros.desde(),
          parametros.hasta(),
          json.writeValueAsString(parametros));
    else
      jdbc.update(
          "UPDATE nomina_parametros SET desde=?,hasta=?,contenido=?::jsonb WHERE empresa_id=? AND"
              + " id=?",
          parametros.desde(),
          parametros.hasta(),
          json.writeValueAsString(parametros),
          parametros.empresaId(),
          parametros.id());
  }

  public Page<CasosUsoNomina.Resumen> periodos(UUID empresa, SearchPage pagina) {
    var registros = listar("nomina_periodos", empresa, pagina, PeriodoNomina.class);
    return new Page<>(
        registros.items().stream()
            .map(
                periodo ->
                    new CasosUsoNomina.Resumen(
                        periodo.id(),
                        periodo.anio(),
                        periodo.mes(),
                        periodo.descripcion(),
                        periodo.estado(),
                        periodo.totales(),
                        periodo.version()))
            .toList(),
        registros.total(),
        registros.page(),
        registros.size());
  }

  public PeriodoNomina periodo(UUID empresa, UUID id) {
    return exigir(periodoOpcional(empresa, id));
  }

  public Optional<PeriodoNomina> periodoOpcional(UUID empresa, UUID id) {
    return buscar("nomina_periodos", empresa, id, PeriodoNomina.class);
  }

  public void guardarPeriodo(PeriodoNomina periodo, boolean nuevo) {
    try {
      if (nuevo) {
        jdbc.update(
            "INSERT INTO nomina_periodos(id,empresa_id,anio,mes,estado,nombre,contenido) VALUES"
                + " (?,?,?,?,?,?,?::jsonb)",
            periodo.id(),
            periodo.empresaId(),
            periodo.anio(),
            periodo.mes(),
            periodo.estado().name(),
            "%04d-%02d · %s".formatted(periodo.anio(), periodo.mes(), periodo.descripcion()),
            json.writeValueAsString(periodo));
        for (var seleccionado : periodo.seleccion())
          jdbc.update(
              "INSERT INTO nomina_periodo_contratos(empresa_id,periodo_id,contrato_id) VALUES"
                  + " (?,?,?)",
              periodo.empresaId(),
              periodo.id(),
              seleccionado.contratoId());
      } else
        jdbc.update(
            "UPDATE nomina_periodos SET estado=?,contenido=?::jsonb WHERE empresa_id=? AND id=?",
            periodo.estado().name(),
            json.writeValueAsString(periodo),
            periodo.empresaId(),
            periodo.id());
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(NOMINA_REGISTRO_DUPLICADO);
    }
  }

  public PeriodoNomina.Emisor emisor(UUID empresa) {
    return jdbc.queryForObject(
        "SELECT legal_name,nit,verification_digit FROM companies WHERE id=?",
        (rs, n) -> new PeriodoNomina.Emisor(rs.getString(1), rs.getString(2), rs.getString(3)),
        empresa);
  }

  public void auditar(Actor actor, UUID empresa, UUID entidad, String operacion) {
    jdbc.update(
        "INSERT INTO audit(id,company_id,issuer,subject,operation,entity_id,occurred_at) VALUES"
            + " (?,?,?,?,?,?,current_timestamp)",
        UUID.randomUUID(),
        empresa,
        actor.issuer(),
        actor.subject(),
        operacion,
        entidad);
  }
}
