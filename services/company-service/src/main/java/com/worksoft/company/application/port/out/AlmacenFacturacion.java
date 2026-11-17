package com.worksoft.company.application.port.out;

import com.worksoft.company.application.port.in.*;
import com.worksoft.company.domain.facturacion.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public interface AlmacenFacturacion {
  <T> T transaccion(UUID empresa, Supplier<T> operacion);

  Page<ProductoVenta> productos(UUID empresa, SearchPage pagina);

  ProductoVenta producto(UUID empresa, UUID id);

  ProductoVenta guardarProducto(ProductoVenta producto, boolean nuevo);

  List<DocumentoComercial.Cliente> clientes(UUID empresa, String busqueda);

  DocumentoComercial.Cliente cliente(UUID empresa, UUID id);

  DocumentoComercial.Emisor emisor(UUID empresa);

  List<CasosUsoFacturacion.Cuenta> cuentas(UUID empresa, String busqueda);

  void validarCuentas(UUID empresa, ConfiguracionFacturacion configuracion);

  ConfiguracionFacturacion configuracion(UUID empresa);

  ConfiguracionFacturacion configurar(UUID empresa, ConfiguracionFacturacion configuracion);

  Page<CasosUsoFacturacion.Resumen> documentos(
      UUID empresa,
      SearchPage pagina,
      String estado,
      String tipo,
      LocalDate desde,
      LocalDate hasta);

  DocumentoComercial documento(UUID empresa, UUID id);

  void guardarDocumento(DocumentoComercial documento, boolean nuevo);

  DocumentoComercial porClave(UUID empresa, String clave, String huella);

  void registrarClave(UUID empresa, String clave, String huella, UUID documento);

  String consecutivo(UUID empresa, DocumentoComercial.Tipo tipo, int anio, String prefijo);

  List<PagoVenta> pagos(UUID empresa, UUID documento);

  PagoVenta pagoPorClave(UUID empresa, String clave, String huella);

  void guardarPago(PagoVenta pago, String clave, String huella, boolean nuevo);

  void auditar(Actor actor, UUID empresa, UUID entidad, String operacion);

  CasosUsoFacturacion.Cartera cartera(UUID empresa, LocalDate corte);
}
