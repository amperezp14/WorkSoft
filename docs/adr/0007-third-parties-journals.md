# ADR 0007: terceros y comprobantes contables

Fecha: 2026-11-16. Estado: implementado para el espacio local PostgreSQL.

## Decisión

Mantener el incremento dentro de company-service para compartir transacciones e
integridad por empresa. Separar dominio, casos de uso, puertos y adaptadores;
no introducir dependencias Spring, JDBC ni Angular en dominio/aplicación.
ThirdPartyUseCases y JournalUseCases son puertos de entrada; ThirdPartyStore y
JournalStore encapsulan la persistencia. Los controladores traducen DTO y errores;
los adaptadores PostgreSQL implementan transacciones y resolución de referencias.

El dominio valida identificación, importes exactos, líneas, equilibrio, referencias
aptas para contabilizar y transiciones de borrador. Las cantidades viajan en JSON
como cadenas decimales, usan BigDecimal en Java y centavos BigInt en el frontend.
No se usa punto flotante para dinero.

## Integridad

Cada referencia incluye empresa y UUID; las claves foráneas compuestas impiden
mezclar empresas. Las versiones previenen pérdida de actualizaciones. Crear un
borrador requiere Idempotency-Key; reutilizarlo con otro contenido se rechaza.
Repetir la contabilización devuelve el documento contabilizado sin otro número.

La transacción de contabilización bloquea la empresa, valida versiones y maestros,
congela sus datos, asigna consecutivo anual, actualiza el encabezado e inserta
líneas históricas, auditoría y outbox. Un fallo revierte también el consecutivo.
El bloqueo de empresa se comparte con modificaciones de cuentas y terceros.
La lectura del encabezado y líneas es transaccional para evitar estados mezclados.

Restricciones SQL verifican importes, referencias y estados. Triggers protegen de
edición/borrado los comprobantes contabilizados y verifican al confirmar que las
líneas coincidan con los totales balanceados del encabezado.

## Normativa y límites

Consulta: 2026-11-16. Aplicación: contabilidad mercantil colombiana, sujeta al marco
que corresponda a cada entidad. Fuentes oficiales consultadas:

- [Código de Comercio, artículo 53](https://gestornormativo.creg.gov.co/gestor/entorno/docs/codigo_comercio.htm):
  identificación, fecha, origen, descripción, cuantía y cuentas del comprobante.
- [Supersociedades, libros de contabilidad](https://www.supersociedades.gov.co/en/web/nuestra-entidad/cap-1-libros-de-contabilidad):
  partida doble, conservación e integridad, referencia al Anexo 6 del DUR 2420 de
  2015 y correcciones mediante nuevos asientos.
- [Contexto colombiano](../architecture/colombia.md): tratamiento de datos personales.

Se toma esta regulación como referencia de diseño vigente a la consulta; el
módulo no certifica cumplimiento integral. La revisión profesional contable de su
uso operativo está pendiente. No hay archivo de soportes, firma autenticada,
reversiones, cierre de periodos ni emisión fiscal. Estos límites están visibles en
la guía de uso. La autenticación propia sigue pendiente; se conserva el arranque
local sin Keycloak solicitado por el usuario.
