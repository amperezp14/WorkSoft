package com.worksoft.company.domain.nomina;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.math.*;
import java.time.*;

public final class ReglasNomina {
  private ReglasNomina() {}

  public static void exigir(boolean condicion) {
    if (!condicion) throw new BusinessException(VALIDATION_ERROR);
  }

  public static String texto(String valor, int max, boolean requerido) {
    var limpio = valor == null ? "" : valor.strip();
    exigir(limpio.length() <= max && (!requerido || !limpio.isEmpty()));
    return limpio;
  }

  public static BigDecimal importe(String valor, boolean positivo) {
    exigir(valor != null && valor.matches("[0-9]{1,12}(\\.[0-9]{1,2})?"));
    var numero = new BigDecimal(valor);
    exigir(!positivo || numero.signum() > 0);
    return numero.setScale(2);
  }

  public static String dinero(BigDecimal valor) {
    exigir(valor.abs().compareTo(new BigDecimal("999999999999.99")) <= 0);
    return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  public static String porcentaje(String valor) {
    var numero = importe(valor, false);
    exigir(numero.compareTo(new BigDecimal("100")) <= 0);
    return dinero(numero);
  }

  public static BigDecimal porcentaje(BigDecimal base, String tasa) {
    return new BigDecimal(dinero(base.multiply(new BigDecimal(tasa)).movePointLeft(2)));
  }

  public static void fecha(LocalDate fecha) {
    exigir(fecha != null && fecha.getYear() >= 1900 && fecha.getYear() <= 9999);
  }

  public static void version(long actual, long esperada) {
    if (actual != esperada) throw new BusinessException(VERSION_CONFLICT);
  }

  public static YearMonth mes(int anio, int mes) {
    exigir(anio >= 1900 && anio <= 9999 && mes >= 1 && mes <= 12);
    return YearMonth.of(anio, mes);
  }
}
