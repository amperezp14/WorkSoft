# ADR 0008: facturación comercial interna

Fecha: 2026-11-17. Estado: implementado.

## Decisión

Mantener el módulo junto a empresas y contabilidad para preservar el arranque
sencillo y las transacciones atómicas en PostgreSQL. La separación hexagonal se
aplica por responsabilidades; no se añaden servicios externos de autenticación.

- Dominio en `domain/facturacion`: productos, configuración, cálculo decimal,
  documentos, recaudos e invariantes. Sin Spring, JDBC ni Jackson.
- Entrada `CasosUsoFacturacion`: contratos del módulo; `ServicioFacturacion`
  coordina acceso por empresa y operaciones mediante puertos.
- Salidas `AlmacenFacturacion` y `ContabilidadFacturacion`: unidad de trabajo,
  almacenamiento y generación de comprobantes. Sin dependencias de frameworks.
- Adaptadores: `ControladorFacturacion` HTTP, `PostgresAlmacenFacturacion` JDBC/JSONB
  y `AdaptadorContabilidadFacturacion`, que reutiliza los casos de uso contables.
- Composición Spring en `ConfiguracionModuloFacturacion`.

Los nombres nuevos son mayoritariamente españoles; se reutilizan Actor, Page,
CompanyAccess y demás contratos existentes sin renombrar módulos anteriores.

## Persistencia e integridad

El puerto de almacenamiento ofrece una transacción por empresa y bloquea su fila,
compartida con cuentas y terceros. Las operaciones contables participan en la misma
transacción REQUIRED. Se valida la versión antes de editar; las claves idempotentes
usan SHA-256 de campos con longitud explícita para evitar ambigüedad. Las referencias
compuestas impiden mezclar empresas. No hay borrado de documentos confirmados ni
recaudos; sus cambios se expresan mediante nuevos documentos o anulaciones trazables.

Encabezados indexados y columnas numéricas sirven a los listados y cartera. JSONB
conserva la instantánea completa de líneas, emisor, cliente y configuración; Jackson
se usa solo en el adaptador. Los totales se recalculan en dominio y se contrastan al
reconstruir documentos. Triggers protegen documentos confirmados y recaudos de cambios
no permitidos. Los prefijos reservados no se reasignan a otros tipos. Los consecutivos
se asignan transaccionalmente al confirmar, nunca al guardar el borrador.

El catálogo no modifica documentos confirmados. Pagos y notas crédito toman la
configuración contable histórica, evitando invertir una venta contra cuentas
seleccionadas posteriormente. La nota débito es suplementaria y genera cartera
independiente. La nota crédito implementada es exclusivamente de reversión total.

Se registran operaciones en audit con entity_id. Los comprobantes contabilizados
mantienen su auditoría y outbox preexistentes; no se introduce un publicador de eventos.

## Cálculo y alcance

COP, fecha de negocio Bogotá, cantidades hasta tres decimales y dinero con dos.
BigDecimal en Java y BigInt en el frontend. Redondeo HALF_UP por línea de bruto,
descuento e IVA; suma de importes redondeados. El servidor es la autoridad de cálculo.

La configuración fiscal es declarada por la empresa; no se infiere régimen ni se
presenta una tarifa como universal. Fuentes y vigencia consultada registradas en
[alcance colombiano](../facturacion/alcance-colombia.md). No es emisión electrónica
DIAN ni certificación de cumplimiento. Integración fiscal, créditos parciales,
retenciones, inventarios y autenticación quedan fuera de este incremento.

## Verificación acordada

Por instrucción del usuario, no añadir ni ejecutar suites de pruebas unitarias o e2e.
Compilar ambos proyectos y comprobar arranque y operación básica contra PostgreSQL
con datos sintéticos. Esto no sustituye una validación integral antes de producción.
