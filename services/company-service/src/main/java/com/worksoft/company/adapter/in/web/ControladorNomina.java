package com.worksoft.company.adapter.in.web;

import com.worksoft.company.application.port.in.*;
import com.worksoft.company.domain.nomina.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1/companies/{empresa}/nomina")
public class ControladorNomina {
  private final CasosUsoNomina servicio;

  public ControladorNomina(CasosUsoNomina servicio) {
    this.servicio = servicio;
  }

  public record Empleado(
      @NotNull UUID terceroId,
      @NotBlank String codigo,
      @NotBlank String cargo,
      @NotNull Boolean activo,
      @NotNull @PositiveOrZero Long version) {
    CasosUsoNomina.ValoresEmpleado valores() {
      return new CasosUsoNomina.ValoresEmpleado(terceroId, codigo, cargo, activo, version);
    }
  }

  public record Contrato(
      @NotNull UUID empleadoId,
      @NotNull ContratoNomina.Tipo tipo,
      @NotNull LocalDate inicio,
      LocalDate fin,
      @NotBlank String salarioMensual,
      @NotNull Boolean auxilioTransporte,
      @NotNull @PositiveOrZero Long version) {
    CasosUsoNomina.ValoresContrato valores() {
      return new CasosUsoNomina.ValoresContrato(
          empleadoId, tipo, inicio, fin, salarioMensual, auxilioTransporte, version);
    }
  }

  public record Parametros(
      @NotNull LocalDate desde,
      @NotNull LocalDate hasta,
      @NotBlank String salarioMinimo,
      @NotBlank String auxilioTransporte,
      @NotBlank String saludTrabajador,
      @NotBlank String pensionTrabajador,
      @NotBlank String fuente,
      @NotNull Boolean revisado,
      String responsableRevision,
      @NotNull @PositiveOrZero Long version) {
    CasosUsoNomina.ValoresParametros valores() {
      return new CasosUsoNomina.ValoresParametros(
          desde,
          hasta,
          salarioMinimo,
          auxilioTransporte,
          saludTrabajador,
          pensionTrabajador,
          fuente,
          revisado,
          responsableRevision,
          version);
    }
  }

  public record Periodo(
      @NotNull UUID id,
      @NotNull Integer anio,
      @NotNull Integer mes,
      @NotBlank String descripcion,
      @NotBlank String elaboradoPor,
      @NotNull @Size(min = 1, max = 100) List<PeriodoNomina.Seleccion> seleccion) {
    CasosUsoNomina.ValoresPeriodo valores() {
      return new CasosUsoNomina.ValoresPeriodo(id, anio, mes, descripcion, elaboradoPor, seleccion);
    }
  }

  public record Novedad(
      @NotNull UUID id,
      @NotNull UUID empleadoId,
      @NotNull NovedadNomina.Tipo tipo,
      @NotBlank String descripcion,
      @NotBlank String importe,
      @NotBlank String soporte,
      @NotNull @PositiveOrZero Long version) {
    CasosUsoNomina.ValoresNovedad valores() {
      return new CasosUsoNomina.ValoresNovedad(
          id, empleadoId, tipo, descripcion, importe, soporte, version);
    }
  }

  public record Version(@NotNull @PositiveOrZero Long version) {}

  public record Anulacion(@NotNull @PositiveOrZero Long version, @NotBlank String motivo) {}

  public record Pago(
      @NotNull @PositiveOrZero Long version,
      @NotNull LocalDate fecha,
      @NotBlank String referencia) {}

  @GetMapping("/empleados")
  public Page<EmpleadoNomina> empleados(
      Authentication auth,
      @PathVariable UUID empresa,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return servicio.empleados(CurrentActor.from(auth), empresa, new SearchPage(q, page, size));
  }

  @PostMapping("/empleados")
  public EmpleadoNomina crearEmpleado(
      Authentication auth, @PathVariable UUID empresa, @Valid @RequestBody Empleado solicitud) {
    return servicio.guardarEmpleado(CurrentActor.from(auth), empresa, null, solicitud.valores());
  }

  @PutMapping("/empleados/{id}")
  public EmpleadoNomina editarEmpleado(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody Empleado solicitud) {
    return servicio.guardarEmpleado(CurrentActor.from(auth), empresa, id, solicitud.valores());
  }

  @GetMapping("/contratos")
  public Page<ContratoNomina> contratos(
      Authentication auth,
      @PathVariable UUID empresa,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return servicio.contratos(CurrentActor.from(auth), empresa, new SearchPage(q, page, size));
  }

  @PostMapping("/contratos")
  public ContratoNomina crearContrato(
      Authentication auth, @PathVariable UUID empresa, @Valid @RequestBody Contrato solicitud) {
    return servicio.guardarContrato(CurrentActor.from(auth), empresa, null, solicitud.valores());
  }

  @PutMapping("/contratos/{id}")
  public ContratoNomina editarContrato(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody Contrato solicitud) {
    return servicio.guardarContrato(CurrentActor.from(auth), empresa, id, solicitud.valores());
  }

  @GetMapping("/parametros")
  public List<ParametrosNomina> parametros(Authentication auth, @PathVariable UUID empresa) {
    return servicio.parametros(CurrentActor.from(auth), empresa);
  }

  @PostMapping("/parametros")
  public ParametrosNomina crearParametros(
      Authentication auth, @PathVariable UUID empresa, @Valid @RequestBody Parametros solicitud) {
    return servicio.guardarParametros(CurrentActor.from(auth), empresa, null, solicitud.valores());
  }

  @PutMapping("/parametros/{id}")
  public ParametrosNomina editarParametros(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody Parametros solicitud) {
    return servicio.guardarParametros(CurrentActor.from(auth), empresa, id, solicitud.valores());
  }

  @GetMapping("/periodos")
  public Page<CasosUsoNomina.Resumen> periodos(
      Authentication auth,
      @PathVariable UUID empresa,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return servicio.periodos(CurrentActor.from(auth), empresa, new SearchPage(q, page, size));
  }

  @PostMapping("/periodos")
  public CasosUsoNomina.Detalle crear(
      Authentication auth, @PathVariable UUID empresa, @Valid @RequestBody Periodo solicitud) {
    return servicio.crear(CurrentActor.from(auth), empresa, solicitud.valores());
  }

  @GetMapping("/periodos/{id}")
  public CasosUsoNomina.Detalle consultar(
      Authentication auth, @PathVariable UUID empresa, @PathVariable UUID id) {
    return servicio.consultar(CurrentActor.from(auth), empresa, id);
  }

  @PostMapping("/periodos/{id}/novedades")
  public CasosUsoNomina.Detalle novedad(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody Novedad solicitud) {
    return servicio.guardarNovedad(CurrentActor.from(auth), empresa, id, solicitud.valores());
  }

  @DeleteMapping("/periodos/{id}/novedades/{novedad}")
  public CasosUsoNomina.Detalle eliminarNovedad(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @PathVariable UUID novedad,
      @RequestParam long version) {
    return servicio.eliminarNovedad(CurrentActor.from(auth), empresa, id, novedad, version);
  }

  @PostMapping("/periodos/{id}/calcular")
  public CasosUsoNomina.Detalle calcular(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody Version solicitud) {
    return servicio.calcular(CurrentActor.from(auth), empresa, id, solicitud.version());
  }

  @PostMapping("/periodos/{id}/aprobar")
  public CasosUsoNomina.Detalle aprobar(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody Version solicitud) {
    return servicio.aprobar(CurrentActor.from(auth), empresa, id, solicitud.version());
  }

  @PostMapping("/periodos/{id}/anular")
  public CasosUsoNomina.Detalle anular(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody Anulacion solicitud) {
    return servicio.anular(
        CurrentActor.from(auth), empresa, id, solicitud.version(), solicitud.motivo());
  }

  @PostMapping("/periodos/{id}/pago")
  public CasosUsoNomina.Detalle pago(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody Pago solicitud) {
    return servicio.registrarPago(
        CurrentActor.from(auth),
        empresa,
        id,
        solicitud.version(),
        solicitud.fecha(),
        solicitud.referencia());
  }
}
