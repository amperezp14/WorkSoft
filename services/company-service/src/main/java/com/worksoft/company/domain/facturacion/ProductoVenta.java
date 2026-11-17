package com.worksoft.company.domain.facturacion;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;
import static com.worksoft.company.domain.facturacion.ReglasFacturacion.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.time.Instant;
import java.util.UUID;

public record ProductoVenta(
    UUID id,
    UUID empresaId,
    String codigo,
    String nombre,
    Tipo tipo,
    String unidad,
    String precio,
    TratamientoIva tratamientoIva,
    String tarifaIva,
    boolean activo,
    long version,
    Instant actualizadoEn) {
  public enum Tipo {
    PRODUCTO,
    SERVICIO
  }

  public enum TratamientoIva {
    GRAVADO,
    EXENTO,
    EXCLUIDO
  }

  public ProductoVenta {
    if (id == null || empresaId == null || tipo == null || tratamientoIva == null || version < 0)
      throw new BusinessException(VALIDATION_ERROR);
    codigo = texto(codigo, 40, true).toUpperCase(java.util.Locale.ROOT);
    nombre = texto(nombre, 200, true);
    unidad = texto(unidad, 30, true);
    precio = dinero(decimal(precio, 12, 2, false));
    tarifaIva = porcentaje(tarifaIva);
    if (tratamientoIva != TratamientoIva.GRAVADO && decimal(tarifaIva, 3, 2, false).signum() != 0)
      throw new BusinessException(VALIDATION_ERROR);
  }
}
