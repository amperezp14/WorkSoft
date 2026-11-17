package com.worksoft.company.adapter.in.web;

import com.worksoft.company.application.port.in.*;
import com.worksoft.company.domain.facturacion.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1/companies/{empresa}/facturacion")
public class ControladorFacturacion {
  private final CasosUsoFacturacion servicio;

  public ControladorFacturacion(CasosUsoFacturacion servicio) {
    this.servicio = servicio;
  }

  public record SolicitudProducto(
      @NotBlank String codigo,
      @NotBlank String nombre,
      @NotNull ProductoVenta.Tipo tipo,
      @NotBlank String unidad,
      @NotBlank String precio,
      @NotNull ProductoVenta.TratamientoIva tratamientoIva,
      @NotBlank String tarifaIva,
      @NotNull Boolean activo,
      @NotNull @PositiveOrZero Long version) {
    CasosUsoFacturacion.ValoresProducto valores() {
      return new CasosUsoFacturacion.ValoresProducto(
          codigo, nombre, tipo, unidad, precio, tratamientoIva, tarifaIva, activo, version);
    }
  }

  public record SolicitudDocumento(
      @NotNull UUID clienteId,
      @NotNull DocumentoComercial.Tipo tipo,
      UUID documentoOrigenId,
      @NotNull LocalDate fecha,
      @NotNull LocalDate vencimiento,
      String observaciones,
      String referencia,
      @NotBlank String elaboradoPor,
      @NotNull @Size(min = 1, max = 100) List<CalculadoraVentas.Entrada> lineas,
      @NotNull @PositiveOrZero Long version) {
    CasosUsoFacturacion.ValoresDocumento valores() {
      return new CasosUsoFacturacion.ValoresDocumento(
          clienteId,
          tipo,
          documentoOrigenId,
          fecha,
          vencimiento,
          observaciones,
          referencia,
          elaboradoPor,
          lineas,
          version);
    }
  }

  public record SolicitudPago(
      @NotNull LocalDate fecha,
      @NotBlank String importe,
      @NotNull PagoVenta.Medio medio,
      @NotBlank String referencia,
      @NotBlank String elaboradoPor,
      @NotNull @PositiveOrZero Long version) {
    CasosUsoFacturacion.ValoresPago valores() {
      return new CasosUsoFacturacion.ValoresPago(
          fecha, importe, medio, referencia, elaboradoPor, version);
    }
  }

  public record SolicitudReversion(
      @NotNull LocalDate fecha,
      @NotBlank String motivo,
      @NotBlank String elaboradoPor,
      @NotNull @PositiveOrZero Long version) {
    CasosUsoFacturacion.Reversion valores() {
      return new CasosUsoFacturacion.Reversion(fecha, motivo, elaboradoPor, version);
    }
  }

  public record Version(@NotNull @PositiveOrZero Long version) {}

  @GetMapping("/productos")
  public Page<ProductoVenta> productos(
      Authentication auth,
      @PathVariable UUID empresa,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return servicio.productos(CurrentActor.from(auth), empresa, new SearchPage(q, page, size));
  }

  @PostMapping("/productos")
  public ProductoVenta crearProducto(
      Authentication auth,
      @PathVariable UUID empresa,
      @Valid @RequestBody SolicitudProducto solicitud) {
    return servicio.guardarProducto(CurrentActor.from(auth), empresa, null, solicitud.valores());
  }

  @PutMapping("/productos/{id}")
  public ProductoVenta actualizarProducto(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody SolicitudProducto solicitud) {
    return servicio.guardarProducto(CurrentActor.from(auth), empresa, id, solicitud.valores());
  }

  @GetMapping("/clientes")
  public List<DocumentoComercial.Cliente> clientes(
      Authentication auth, @PathVariable UUID empresa, @RequestParam(defaultValue = "") String q) {
    return servicio.clientes(CurrentActor.from(auth), empresa, q);
  }

  @GetMapping("/cuentas")
  public List<CasosUsoFacturacion.Cuenta> cuentas(
      Authentication auth, @PathVariable UUID empresa, @RequestParam(defaultValue = "") String q) {
    return servicio.cuentas(CurrentActor.from(auth), empresa, q);
  }

  @GetMapping("/configuracion")
  public ConfiguracionFacturacion configuracion(Authentication auth, @PathVariable UUID empresa) {
    return servicio.configuracion(CurrentActor.from(auth), empresa);
  }

  @PutMapping("/configuracion")
  public ConfiguracionFacturacion configurar(
      Authentication auth,
      @PathVariable UUID empresa,
      @RequestBody ConfiguracionFacturacion solicitud) {
    return servicio.configurar(CurrentActor.from(auth), empresa, solicitud);
  }

  @GetMapping("/documentos")
  public Page<CasosUsoFacturacion.Resumen> documentos(
      Authentication auth,
      @PathVariable UUID empresa,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      @RequestParam(defaultValue = "") String estado,
      @RequestParam(defaultValue = "") String tipo,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta) {
    return servicio.documentos(
        CurrentActor.from(auth),
        empresa,
        new SearchPage(q, page, size),
        estado,
        tipo,
        desde,
        hasta);
  }

  @GetMapping("/documentos/{id}")
  public CasosUsoFacturacion.Detalle documento(
      Authentication auth, @PathVariable UUID empresa, @PathVariable UUID id) {
    return servicio.consultar(CurrentActor.from(auth), empresa, id);
  }

  @PostMapping("/documentos")
  public CasosUsoFacturacion.Detalle crear(
      Authentication auth,
      @PathVariable UUID empresa,
      @Valid @RequestBody SolicitudDocumento solicitud,
      @RequestHeader("Idempotency-Key") String clave) {
    return servicio.guardar(CurrentActor.from(auth), empresa, null, solicitud.valores(), clave);
  }

  @PutMapping("/documentos/{id}")
  public CasosUsoFacturacion.Detalle actualizar(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody SolicitudDocumento solicitud) {
    return servicio.guardar(CurrentActor.from(auth), empresa, id, solicitud.valores(), null);
  }

  @PostMapping("/documentos/{id}/confirmar")
  public CasosUsoFacturacion.Detalle confirmar(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody Version solicitud) {
    return servicio.confirmar(CurrentActor.from(auth), empresa, id, solicitud.version());
  }

  @PostMapping("/documentos/{id}/nota-credito")
  public CasosUsoFacturacion.Detalle acreditar(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody SolicitudReversion solicitud) {
    return servicio.acreditar(CurrentActor.from(auth), empresa, id, solicitud.valores());
  }

  @PostMapping("/documentos/{id}/pagos")
  public CasosUsoFacturacion.Detalle pagar(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @Valid @RequestBody SolicitudPago solicitud,
      @RequestHeader("Idempotency-Key") String clave) {
    return servicio.pagar(CurrentActor.from(auth), empresa, id, solicitud.valores(), clave);
  }

  @PostMapping("/documentos/{id}/pagos/{pago}/anular")
  public CasosUsoFacturacion.Detalle anularPago(
      Authentication auth,
      @PathVariable UUID empresa,
      @PathVariable UUID id,
      @PathVariable UUID pago,
      @Valid @RequestBody SolicitudReversion solicitud) {
    return servicio.anularPago(CurrentActor.from(auth), empresa, id, pago, solicitud.valores());
  }

  @GetMapping("/cartera")
  public CasosUsoFacturacion.Cartera cartera(
      Authentication auth, @PathVariable UUID empresa, @RequestParam LocalDate corte) {
    return servicio.cartera(CurrentActor.from(auth), empresa, corte);
  }
}
