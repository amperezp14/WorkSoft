package com.worksoft.company.application.port.in;

import com.worksoft.company.domain.facturacion.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CasosUsoFacturacion {
  record ValoresProducto(
      String codigo,
      String nombre,
      ProductoVenta.Tipo tipo,
      String unidad,
      String precio,
      ProductoVenta.TratamientoIva tratamientoIva,
      String tarifaIva,
      boolean activo,
      long version) {}

  record ValoresDocumento(
      UUID clienteId,
      DocumentoComercial.Tipo tipo,
      UUID documentoOrigenId,
      LocalDate fecha,
      LocalDate vencimiento,
      String observaciones,
      String referencia,
      String elaboradoPor,
      List<CalculadoraVentas.Entrada> lineas,
      long version) {}

  record ValoresPago(
      LocalDate fecha,
      String importe,
      PagoVenta.Medio medio,
      String referencia,
      String elaboradoPor,
      long version) {}

  record Reversion(LocalDate fecha, String motivo, String elaboradoPor, long version) {}

  record Detalle(
      DocumentoComercial documento, List<PagoVenta> pagos, String recaudado, String saldo) {}

  record Resumen(
      UUID id,
      DocumentoComercial.Tipo tipo,
      String numero,
      LocalDate fecha,
      LocalDate vencimiento,
      String cliente,
      DocumentoComercial.Estado estado,
      String total,
      String recaudado,
      String saldo,
      long version) {}

  record Cartera(
      String ventas,
      String iva,
      String recaudado,
      String pendiente,
      String vencido,
      long documentosPendientes) {}

  record Cuenta(UUID id, String codigo, String nombre) {}

  Page<ProductoVenta> productos(Actor actor, UUID empresa, SearchPage pagina);

  ProductoVenta guardarProducto(Actor actor, UUID empresa, UUID id, ValoresProducto valores);

  List<DocumentoComercial.Cliente> clientes(Actor actor, UUID empresa, String busqueda);

  List<Cuenta> cuentas(Actor actor, UUID empresa, String busqueda);

  ConfiguracionFacturacion configuracion(Actor actor, UUID empresa);

  ConfiguracionFacturacion configurar(Actor actor, UUID empresa, ConfiguracionFacturacion valores);

  Page<Resumen> documentos(
      Actor actor,
      UUID empresa,
      SearchPage pagina,
      String estado,
      String tipo,
      LocalDate desde,
      LocalDate hasta);

  Detalle consultar(Actor actor, UUID empresa, UUID id);

  Detalle guardar(Actor actor, UUID empresa, UUID id, ValoresDocumento valores, String clave);

  Detalle confirmar(Actor actor, UUID empresa, UUID id, long version);

  Detalle acreditar(Actor actor, UUID empresa, UUID id, Reversion valores);

  Detalle pagar(Actor actor, UUID empresa, UUID id, ValoresPago valores, String clave);

  Detalle anularPago(Actor actor, UUID empresa, UUID documento, UUID pago, Reversion valores);

  Cartera cartera(Actor actor, UUID empresa, LocalDate corte);
}
