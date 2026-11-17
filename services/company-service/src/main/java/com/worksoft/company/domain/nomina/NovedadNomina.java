package com.worksoft.company.domain.nomina;

import static com.worksoft.company.domain.nomina.ReglasNomina.*;

import java.util.UUID;

public record NovedadNomina(
    UUID id, UUID empleadoId, Tipo tipo, String descripcion, String importe, String soporte) {
  public enum Tipo {
    BONIFICACION_SALARIAL,
    COMISION_SALARIAL,
    DEDUCCION_AUTORIZADA,
    RETENCION_FUENTE
  }

  public NovedadNomina {
    exigir(id != null && empleadoId != null && tipo != null);
    descripcion = texto(descripcion, 200, true);
    importe = dinero(ReglasNomina.importe(importe, true));
    soporte = texto(soporte, 500, true);
  }

  public boolean salarial() {
    return tipo == Tipo.BONIFICACION_SALARIAL || tipo == Tipo.COMISION_SALARIAL;
  }
}
