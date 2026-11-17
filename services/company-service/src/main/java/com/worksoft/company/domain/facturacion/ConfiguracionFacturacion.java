package com.worksoft.company.domain.facturacion;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;
import static com.worksoft.company.domain.facturacion.ReglasFacturacion.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.util.UUID;

public record ConfiguracionFacturacion(
    String prefijoFactura,
    String prefijoCredito,
    String prefijoDebito,
    boolean contabilizar,
    UUID cuentaCartera,
    UUID cuentaIngreso,
    UUID cuentaIva,
    UUID cuentaRecaudo,
    String condiciones,
    long version) {
  public ConfiguracionFacturacion {
    for (var prefijo : new String[] {prefijoFactura, prefijoCredito, prefijoDebito})
      if (prefijo == null || !prefijo.matches("[A-Z][A-Z0-9]{0,7}"))
        throw new BusinessException(VALIDATION_ERROR);
    if (prefijoFactura.equals(prefijoCredito)
        || prefijoFactura.equals(prefijoDebito)
        || prefijoCredito.equals(prefijoDebito)
        || version < 0) throw new BusinessException(VALIDATION_ERROR);
    if (contabilizar
        && (cuentaCartera == null
            || cuentaIngreso == null
            || cuentaIva == null
            || cuentaRecaudo == null
            || new java.util.HashSet<>(
                        java.util.Arrays.asList(
                            cuentaCartera, cuentaIngreso, cuentaIva, cuentaRecaudo))
                    .size()
                != 4)) throw new BusinessException(CONFIGURACION_FACTURACION_INVALIDA);
    condiciones = texto(condiciones, 1000, false);
  }

  public static ConfiguracionFacturacion inicial() {
    return new ConfiguracionFacturacion(
        "INTFV",
        "INTNC",
        "INTND",
        false,
        null,
        null,
        null,
        null,
        "Documento comercial interno. No constituye factura electrónica validada por la DIAN.",
        0);
  }

  public String prefijo(DocumentoComercial.Tipo tipo) {
    return switch (tipo) {
      case FACTURA -> prefijoFactura;
      case NOTA_CREDITO -> prefijoCredito;
      case NOTA_DEBITO -> prefijoDebito;
    };
  }
}
