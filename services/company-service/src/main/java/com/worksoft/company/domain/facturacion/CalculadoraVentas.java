package com.worksoft.company.domain.facturacion;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;
import static com.worksoft.company.domain.facturacion.ReglasFacturacion.*;

import com.worksoft.company.domain.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CalculadoraVentas {
  private CalculadoraVentas() {}

  public record Entrada(
      UUID productoId, String descripcion, String cantidad, String precio, String descuento) {
    public Entrada {
      if (productoId == null) throw new BusinessException(VALIDATION_ERROR);
      descripcion = texto(descripcion, 300, false);
      cantidad = decimal(cantidad, 9, 3, true).toPlainString();
      precio = dinero(decimal(precio, 12, 2, false));
      descuento = porcentaje(descuento);
    }
  }

  public record Linea(
      UUID productoId,
      String codigo,
      String descripcion,
      String unidad,
      String cantidad,
      String precio,
      String porcentajeDescuento,
      ProductoVenta.TratamientoIva tratamientoIva,
      String tarifaIva,
      String bruto,
      String descuento,
      String base,
      String iva,
      String total) {}

  public record Totales(String bruto, String descuento, String base, String iva, String total) {}

  public static Linea calcular(Entrada entrada, ProductoVenta producto) {
    if (!producto.activo()) throw new BusinessException(REFERENCIA_FACTURACION_INVALIDA);
    var bruto =
        new BigDecimal(
            dinero(new BigDecimal(entrada.cantidad()).multiply(new BigDecimal(entrada.precio()))));
    var descuento =
        new BigDecimal(
            dinero(bruto.multiply(new BigDecimal(entrada.descuento())).movePointLeft(2)));
    var base = bruto.subtract(descuento);
    var iva =
        new BigDecimal(
            dinero(base.multiply(new BigDecimal(producto.tarifaIva())).movePointLeft(2)));
    var total = base.add(iva);
    if (total.compareTo(new BigDecimal("9999999999999999.99")) > 0)
      throw new BusinessException(VALIDATION_ERROR);
    return new Linea(
        producto.id(),
        producto.codigo(),
        entrada.descripcion().isBlank() ? producto.nombre() : entrada.descripcion(),
        producto.unidad(),
        entrada.cantidad(),
        entrada.precio(),
        entrada.descuento(),
        producto.tratamientoIva(),
        producto.tarifaIva(),
        dinero(bruto),
        dinero(descuento),
        dinero(base),
        dinero(iva),
        dinero(total));
  }

  public static Totales sumar(List<Linea> lineas) {
    if (lineas == null
        || lineas.isEmpty()
        || lineas.size() > 100
        || lineas.stream().anyMatch(java.util.Objects::isNull))
      throw new BusinessException(VALIDATION_ERROR);
    BigDecimal bruto = BigDecimal.ZERO,
        descuento = BigDecimal.ZERO,
        base = BigDecimal.ZERO,
        iva = BigDecimal.ZERO;
    for (var linea : lineas) {
      bruto = bruto.add(new BigDecimal(linea.bruto()));
      descuento = descuento.add(new BigDecimal(linea.descuento()));
      base = base.add(new BigDecimal(linea.base()));
      iva = iva.add(new BigDecimal(linea.iva()));
    }
    var total = base.add(iva);
    if (total.compareTo(new BigDecimal("9999999999999999.99")) > 0)
      throw new BusinessException(VALIDATION_ERROR);
    return new Totales(dinero(bruto), dinero(descuento), dinero(base), dinero(iva), dinero(total));
  }
}
