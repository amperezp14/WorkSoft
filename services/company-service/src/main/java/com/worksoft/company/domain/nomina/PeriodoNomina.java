package com.worksoft.company.domain.nomina;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;
import static com.worksoft.company.domain.nomina.ReglasNomina.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record PeriodoNomina(
    UUID id,
    UUID empresaId,
    int anio,
    int mes,
    String descripcion,
    String elaboradoPor,
    Estado estado,
    List<Seleccion> seleccion,
    List<NovedadNomina> novedades,
    List<LiquidacionNomina> liquidaciones,
    ParametrosNomina parametros,
    Emisor emisor,
    String motivoAnulacion,
    LocalDate fechaPago,
    String referenciaPago,
    long version,
    Instant creadoEn,
    Instant actualizadoEn) {
  public enum Estado {
    BORRADOR,
    CALCULADA,
    APROBADA,
    PAGADA,
    ANULADA
  }

  public record Seleccion(UUID contratoId, int diasAuxilio) {
    public Seleccion {
      exigir(contratoId != null && diasAuxilio >= 0 && diasAuxilio <= 30);
    }
  }

  public record Emisor(String nombre, String nit, String digitoVerificacion) {}

  public record Totales(String devengado, String deducciones, String neto, int empleados) {}

  public PeriodoNomina {
    exigir(
        id != null
            && empresaId != null
            && estado != null
            && version >= 0
            && creadoEn != null
            && actualizadoEn != null
            && emisor != null);
    ReglasNomina.mes(anio, mes);
    descripcion = texto(descripcion, 200, true);
    elaboradoPor = texto(elaboradoPor, 100, true);
    motivoAnulacion = texto(motivoAnulacion, 1000, estado == Estado.ANULADA);
    referenciaPago = texto(referenciaPago, 300, estado == Estado.PAGADA);
    exigir(
        seleccion != null
            && !seleccion.isEmpty()
            && seleccion.size() <= 100
            && novedades != null
            && novedades.size() <= 500
            && liquidaciones != null);
    exigir(
        seleccion.stream().noneMatch(Objects::isNull)
            && novedades.stream().noneMatch(Objects::isNull)
            && liquidaciones.stream().noneMatch(Objects::isNull));
    seleccion = List.copyOf(seleccion);
    novedades = List.copyOf(novedades);
    liquidaciones = List.copyOf(liquidaciones);
    exigir(seleccion.stream().map(Seleccion::contratoId).distinct().count() == seleccion.size());
    exigir(novedades.stream().map(NovedadNomina::id).distinct().count() == novedades.size());
    if (estado == Estado.CALCULADA || estado == Estado.APROBADA || estado == Estado.PAGADA)
      exigir(parametros != null && liquidaciones.size() == seleccion.size());
    if (estado == Estado.PAGADA) {
      fecha(fechaPago);
      exigir(!fechaPago.isBefore(YearMonth.of(anio, mes).atDay(1)));
    } else exigir(fechaPago == null);
  }

  public void exigirEditable(long esperada) {
    ReglasNomina.version(version, esperada);
    if (estado != Estado.BORRADOR && estado != Estado.CALCULADA)
      throw new BusinessException(NOMINA_PERIODO_CERRADO);
  }

  public Totales totales() {
    BigDecimal devengado = BigDecimal.ZERO, deducciones = BigDecimal.ZERO, neto = BigDecimal.ZERO;
    for (var linea : liquidaciones) {
      devengado = devengado.add(new BigDecimal(linea.devengado()));
      deducciones = deducciones.add(new BigDecimal(linea.deducciones()));
      neto = neto.add(new BigDecimal(linea.neto()));
    }
    return new Totales(dinero(devengado), dinero(deducciones), dinero(neto), liquidaciones.size());
  }

  public PeriodoNomina cambiar(
      Estado nuevo,
      List<NovedadNomina> novedades,
      List<LiquidacionNomina> liquidaciones,
      ParametrosNomina parametros,
      Emisor emisor,
      String motivo,
      LocalDate pago,
      String referencia,
      Instant ahora) {
    return new PeriodoNomina(
        id,
        empresaId,
        anio,
        mes,
        descripcion,
        elaboradoPor,
        nuevo,
        seleccion,
        novedades,
        liquidaciones,
        parametros,
        emisor,
        motivo,
        pago,
        referencia,
        version + 1,
        creadoEn,
        ahora);
  }
}
