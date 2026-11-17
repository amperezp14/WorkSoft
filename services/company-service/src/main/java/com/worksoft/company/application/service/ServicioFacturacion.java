package com.worksoft.company.application.service;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;
import static com.worksoft.company.domain.facturacion.ReglasFacturacion.*;

import com.worksoft.company.application.port.in.*;
import com.worksoft.company.application.port.out.*;
import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.facturacion.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ServicioFacturacion implements CasosUsoFacturacion {
  private final CompanyAccess acceso;
  private final AlmacenFacturacion almacen;
  private final ContabilidadFacturacion contabilidad;
  private final Clock reloj;

  public ServicioFacturacion(
      CompanyAccess acceso,
      AlmacenFacturacion almacen,
      ContabilidadFacturacion contabilidad,
      Clock reloj) {
    this.acceso = acceso;
    this.almacen = almacen;
    this.contabilidad = contabilidad;
    this.reloj = reloj;
  }

  public Page<ProductoVenta> productos(Actor actor, UUID empresa, SearchPage pagina) {
    acceso.require(actor, empresa);
    return almacen.productos(empresa, pagina);
  }

  public ProductoVenta guardarProducto(
      Actor actor, UUID empresa, UUID id, ValoresProducto valores) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          if (valores == null) throw new BusinessException(VALIDATION_ERROR);
          long siguiente = 0;
          if (id != null) {
            var anterior = almacen.producto(empresa, id);
            version(anterior.version(), valores.version());
            siguiente = anterior.version() + 1;
          }
          var producto =
              new ProductoVenta(
                  id == null ? UUID.randomUUID() : id,
                  empresa,
                  valores.codigo(),
                  valores.nombre(),
                  valores.tipo(),
                  valores.unidad(),
                  valores.precio(),
                  valores.tratamientoIva(),
                  valores.tarifaIva(),
                  valores.activo(),
                  siguiente,
                  reloj.instant());
          var guardado = almacen.guardarProducto(producto, id == null);
          almacen.auditar(actor, empresa, guardado.id(), "GUARDAR_PRODUCTO");
          return guardado;
        });
  }

  private String busqueda(String valor) {
    return texto(valor, 100, false);
  }

  public List<DocumentoComercial.Cliente> clientes(Actor actor, UUID empresa, String busqueda) {
    acceso.require(actor, empresa);
    return almacen.clientes(empresa, busqueda(busqueda));
  }

  public List<Cuenta> cuentas(Actor actor, UUID empresa, String busqueda) {
    acceso.require(actor, empresa);
    return almacen.cuentas(empresa, busqueda(busqueda));
  }

  public ConfiguracionFacturacion configuracion(Actor actor, UUID empresa) {
    acceso.require(actor, empresa);
    return almacen.configuracion(empresa);
  }

  public ConfiguracionFacturacion configurar(
      Actor actor, UUID empresa, ConfiguracionFacturacion valores) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          version(almacen.configuracion(empresa).version(), valores.version());
          almacen.validarCuentas(empresa, valores);
          var nueva =
              new ConfiguracionFacturacion(
                  valores.prefijoFactura(),
                  valores.prefijoCredito(),
                  valores.prefijoDebito(),
                  valores.contabilizar(),
                  valores.cuentaCartera(),
                  valores.cuentaIngreso(),
                  valores.cuentaIva(),
                  valores.cuentaRecaudo(),
                  valores.condiciones(),
                  valores.version() + 1);
          almacen.auditar(actor, empresa, empresa, "CONFIGURAR_FACTURACION");
          return almacen.configurar(empresa, nueva);
        });
  }

  public Page<Resumen> documentos(
      Actor actor,
      UUID empresa,
      SearchPage pagina,
      String estado,
      String tipo,
      LocalDate desde,
      LocalDate hasta) {
    acceso.require(actor, empresa);
    if (estado != null && !estado.isBlank())
      try {
        DocumentoComercial.Estado.valueOf(estado);
      } catch (IllegalArgumentException ex) {
        throw new BusinessException(VALIDATION_ERROR);
      }
    if (tipo != null && !tipo.isBlank() && !tipo.equals("VENTAS"))
      try {
        DocumentoComercial.Tipo.valueOf(tipo);
      } catch (IllegalArgumentException ex) {
        throw new BusinessException(VALIDATION_ERROR);
      }
    if (desde != null) fecha(desde);
    if (hasta != null) fecha(hasta);
    if (desde != null && hasta != null && desde.isAfter(hasta))
      throw new BusinessException(VALIDATION_ERROR);
    return almacen.documentos(empresa, pagina, estado, tipo, desde, hasta);
  }

  public Detalle consultar(Actor actor, UUID empresa, UUID id) {
    acceso.require(actor, empresa);
    return almacen.transaccion(empresa, () -> detalle(almacen.documento(empresa, id)));
  }

  private Detalle detalle(DocumentoComercial documento) {
    var pagos = almacen.pagos(documento.empresaId(), documento.id());
    var recaudado = recaudado(pagos);
    var saldo =
        documento.estado() == DocumentoComercial.Estado.CONFIRMADO
                && documento.tipo() != DocumentoComercial.Tipo.NOTA_CREDITO
            ? new BigDecimal(documento.totales().total()).subtract(recaudado)
            : BigDecimal.ZERO;
    return new Detalle(documento, pagos, dinero(recaudado), dinero(saldo));
  }

  private BigDecimal recaudado(List<PagoVenta> pagos) {
    return pagos.stream()
        .filter(p -> !p.anulado())
        .map(p -> new BigDecimal(p.importe()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private String clave(String valor) {
    if (valor == null || !valor.matches("[A-Za-z0-9_-]{16,128}"))
      throw new BusinessException(VALIDATION_ERROR);
    return valor;
  }

  public Detalle guardar(
      Actor actor, UUID empresa, UUID id, ValoresDocumento valores, String clave) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          if (valores == null
              || valores.clienteId() == null
              || valores.tipo() == null
              || valores.tipo() == DocumentoComercial.Tipo.NOTA_CREDITO)
            throw new BusinessException(VALIDATION_ERROR);
          String huella = HuellaFacturacion.documento(valores);
          if (id == null) {
            var anterior = almacen.porClave(empresa, clave(clave), huella);
            if (anterior != null) return detalle(anterior);
          }
          DocumentoComercial anterior = id == null ? null : almacen.documento(empresa, id);
          if (anterior != null) anterior.exigirEditable(valores.version());
          fecha(valores.fecha());
          fecha(valores.vencimiento());
          if (valores.vencimiento().isBefore(valores.fecha()))
            throw new BusinessException(VALIDATION_ERROR);
          if (valores.tipo() == DocumentoComercial.Tipo.NOTA_DEBITO) {
            if (valores.documentoOrigenId() == null) throw new BusinessException(VALIDATION_ERROR);
            var origen = almacen.documento(empresa, valores.documentoOrigenId());
            if (origen.tipo() != DocumentoComercial.Tipo.FACTURA
                || origen.estado() != DocumentoComercial.Estado.CONFIRMADO
                || !origen.clienteId().equals(valores.clienteId())
                || valores.fecha().isBefore(origen.fecha()))
              throw new BusinessException(REFERENCIA_FACTURACION_INVALIDA);
          } else if (valores.documentoOrigenId() != null)
            throw new BusinessException(VALIDATION_ERROR);
          if (valores.lineas() == null
              || valores.lineas().isEmpty()
              || valores.lineas().size() > 100
              || valores.lineas().stream().anyMatch(Objects::isNull))
            throw new BusinessException(VALIDATION_ERROR);
          var productos = new HashMap<UUID, ProductoVenta>();
          var lineas =
              valores.lineas().stream()
                  .map(
                      linea ->
                          CalculadoraVentas.calcular(
                              linea,
                              productos.computeIfAbsent(
                                  linea.productoId(),
                                  producto -> almacen.producto(empresa, producto))))
                  .toList();
          var ahora = reloj.instant();
          var documento =
              new DocumentoComercial(
                  id == null ? UUID.randomUUID() : id,
                  empresa,
                  valores.clienteId(),
                  valores.tipo(),
                  valores.documentoOrigenId(),
                  valores.fecha(),
                  valores.vencimiento(),
                  texto(valores.observaciones(), 1000, false),
                  texto(valores.referencia(), 300, false),
                  texto(valores.elaboradoPor(), 100, true),
                  DocumentoComercial.Estado.BORRADOR,
                  null,
                  lineas,
                  CalculadoraVentas.sumar(lineas),
                  almacen.cliente(empresa, valores.clienteId()),
                  almacen.emisor(empresa),
                  almacen.configuracion(empresa),
                  null,
                  anterior == null ? 0 : anterior.version() + 1,
                  anterior == null ? ahora : anterior.creadoEn(),
                  ahora);
          almacen.guardarDocumento(documento, anterior == null);
          if (anterior == null) almacen.registrarClave(empresa, clave, huella, documento.id());
          almacen.auditar(actor, empresa, documento.id(), "GUARDAR_VENTA");
          return detalle(documento);
        });
  }

  public Detalle confirmar(Actor actor, UUID empresa, UUID id, long esperada) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          var anterior = almacen.documento(empresa, id);
          if (anterior.estado() == DocumentoComercial.Estado.CONFIRMADO) return detalle(anterior);
          anterior.exigirConfirmable(esperada);
          if (anterior.tipo() == DocumentoComercial.Tipo.NOTA_DEBITO
              && almacen.documento(empresa, anterior.documentoOrigenId()).estado()
                  != DocumentoComercial.Estado.CONFIRMADO)
            throw new BusinessException(REFERENCIA_FACTURACION_INVALIDA);
          for (var linea : anterior.lineas())
            if (!almacen.producto(empresa, linea.productoId()).activo())
              throw new BusinessException(REFERENCIA_FACTURACION_INVALIDA);
          var configuracion = almacen.configuracion(empresa);
          almacen.validarCuentas(empresa, configuracion);
          var numero =
              almacen.consecutivo(
                  empresa,
                  anterior.tipo(),
                  anterior.fecha().getYear(),
                  configuracion.prefijo(anterior.tipo()));
          var documento =
              new DocumentoComercial(
                  anterior.id(),
                  empresa,
                  anterior.clienteId(),
                  anterior.tipo(),
                  anterior.documentoOrigenId(),
                  anterior.fecha(),
                  anterior.vencimiento(),
                  anterior.observaciones(),
                  anterior.referencia(),
                  anterior.elaboradoPor(),
                  DocumentoComercial.Estado.CONFIRMADO,
                  numero,
                  anterior.lineas(),
                  anterior.totales(),
                  almacen.cliente(empresa, anterior.clienteId()),
                  almacen.emisor(empresa),
                  configuracion,
                  null,
                  anterior.version() + 1,
                  anterior.creadoEn(),
                  reloj.instant());
          if (configuracion.contabilizar())
            documento =
                conComprobante(documento, contabilidad.contabilizarVenta(actor, documento, false));
          almacen.guardarDocumento(documento, false);
          almacen.auditar(actor, empresa, id, "CONFIRMAR_VENTA");
          return detalle(documento);
        });
  }

  private DocumentoComercial conComprobante(DocumentoComercial d, UUID comprobante) {
    return new DocumentoComercial(
        d.id(),
        d.empresaId(),
        d.clienteId(),
        d.tipo(),
        d.documentoOrigenId(),
        d.fecha(),
        d.vencimiento(),
        d.observaciones(),
        d.referencia(),
        d.elaboradoPor(),
        d.estado(),
        d.numero(),
        d.lineas(),
        d.totales(),
        d.cliente(),
        d.emisor(),
        d.configuracion(),
        comprobante,
        d.version(),
        d.creadoEn(),
        d.actualizadoEn());
  }

  public Detalle acreditar(Actor actor, UUID empresa, UUID id, Reversion valores) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          var original = almacen.documento(empresa, id);
          original.exigirReversible(valores.version(), recaudado(almacen.pagos(empresa, id)));
          fecha(valores.fecha());
          if (valores.fecha().isBefore(original.fecha()))
            throw new BusinessException(VALIDATION_ERROR);
          var motivo = texto(valores.motivo(), 1000, true);
          var autor = texto(valores.elaboradoPor(), 100, true);
          var configuracion = original.configuracion();
          var ahora = reloj.instant();
          var nota =
              new DocumentoComercial(
                  UUID.randomUUID(),
                  empresa,
                  original.clienteId(),
                  DocumentoComercial.Tipo.NOTA_CREDITO,
                  id,
                  valores.fecha(),
                  valores.fecha(),
                  motivo,
                  original.numero(),
                  autor,
                  DocumentoComercial.Estado.CONFIRMADO,
                  almacen.consecutivo(
                      empresa,
                      DocumentoComercial.Tipo.NOTA_CREDITO,
                      valores.fecha().getYear(),
                      configuracion.prefijoCredito()),
                  original.lineas(),
                  original.totales(),
                  original.cliente(),
                  original.emisor(),
                  configuracion,
                  null,
                  0,
                  ahora,
                  ahora);
          if (configuracion.contabilizar())
            nota = conComprobante(nota, contabilidad.contabilizarVenta(actor, nota, true));
          almacen.guardarDocumento(nota, true);
          almacen.guardarDocumento(
              original.cambiarEstado(DocumentoComercial.Estado.ANULADO, ahora), false);
          almacen.auditar(actor, empresa, id, "REVERTIR_VENTA_TOTAL");
          return detalle(nota);
        });
  }

  public Detalle pagar(Actor actor, UUID empresa, UUID id, ValoresPago valores, String clave) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          var huella = HuellaFacturacion.pago(id, valores);
          var anterior = almacen.pagoPorClave(empresa, clave(clave), huella);
          if (anterior != null) return detalle(almacen.documento(empresa, id));
          var documento = almacen.documento(empresa, id);
          version(documento.version(), valores.version());
          var importe = decimal(valores.importe(), 16, 2, true);
          PagoVenta.exigirRecaudable(
              documento, valores.fecha(), importe, recaudado(almacen.pagos(empresa, id)));
          if (valores.medio() == null) throw new BusinessException(VALIDATION_ERROR);
          var pago =
              new PagoVenta(
                  UUID.randomUUID(),
                  empresa,
                  id,
                  valores.fecha(),
                  dinero(importe),
                  valores.medio(),
                  texto(valores.referencia(), 300, true),
                  texto(valores.elaboradoPor(), 100, true),
                  false,
                  null,
                  "",
                  "",
                  null,
                  null,
                  reloj.instant());
          if (documento.configuracion().contabilizar())
            pago =
                new PagoVenta(
                    pago.id(),
                    empresa,
                    id,
                    pago.fecha(),
                    pago.importe(),
                    pago.medio(),
                    pago.referencia(),
                    pago.elaboradoPor(),
                    false,
                    null,
                    "",
                    "",
                    contabilidad.contabilizarPago(
                        actor, documento, pago, false, pago.fecha(), pago.elaboradoPor()),
                    null,
                    pago.creadoEn());
          almacen.guardarPago(pago, clave, huella, true);
          almacen.auditar(actor, empresa, pago.id(), "REGISTRAR_RECAUDO");
          return detalle(documento);
        });
  }

  public Detalle anularPago(Actor actor, UUID empresa, UUID id, UUID pagoId, Reversion valores) {
    acceso.require(actor, empresa);
    return almacen.transaccion(
        empresa,
        () -> {
          var documento = almacen.documento(empresa, id);
          version(documento.version(), valores.version());
          var pago =
              almacen.pagos(empresa, id).stream()
                  .filter(p -> p.id().equals(pagoId))
                  .findFirst()
                  .orElseThrow(() -> new BusinessException(FACTURACION_NO_ENCONTRADA));
          if (pago.anulado()) return detalle(documento);
          fecha(valores.fecha());
          if (valores.fecha().isBefore(pago.fecha())) throw new BusinessException(VALIDATION_ERROR);
          var motivo = texto(valores.motivo(), 1000, true);
          var autor = texto(valores.elaboradoPor(), 100, true);
          UUID comprobante =
              documento.configuracion().contabilizar()
                  ? contabilidad.contabilizarPago(
                      actor, documento, pago, true, valores.fecha(), autor)
                  : null;
          almacen.guardarPago(
              new PagoVenta(
                  pago.id(),
                  empresa,
                  id,
                  pago.fecha(),
                  pago.importe(),
                  pago.medio(),
                  pago.referencia(),
                  pago.elaboradoPor(),
                  true,
                  valores.fecha(),
                  motivo,
                  autor,
                  pago.comprobanteId(),
                  comprobante,
                  pago.creadoEn()),
              null,
              null,
              false);
          almacen.auditar(actor, empresa, pago.id(), "ANULAR_RECAUDO");
          return detalle(documento);
        });
  }

  public Cartera cartera(Actor actor, UUID empresa, LocalDate corte) {
    acceso.require(actor, empresa);
    fecha(corte);
    return almacen.cartera(empresa, corte);
  }
}
