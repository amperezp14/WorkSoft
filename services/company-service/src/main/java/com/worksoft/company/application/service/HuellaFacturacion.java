package com.worksoft.company.application.service;

import com.worksoft.company.application.port.in.CasosUsoFacturacion;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

final class HuellaFacturacion {
  private final StringBuilder contenido = new StringBuilder();

  private HuellaFacturacion agregar(Object campo) {
    if (campo == null) contenido.append("-1:");
    else {
      var texto = campo.toString();
      contenido.append(texto.length()).append(':').append(texto);
    }
    return this;
  }

  private String calcular() {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(contenido.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  static String documento(CasosUsoFacturacion.ValoresDocumento valores) {
    var huella =
        new HuellaFacturacion()
            .agregar(valores.clienteId())
            .agregar(valores.tipo())
            .agregar(valores.documentoOrigenId())
            .agregar(valores.fecha())
            .agregar(valores.vencimiento())
            .agregar(valores.observaciones())
            .agregar(valores.referencia())
            .agregar(valores.elaboradoPor());
    if (valores.lineas() == null) huella.agregar(null);
    else {
      huella.agregar(valores.lineas().size());
      for (var linea : valores.lineas()) {
        if (linea == null) huella.agregar(null);
        else
          huella
              .agregar(linea.productoId())
              .agregar(linea.descripcion())
              .agregar(linea.cantidad())
              .agregar(linea.precio())
              .agregar(linea.descuento());
      }
    }
    return huella.calcular();
  }

  static String pago(UUID documento, CasosUsoFacturacion.ValoresPago valores) {
    return new HuellaFacturacion()
        .agregar(documento)
        .agregar(valores.fecha())
        .agregar(valores.importe())
        .agregar(valores.medio())
        .agregar(valores.referencia())
        .agregar(valores.elaboradoPor())
        .calcular();
  }
}
