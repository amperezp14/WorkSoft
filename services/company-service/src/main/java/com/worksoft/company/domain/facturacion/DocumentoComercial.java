package com.worksoft.company.domain.facturacion;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DocumentoComercial(
    UUID id,
    UUID empresaId,
    UUID clienteId,
    Tipo tipo,
    UUID documentoOrigenId,
    LocalDate fecha,
    LocalDate vencimiento,
    String observaciones,
    String referencia,
    String elaboradoPor,
    Estado estado,
    String numero,
    List<CalculadoraVentas.Linea> lineas,
    CalculadoraVentas.Totales totales,
    Cliente cliente,
    Emisor emisor,
    ConfiguracionFacturacion configuracion,
    UUID comprobanteId,
    long version,
    Instant creadoEn,
    Instant actualizadoEn) {
  public enum Tipo {
    FACTURA,
    NOTA_DEBITO,
    NOTA_CREDITO
  }

  public enum Estado {
    BORRADOR,
    CONFIRMADO,
    ANULADO
  }

  public record Cliente(
      UUID id,
      String tipoDocumento,
      String documento,
      String digitoVerificacion,
      String nombre,
      String direccion,
      String correo) {}

  public record Emisor(String nombre, String nit, String digitoVerificacion) {}

  public DocumentoComercial {
    if (id == null
        || empresaId == null
        || clienteId == null
        || tipo == null
        || estado == null
        || cliente == null
        || emisor == null
        || configuracion == null
        || version < 0
        || creadoEn == null
        || actualizadoEn == null
        || !cliente.id().equals(clienteId)) throw new BusinessException(VALIDATION_ERROR);
    ReglasFacturacion.fecha(fecha);
    ReglasFacturacion.fecha(vencimiento);
    if (vencimiento.isBefore(fecha) || (tipo == Tipo.FACTURA) != (documentoOrigenId == null))
      throw new BusinessException(VALIDATION_ERROR);
    var calculados = CalculadoraVentas.sumar(lineas);
    if (!calculados.equals(totales)) throw new BusinessException(VALIDATION_ERROR);
    if (estado == Estado.BORRADOR
        ? numero != null
        : numero == null || numero.isBlank() || new BigDecimal(totales.total()).signum() <= 0)
      throw new BusinessException(VALIDATION_ERROR);
    observaciones = ReglasFacturacion.texto(observaciones, 1000, false);
    referencia = ReglasFacturacion.texto(referencia, 300, false);
    elaboradoPor = ReglasFacturacion.texto(elaboradoPor, 100, true);
    lineas = List.copyOf(lineas);
  }

  public void exigirEditable(long esperada) {
    ReglasFacturacion.version(version, esperada);
    if (estado != Estado.BORRADOR) throw new BusinessException(DOCUMENTO_NO_EDITABLE);
  }

  public void exigirConfirmable(long esperada) {
    exigirEditable(esperada);
    if (new BigDecimal(totales.total()).signum() <= 0)
      throw new BusinessException(VALIDATION_ERROR);
  }

  public void exigirReversible(long esperada, BigDecimal recaudado) {
    ReglasFacturacion.version(version, esperada);
    if (estado != Estado.CONFIRMADO || tipo == Tipo.NOTA_CREDITO || recaudado.signum() != 0)
      throw new BusinessException(REVERSO_FACTURACION_INVALIDO);
  }

  public DocumentoComercial cambiarEstado(Estado nuevo, Instant ahora) {
    return new DocumentoComercial(
        id,
        empresaId,
        clienteId,
        tipo,
        documentoOrigenId,
        fecha,
        vencimiento,
        observaciones,
        referencia,
        elaboradoPor,
        nuevo,
        numero,
        lineas,
        totales,
        cliente,
        emisor,
        configuracion,
        comprobanteId,
        version + 1,
        creadoEn,
        ahora);
  }
}
