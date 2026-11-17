package com.worksoft.company.domain.facturacion;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class ReglasFacturacion {
  private ReglasFacturacion() {}

  public static String texto(String valor, int max, boolean requerido) {
    var limpio = valor == null ? "" : valor.strip();
    if (limpio.length() > max || requerido && limpio.isEmpty())
      throw new BusinessException(VALIDATION_ERROR);
    return limpio;
  }

  public static BigDecimal decimal(String valor, int enteros, int decimales, boolean positivo) {
    if (valor == null || !valor.matches("[0-9]{1," + enteros + "}(\\.[0-9]{1," + decimales + "})?"))
      throw new BusinessException(VALIDATION_ERROR);
    var numero = new BigDecimal(valor);
    if (positivo && numero.signum() <= 0) throw new BusinessException(VALIDATION_ERROR);
    return numero;
  }

  public static String dinero(BigDecimal valor) {
    return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  public static String porcentaje(String valor) {
    var numero = decimal(valor, 3, 2, false);
    if (numero.compareTo(new BigDecimal("100")) > 0) throw new BusinessException(VALIDATION_ERROR);
    return dinero(numero);
  }

  public static void fecha(LocalDate fecha) {
    if (fecha == null || fecha.getYear() < 1 || fecha.getYear() > 9999)
      throw new BusinessException(VALIDATION_ERROR);
  }

  public static void version(long actual, long esperada) {
    if (actual != esperada) throw new BusinessException(VERSION_CONFLICT);
  }
}
