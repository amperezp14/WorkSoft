package com.worksoft.company.domain.nomina;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;
import static com.worksoft.company.domain.nomina.ReglasNomina.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.time.*;
import java.util.UUID;

public record ContratoNomina(
    UUID id,
    UUID empresaId,
    UUID empleadoId,
    String empleadoNombre,
    Tipo tipo,
    LocalDate inicio,
    LocalDate fin,
    String salarioMensual,
    boolean auxilioTransporte,
    long version,
    Instant actualizadoEn) {
  public enum Tipo {
    INDEFINIDO,
    FIJO
  }

  public ContratoNomina {
    exigir(
        id != null
            && empresaId != null
            && empleadoId != null
            && tipo != null
            && version >= 0
            && actualizadoEn != null);
    fecha(inicio);
    if (fin != null) {
      fecha(fin);
      exigir(!fin.isBefore(inicio));
    }
    exigir(tipo != Tipo.FIJO || fin != null);
    salarioMensual = dinero(importe(salarioMensual, true));
    empleadoNombre = texto(empleadoNombre, 200, true);
  }

  public void exigirMesCompleto(YearMonth mes) {
    if (inicio.isAfter(mes.atDay(1)) || fin != null && fin.isBefore(mes.atEndOfMonth()))
      throw new BusinessException(NOMINA_CASO_NO_SOPORTADO);
  }
}
