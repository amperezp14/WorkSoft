package com.worksoft.company.domain.nomina;

import java.util.List;
import java.util.UUID;

public record LiquidacionNomina(
    UUID empleadoId,
    String codigo,
    String nombre,
    String tipoDocumento,
    String documento,
    String cargo,
    long versionEmpleado,
    long versionTercero,
    ContratoNomina contrato,
    int diasAuxilio,
    List<NovedadNomina> novedades,
    String salario,
    String adicionalSalarial,
    String auxilio,
    String devengado,
    String ibc,
    String salud,
    String pension,
    String tasaSolidaridad,
    String solidaridad,
    String otrasDeducciones,
    String retencion,
    String deducciones,
    String neto) {
  public LiquidacionNomina {
    novedades = List.copyOf(novedades);
  }
}
