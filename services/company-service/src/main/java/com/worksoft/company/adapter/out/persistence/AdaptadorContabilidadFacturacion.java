package com.worksoft.company.adapter.out.persistence;

import com.worksoft.company.application.port.in.*;
import com.worksoft.company.application.port.out.ContabilidadFacturacion;
import com.worksoft.company.domain.facturacion.*;
import com.worksoft.company.domain.journal.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class AdaptadorContabilidadFacturacion implements ContabilidadFacturacion {
  private final JournalUseCases comprobantes;

  public AdaptadorContabilidadFacturacion(JournalUseCases comprobantes) {
    this.comprobantes = comprobantes;
  }

  private JournalLine linea(
      UUID cuenta, UUID tercero, String detalle, String valor, boolean debito) {
    return new JournalLine(
        cuenta,
        tercero,
        detalle,
        new BigDecimal(debito ? valor : "0.00"),
        new BigDecimal(debito ? "0.00" : valor));
  }

  private UUID registrar(
      Actor actor,
      DocumentoComercial documento,
      LocalDate fecha,
      String autor,
      String origen,
      String clave,
      List<JournalLine> lineas) {
    var valores =
        new JournalUseCases.Values(
            new JournalHeader(
                fecha,
                JournalHeader.Type.GENERAL,
                origen,
                "Documento comercial interno " + origen,
                "WorkSoft: " + origen,
                autor),
            lineas,
            0);
    var borrador = comprobantes.create(actor, documento.empresaId(), valores, clave);
    return comprobantes
        .post(actor, documento.empresaId(), borrador.draft().id(), borrador.draft().version())
        .draft()
        .id();
  }

  public UUID contabilizarVenta(Actor actor, DocumentoComercial documento, boolean reverso) {
    var cuentas = documento.configuracion();
    var lineas = new ArrayList<JournalLine>();
    lineas.add(
        linea(
            cuentas.cuentaCartera(),
            documento.clienteId(),
            documento.numero(),
            documento.totales().total(),
            !reverso));
    if (new BigDecimal(documento.totales().base()).signum() > 0)
      lineas.add(
          linea(
              cuentas.cuentaIngreso(),
              documento.clienteId(),
              documento.numero(),
              documento.totales().base(),
              reverso));
    if (new BigDecimal(documento.totales().iva()).signum() > 0)
      lineas.add(
          linea(
              cuentas.cuentaIva(),
              documento.clienteId(),
              documento.numero(),
              documento.totales().iva(),
              reverso));
    return registrar(
        actor,
        documento,
        documento.fecha(),
        documento.elaboradoPor(),
        documento.numero(),
        "venta_" + documento.id(),
        lineas);
  }

  public UUID contabilizarPago(
      Actor actor,
      DocumentoComercial documento,
      PagoVenta pago,
      boolean reverso,
      LocalDate fecha,
      String autor) {
    var cuentas = documento.configuracion();
    var lineas =
        List.of(
            linea(
                cuentas.cuentaRecaudo(),
                documento.clienteId(),
                documento.numero(),
                pago.importe(),
                !reverso),
            linea(
                cuentas.cuentaCartera(),
                documento.clienteId(),
                documento.numero(),
                pago.importe(),
                reverso));
    return registrar(
        actor,
        documento,
        fecha,
        autor,
        (reverso ? "Reversión pago " : "Pago ") + documento.numero(),
        (reverso ? "reverso_" : "pago_") + pago.id(),
        lineas);
  }
}
