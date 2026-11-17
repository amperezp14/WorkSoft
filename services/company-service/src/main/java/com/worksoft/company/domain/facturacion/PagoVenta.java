package com.worksoft.company.domain.facturacion;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PagoVenta(
    UUID id,
    UUID empresaId,
    UUID documentoId,
    LocalDate fecha,
    String importe,
    Medio medio,
    String referencia,
    String elaboradoPor,
    boolean anulado,
    LocalDate fechaAnulacion,
    String motivoAnulacion,
    String anuladoPor,
    UUID comprobanteId,
    UUID comprobanteReversionId,
    Instant creadoEn) {
  public PagoVenta {
    if (id == null
        || empresaId == null
        || documentoId == null
        || medio == null
        || creadoEn == null
        || anulado != (fechaAnulacion != null)) throw new BusinessException(VALIDATION_ERROR);
    ReglasFacturacion.fecha(fecha);
    importe = ReglasFacturacion.dinero(ReglasFacturacion.decimal(importe, 16, 2, true));
    referencia = ReglasFacturacion.texto(referencia, 300, true);
    elaboradoPor = ReglasFacturacion.texto(elaboradoPor, 100, true);
    motivoAnulacion = ReglasFacturacion.texto(motivoAnulacion, 1000, anulado);
    anuladoPor = ReglasFacturacion.texto(anuladoPor, 100, anulado);
    if (fechaAnulacion != null) {
      ReglasFacturacion.fecha(fechaAnulacion);
      if (fechaAnulacion.isBefore(fecha)) throw new BusinessException(VALIDATION_ERROR);
    }
  }

  public enum Medio {
    EFECTIVO,
    TRANSFERENCIA,
    TARJETA,
    OTRO
  }

  public static void exigirRecaudable(
      DocumentoComercial documento, LocalDate fecha, BigDecimal importe, BigDecimal recaudado) {
    ReglasFacturacion.fecha(fecha);
    if (documento.estado() != DocumentoComercial.Estado.CONFIRMADO
        || documento.tipo() == DocumentoComercial.Tipo.NOTA_CREDITO
        || fecha.isBefore(documento.fecha())
        || importe.signum() <= 0
        || importe.compareTo(new BigDecimal(documento.totales().total()).subtract(recaudado)) > 0)
      throw new BusinessException(PAGO_FACTURACION_INVALIDO);
  }
}
