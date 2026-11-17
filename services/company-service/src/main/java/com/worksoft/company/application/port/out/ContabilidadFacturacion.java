package com.worksoft.company.application.port.out;

import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.domain.facturacion.*;
import java.time.LocalDate;
import java.util.UUID;

public interface ContabilidadFacturacion {
  UUID contabilizarVenta(Actor actor, DocumentoComercial documento, boolean reverso);

  UUID contabilizarPago(
      Actor actor,
      DocumentoComercial documento,
      PagoVenta pago,
      boolean reverso,
      LocalDate fecha,
      String elaboradoPor);
}
