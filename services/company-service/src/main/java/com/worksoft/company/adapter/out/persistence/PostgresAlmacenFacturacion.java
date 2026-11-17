package com.worksoft.company.adapter.out.persistence;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;

import com.worksoft.company.application.port.in.*;
import com.worksoft.company.application.port.out.AlmacenFacturacion;
import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.facturacion.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

public final class PostgresAlmacenFacturacion implements AlmacenFacturacion {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transacciones;
  private final JsonMapper json;
  private final JdbcCompanyLock bloqueo;

  public PostgresAlmacenFacturacion(
      JdbcTemplate jdbc, TransactionTemplate transacciones, JsonMapper json) {
    this.jdbc = jdbc;
    this.transacciones = transacciones;
    this.json = json;
    this.bloqueo = new JdbcCompanyLock(jdbc);
  }

  public <T> T transaccion(UUID empresa, Supplier<T> operacion) {
    return transacciones.execute(
        estado -> {
          bloqueo.acquire(empresa);
          return operacion.get();
        });
  }

  private <T> T leer(String contenido, Class<T> tipo) {
    return json.readValue(contenido, tipo);
  }

  private String escribir(Object valor) {
    return json.writeValueAsString(valor);
  }

  private <T> T unico(List<T> valores) {
    if (valores.isEmpty()) throw new BusinessException(FACTURACION_NO_ENCONTRADA);
    return valores.getFirst();
  }

  public Page<ProductoVenta> productos(UUID empresa, SearchPage pagina) {
    var patron = SqlPatterns.contains(pagina.query());
    var total =
        jdbc.queryForObject(
            "SELECT count(*) FROM facturacion_productos WHERE empresa_id=? AND (codigo ILIKE ?"
                + " ESCAPE '\\' OR nombre ILIKE ? ESCAPE '\\')",
            Long.class,
            empresa,
            patron,
            patron);
    var elementos =
        jdbc.query(
            "SELECT contenido::text FROM facturacion_productos WHERE empresa_id=? AND (codigo ILIKE"
                + " ? ESCAPE '\\' OR nombre ILIKE ? ESCAPE '\\') ORDER BY codigo,id LIMIT ? OFFSET"
                + " ?",
            (rs, n) -> leer(rs.getString(1), ProductoVenta.class),
            empresa,
            patron,
            patron,
            pagina.size(),
            pagina.page() * pagina.size());
    return new Page<>(elementos, total, pagina.page(), pagina.size());
  }

  public ProductoVenta producto(UUID empresa, UUID id) {
    return unico(
        jdbc.query(
            "SELECT contenido::text FROM facturacion_productos WHERE empresa_id=? AND id=?",
            (rs, n) -> leer(rs.getString(1), ProductoVenta.class),
            empresa,
            id));
  }

  public ProductoVenta guardarProducto(ProductoVenta producto, boolean nuevo) {
    try {
      if (nuevo)
        jdbc.update(
            "INSERT INTO"
                + " facturacion_productos(id,empresa_id,codigo,nombre,activo,version,contenido)"
                + " VALUES (?,?,?,?,?,?,?::jsonb)",
            producto.id(),
            producto.empresaId(),
            producto.codigo(),
            producto.nombre(),
            producto.activo(),
            producto.version(),
            escribir(producto));
      else
        jdbc.update(
            "UPDATE facturacion_productos SET"
                + " codigo=?,nombre=?,activo=?,version=?,contenido=?::jsonb WHERE empresa_id=? AND"
                + " id=?",
            producto.codigo(),
            producto.nombre(),
            producto.activo(),
            producto.version(),
            escribir(producto),
            producto.empresaId(),
            producto.id());
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(PRODUCTO_DUPLICADO);
    }
    return producto;
  }

  private DocumentoComercial.Cliente mapearCliente(ResultSet rs) throws SQLException {
    return new DocumentoComercial.Cliente(
        rs.getObject("id", UUID.class),
        rs.getString("document_type"),
        rs.getString("document_number"),
        rs.getString("verification_digit"),
        rs.getString("name"),
        rs.getString("address"),
        rs.getString("email"));
  }

  public List<DocumentoComercial.Cliente> clientes(UUID empresa, String busqueda) {
    var patron = SqlPatterns.contains(busqueda);
    return jdbc.query(
        "SELECT * FROM third_parties WHERE company_id=? AND active AND category IN"
            + " ('CUSTOMER','BOTH') AND (name ILIKE ? ESCAPE '\\' OR document_number ILIKE ? ESCAPE"
            + " '\\') ORDER BY name,id LIMIT 50",
        (rs, n) -> mapearCliente(rs),
        empresa,
        patron,
        patron);
  }

  public DocumentoComercial.Cliente cliente(UUID empresa, UUID id) {
    var valores =
        jdbc.query(
            "SELECT * FROM third_parties WHERE company_id=? AND id=? AND active AND category IN"
                + " ('CUSTOMER','BOTH')",
            (rs, n) -> mapearCliente(rs),
            empresa,
            id);
    if (valores.isEmpty()) throw new BusinessException(REFERENCIA_FACTURACION_INVALIDA);
    return valores.getFirst();
  }

  public DocumentoComercial.Emisor emisor(UUID empresa) {
    return jdbc.queryForObject(
        "SELECT legal_name,nit,verification_digit FROM companies WHERE id=?",
        (rs, n) -> new DocumentoComercial.Emisor(rs.getString(1), rs.getString(2), rs.getString(3)),
        empresa);
  }

  private static final String CUENTAS =
      "FROM chart_accounts a WHERE a.company_id=? AND a.active AND a.allows_posting AND NOT"
          + " EXISTS(SELECT 1 FROM chart_accounts h WHERE h.company_id=a.company_id AND"
          + " h.parent_code=a.code)";

  public List<CasosUsoFacturacion.Cuenta> cuentas(UUID empresa, String busqueda) {
    var patron = SqlPatterns.contains(busqueda);
    return jdbc.query(
        "SELECT a.id,a.code,a.name "
            + CUENTAS
            + " AND (a.code ILIKE ? ESCAPE '\\' OR a.name ILIKE ? ESCAPE '\\') ORDER BY a.code"
            + " LIMIT 50",
        (rs, n) ->
            new CasosUsoFacturacion.Cuenta(
                rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)),
        empresa,
        patron,
        patron);
  }

  public void validarCuentas(UUID empresa, ConfiguracionFacturacion configuracion) {
    reservarPrefijo(empresa, DocumentoComercial.Tipo.FACTURA, configuracion.prefijoFactura());
    reservarPrefijo(empresa, DocumentoComercial.Tipo.NOTA_CREDITO, configuracion.prefijoCredito());
    reservarPrefijo(empresa, DocumentoComercial.Tipo.NOTA_DEBITO, configuracion.prefijoDebito());
    if (!configuracion.contabilizar()) return;
    for (var id :
        List.of(
            configuracion.cuentaCartera(),
            configuracion.cuentaIngreso(),
            configuracion.cuentaIva(),
            configuracion.cuentaRecaudo()))
      if (jdbc.queryForObject("SELECT count(*) " + CUENTAS + " AND a.id=?", Long.class, empresa, id)
          != 1) throw new BusinessException(CONFIGURACION_FACTURACION_INVALIDA);
  }

  private void reservarPrefijo(UUID empresa, DocumentoComercial.Tipo tipo, String prefijo) {
    jdbc.update(
        "INSERT INTO facturacion_prefijos(empresa_id,prefijo,tipo) VALUES (?,?,?) ON"
            + " CONFLICT(empresa_id,prefijo) DO NOTHING",
        empresa,
        prefijo,
        tipo.name());
    var reservado =
        jdbc.queryForObject(
            "SELECT tipo FROM facturacion_prefijos WHERE empresa_id=? AND prefijo=?",
            String.class,
            empresa,
            prefijo);
    if (!tipo.name().equals(reservado))
      throw new BusinessException(CONFIGURACION_FACTURACION_INVALIDA);
  }

  public ConfiguracionFacturacion configuracion(UUID empresa) {
    var valores =
        jdbc.query(
            "SELECT contenido::text FROM facturacion_configuracion WHERE empresa_id=?",
            (rs, n) -> leer(rs.getString(1), ConfiguracionFacturacion.class),
            empresa);
    return valores.isEmpty() ? ConfiguracionFacturacion.inicial() : valores.getFirst();
  }

  public ConfiguracionFacturacion configurar(UUID empresa, ConfiguracionFacturacion configuracion) {
    jdbc.update(
        "INSERT INTO facturacion_configuracion(empresa_id,contenido) VALUES (?,?::jsonb) ON"
            + " CONFLICT(empresa_id) DO UPDATE SET contenido=EXCLUDED.contenido",
        empresa,
        escribir(configuracion));
    return configuracion;
  }

  private static final String BASE_RESUMEN =
      """
      FROM facturacion_documentos d LEFT JOIN LATERAL (
       SELECT coalesce(sum(importe),0) AS recaudado FROM facturacion_pagos p
        WHERE p.empresa_id=d.empresa_id AND p.documento_id=d.id AND NOT p.anulado
      ) p ON true
      """;
  private static final String SALDO =
      "CASE WHEN d.estado='CONFIRMADO' AND d.tipo<>'NOTA_CREDITO' THEN d.total-p.recaudado ELSE 0"
          + " END";

  public Page<CasosUsoFacturacion.Resumen> documentos(
      UUID empresa,
      SearchPage pagina,
      String estado,
      String tipo,
      LocalDate desde,
      LocalDate hasta) {
    var filtros =
        new StringBuilder(
            " WHERE d.empresa_id=? AND (coalesce(d.numero,'Borrador') ILIKE ? ESCAPE '\\' OR"
                + " d.contenido->'cliente'->>'nombre' ILIKE ? ESCAPE '\\')");
    var args =
        new ArrayList<Object>(
            List.of(
                empresa,
                SqlPatterns.contains(pagina.query()),
                SqlPatterns.contains(pagina.query())));
    if (estado != null && !estado.isBlank()) {
      filtros.append(" AND d.estado=?");
      args.add(estado);
    }
    if ("VENTAS".equals(tipo)) {
      filtros.append(" AND d.tipo<>'NOTA_CREDITO'");
    } else if (tipo != null && !tipo.isBlank()) {
      filtros.append(" AND d.tipo=?");
      args.add(tipo);
    }
    if (desde != null) {
      filtros.append(" AND d.fecha>=?");
      args.add(desde);
    }
    if (hasta != null) {
      filtros.append(" AND d.fecha<=?");
      args.add(hasta);
    }
    var total =
        jdbc.queryForObject(
            "SELECT count(*) FROM facturacion_documentos d" + filtros, Long.class, args.toArray());
    args.add(pagina.size());
    args.add(pagina.page() * pagina.size());
    var elementos =
        jdbc.query(
            "SELECT d.*,p.recaudado,"
                + SALDO
                + " AS saldo "
                + BASE_RESUMEN
                + filtros
                + " ORDER BY d.fecha DESC,d.id LIMIT ? OFFSET ?",
            (rs, n) ->
                new CasosUsoFacturacion.Resumen(
                    rs.getObject("id", UUID.class),
                    DocumentoComercial.Tipo.valueOf(rs.getString("tipo")),
                    rs.getString("numero"),
                    rs.getObject("fecha", LocalDate.class),
                    rs.getObject("vencimiento", LocalDate.class),
                    leer(rs.getString("contenido"), DocumentoComercial.class).cliente().nombre(),
                    DocumentoComercial.Estado.valueOf(rs.getString("estado")),
                    rs.getBigDecimal("total").toPlainString(),
                    rs.getBigDecimal("recaudado").setScale(2).toPlainString(),
                    rs.getBigDecimal("saldo").setScale(2).toPlainString(),
                    rs.getLong("version")),
            args.toArray());
    return new Page<>(elementos, total, pagina.page(), pagina.size());
  }

  public DocumentoComercial documento(UUID empresa, UUID id) {
    return unico(
        jdbc.query(
            "SELECT contenido::text FROM facturacion_documentos WHERE empresa_id=? AND id=?",
            (rs, n) -> leer(rs.getString(1), DocumentoComercial.class),
            empresa,
            id));
  }

  public void guardarDocumento(DocumentoComercial documento, boolean nuevo) {
    if (nuevo)
      jdbc.update(
          "INSERT INTO"
              + " facturacion_documentos(id,empresa_id,cliente_id,origen_id,tipo,estado,numero,fecha,vencimiento,total,iva,version,contenido)"
              + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb)",
          documento.id(),
          documento.empresaId(),
          documento.clienteId(),
          documento.documentoOrigenId(),
          documento.tipo().name(),
          documento.estado().name(),
          documento.numero(),
          documento.fecha(),
          documento.vencimiento(),
          new java.math.BigDecimal(documento.totales().total()),
          new java.math.BigDecimal(documento.totales().iva()),
          documento.version(),
          escribir(documento));
    else
      jdbc.update(
          "UPDATE facturacion_documentos SET"
              + " cliente_id=?,origen_id=?,tipo=?,estado=?,numero=?,fecha=?,vencimiento=?,total=?,iva=?,version=?,contenido=?::jsonb"
              + " WHERE empresa_id=? AND id=?",
          documento.clienteId(),
          documento.documentoOrigenId(),
          documento.tipo().name(),
          documento.estado().name(),
          documento.numero(),
          documento.fecha(),
          documento.vencimiento(),
          new java.math.BigDecimal(documento.totales().total()),
          new java.math.BigDecimal(documento.totales().iva()),
          documento.version(),
          escribir(documento),
          documento.empresaId(),
          documento.id());
    // Las referencias solo se sustituyen mientras el documento es borrador o en su inserción.
    if (nuevo || documento.estado() == DocumentoComercial.Estado.BORRADOR) {
      jdbc.update(
          "DELETE FROM facturacion_referencias WHERE empresa_id=? AND documento_id=?",
          documento.empresaId(),
          documento.id());
      for (var producto :
          documento.lineas().stream().map(CalculadoraVentas.Linea::productoId).distinct().toList())
        jdbc.update(
            "INSERT INTO facturacion_referencias(empresa_id,documento_id,producto_id) VALUES"
                + " (?,?,?)",
            documento.empresaId(),
            documento.id(),
            producto);
    }
  }

  public DocumentoComercial porClave(UUID empresa, String clave, String huella) {
    var valores =
        jdbc.query(
            "SELECT documento_id,huella FROM facturacion_solicitudes WHERE empresa_id=? AND"
                + " clave=?",
            (rs, n) -> Map.entry(rs.getObject(1, UUID.class), rs.getString(2)),
            empresa,
            clave);
    if (valores.isEmpty()) return null;
    if (!valores.getFirst().getValue().equals(huella))
      throw new BusinessException(IDEMPOTENCY_CONFLICT);
    return documento(empresa, valores.getFirst().getKey());
  }

  public void registrarClave(UUID empresa, String clave, String huella, UUID documento) {
    jdbc.update(
        "INSERT INTO facturacion_solicitudes(empresa_id,clave,huella,documento_id) VALUES"
            + " (?,?,?,?)",
        empresa,
        clave,
        huella,
        documento);
  }

  public String consecutivo(UUID empresa, DocumentoComercial.Tipo tipo, int anio, String prefijo) {
    var numero =
        jdbc.queryForObject(
            "INSERT INTO facturacion_consecutivos(empresa_id,tipo,anio,ultimo) VALUES (?,?,?,1) ON"
                + " CONFLICT(empresa_id,tipo,anio) DO UPDATE SET"
                + " ultimo=facturacion_consecutivos.ultimo+1 RETURNING ultimo",
            Long.class,
            empresa,
            tipo.name(),
            anio);
    return "%s-%04d-%06d".formatted(prefijo, anio, numero);
  }

  public List<PagoVenta> pagos(UUID empresa, UUID documento) {
    return jdbc.query(
        "SELECT contenido::text FROM facturacion_pagos WHERE empresa_id=? AND documento_id=? ORDER"
            + " BY creado_en,id",
        (rs, n) -> leer(rs.getString(1), PagoVenta.class),
        empresa,
        documento);
  }

  public PagoVenta pagoPorClave(UUID empresa, String clave, String huella) {
    var valores =
        jdbc.query(
            "SELECT contenido::text,huella FROM facturacion_pagos WHERE empresa_id=? AND clave=?",
            (rs, n) -> Map.entry(leer(rs.getString(1), PagoVenta.class), rs.getString(2)),
            empresa,
            clave);
    if (valores.isEmpty()) return null;
    if (!valores.getFirst().getValue().equals(huella))
      throw new BusinessException(IDEMPOTENCY_CONFLICT);
    return valores.getFirst().getKey();
  }

  public void guardarPago(PagoVenta pago, String clave, String huella, boolean nuevo) {
    if (nuevo)
      jdbc.update(
          "INSERT INTO"
              + " facturacion_pagos(id,empresa_id,documento_id,fecha,importe,anulado,clave,huella,creado_en,contenido)"
              + " VALUES (?,?,?,?,?,?,?,?,?,?::jsonb)",
          pago.id(),
          pago.empresaId(),
          pago.documentoId(),
          pago.fecha(),
          new java.math.BigDecimal(pago.importe()),
          pago.anulado(),
          clave,
          huella,
          java.sql.Timestamp.from(pago.creadoEn()),
          escribir(pago));
    else
      jdbc.update(
          "UPDATE facturacion_pagos SET anulado=?,contenido=?::jsonb WHERE empresa_id=? AND id=?",
          pago.anulado(),
          escribir(pago),
          pago.empresaId(),
          pago.id());
  }

  public void auditar(Actor actor, UUID empresa, UUID entidad, String operacion) {
    jdbc.update(
        "INSERT INTO audit(id,company_id,issuer,subject,operation,entity_id,occurred_at) VALUES"
            + " (?,?,?,?,?,?,current_timestamp)",
        UUID.randomUUID(),
        empresa,
        actor.issuer(),
        actor.subject(),
        operacion,
        entidad);
  }

  public CasosUsoFacturacion.Cartera cartera(UUID empresa, LocalDate corte) {
    return jdbc.queryForObject(
        "SELECT coalesce(sum(d.total),0) AS ventas,coalesce(sum(d.iva),0) AS"
            + " iva,coalesce(sum(p.recaudado),0) AS recaudado,coalesce(sum(d.total-p.recaudado),0)"
            + " AS pendiente,coalesce(sum(CASE WHEN d.vencimiento<? THEN d.total-p.recaudado ELSE 0"
            + " END),0) AS vencido,count(*) FILTER (WHERE d.total>p.recaudado) AS cantidad "
            + BASE_RESUMEN
            + " WHERE d.empresa_id=? AND d.estado='CONFIRMADO' AND d.tipo<>'NOTA_CREDITO'",
        (rs, n) ->
            new CasosUsoFacturacion.Cartera(
                rs.getBigDecimal("ventas").setScale(2).toPlainString(),
                rs.getBigDecimal("iva").setScale(2).toPlainString(),
                rs.getBigDecimal("recaudado").setScale(2).toPlainString(),
                rs.getBigDecimal("pendiente").setScale(2).toPlainString(),
                rs.getBigDecimal("vencido").setScale(2).toPlainString(),
                rs.getLong("cantidad")),
        corte,
        empresa);
  }
}
