package com.worksoft.company.domain.nomina;

import static com.worksoft.company.domain.nomina.ReglasNomina.*;

import java.time.Instant;
import java.util.UUID;

public record EmpleadoNomina(
    UUID id,
    UUID empresaId,
    UUID terceroId,
    String codigo,
    String nombre,
    String documento,
    String cargo,
    boolean activo,
    long version,
    Instant actualizadoEn) {
  public EmpleadoNomina {
    exigir(
        id != null
            && empresaId != null
            && terceroId != null
            && version >= 0
            && actualizadoEn != null);
    codigo = texto(codigo, 30, true).toUpperCase(java.util.Locale.ROOT);
    cargo = texto(cargo, 150, true);
    nombre = texto(nombre, 200, true);
    documento = texto(documento, 30, true);
  }
}
