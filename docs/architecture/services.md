# Límites iniciales de servicios

| Servicio | Datos propios | Responsabilidades y exclusiones |
| --- | --- | --- |
| Proveedor OIDC | Identidades y credenciales | Emite tokens; no gestiona inventario ni membresías empresariales de WorkSoft. |
| Empresas | Empresas, membresías, configuración | Aislamiento y permisos; no emite facturas fiscales. |
| Catálogo | Productos, unidades y clasificación | Define referencias; no mantiene existencias. |
| Inventario | Almacenes, reservas, movimientos | Mantiene existencias; referencias externas por ID. |
| Ventas | Clientes comerciales, ofertas y pedidos | Coordina pedidos; no genera documentos DIAN. |
| Facturación | Documentos fiscales y sus estados | Captura instantáneas necesarias y aplica reglas colombianas versionadas. |
| Pagos | Cobros y conciliaciones | No infiere aprobación fiscal de una factura. |
| Notificaciones | Entregas y preferencias aplicables | Consume eventos; minimiza información personal. |
| Informes | Proyecciones de lectura | Sin consultas SQL directas a otras bases; reconoce retraso de actualización. |

Empresas implementa creación y consulta autorizadas. Los demás son límites propuestos; nómina,
compras y contabilidad requieren análisis propio antes de crear servicios.
El gateway enruta y puede validar tokens, pero los servicios verifican identidad
y autorización. No se comparten esquemas JPA, tablas ni repositorios entre servicios.

## Comunicación

HTTP para respuestas inmediatas. Contratos bajo /api/v1; cambios incompatibles
requieren nueva versión y plan de transición. Errores application/problem+json
sin stacktrace, SQL, tokens ni información de otras empresas.

Eventos para hechos ya confirmados: CompanyCreated v1 es el primero definido.
Se escribe outbox en la misma transacción local que el negocio y se publica
después de confirmar. Entrega al menos una vez: event id estable en reintentos,
consumidores idempotentes, reintentos limitados y cola de errores cuando exista
broker. No se promete exactamente una vez ni orden global. No hay broker en este incremento.
Un cambio incompatible del payload crea otro tipo/versionado de evento.

No se crea una transacción distribuida para coordinar inventario, ventas y
facturación. Sus reservas, compensaciones y requisitos de consistencia se diseñan
con los flujos funcionales de esas fases, sin asumir que un evento basta para
resolver requisitos fiscales o financieros.
