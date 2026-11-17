package com.worksoft.company.domain.nomina;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;
import static com.worksoft.company.domain.nomina.ReglasNomina.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.time.*;
import java.util.UUID;

public record ParametrosNomina(
    UUID id,
    UUID empresaId,
    LocalDate desde,
    LocalDate hasta,
    String salarioMinimo,
    String auxilioTransporte,
    String saludTrabajador,
    String pensionTrabajador,
    String estrategia,
    String fuente,
    boolean revisado,
    String responsableRevision,
    long version,
    Instant actualizadoEn) {
  public ParametrosNomina {
    exigir(id != null && empresaId != null && version >= 0 && actualizadoEn != null);
    fecha(desde);
    fecha(hasta);
    exigir(!hasta.isBefore(desde));
    salarioMinimo = dinero(importe(salarioMinimo, true));
    auxilioTransporte = dinero(importe(auxilioTransporte, false));
    saludTrabajador = porcentaje(saludTrabajador);
    pensionTrabajador = porcentaje(pensionTrabajador);
    exigir(estrategia != null && estrategia.equals("ORDINARIA_LEY100_V1"));
    fuente = texto(fuente, 1500, true);
    responsableRevision = texto(responsableRevision, 100, revisado);
  }

  public void exigirAplicable(YearMonth mes) {
    if (!revisado || desde.isAfter(mes.atDay(1)) || hasta.isBefore(mes.atEndOfMonth()))
      throw new BusinessException(NOMINA_PARAMETROS_INVALIDOS);
  }
}
