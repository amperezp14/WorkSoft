# Facturación comercial interna

El módulo se abre en Empresa → Facturación. Persiste exclusivamente en PostgreSQL,
sin Keycloak y sin respaldo de datos en memoria. Usa el mismo arranque de
[Java, Angular y PostgreSQL](run-postgres-accounting.md).

## Actualizar

Desde la raíz, `git pull origin develop`. Reiniciar Java; Flyway aplica
`V5__modulo_facturacion.sql` y conserva los datos anteriores. No borrar volúmenes
ni modificar migraciones ya aplicadas. Reiniciar Angular con `npm start` desde
`frontend/worksoft-web`. No hay nuevas credenciales ni servicios requeridos.

## Flujo de uso

1. **Clientes:** crear o editar el tercero. Para seleccionarlo en ventas debe
   estar activo y tener categoría Cliente o Cliente y proveedor. Es el mismo
   catálogo de Contabilidad; esta pantalla también permite consultar otros terceros.
2. **Productos y servicios:** registrar código único por empresa, nombre, tipo,
   unidad y precio antes de impuestos. Seleccionar explícitamente tratamiento y
   tarifa de IVA. Exento y excluido se mantienen diferenciados y usan tarifa cero.
   Los conceptos pueden desactivarse sin borrar su historial. No gestionan existencias.
3. **Configuración:** definir prefijos internos distintos para ventas, notas crédito
   y notas débito, y condiciones del documento. Los consecutivos son por empresa,
   tipo y año; cambiar el prefijo no reinicia el consecutivo. Un prefijo reservado
   a un tipo no puede reasignarse a otro, para evitar colisiones históricas.
4. **Ventas → Nueva venta:** elegir cliente, fecha, vencimiento, elaborado por y
   conceptos. Se admiten hasta 100 líneas, cantidades con tres decimales, precios
   y porcentajes con dos. El precio puede ajustarse por venta. Usar punto o coma
   decimal sin separadores de miles. Descuento porcentual por línea, antes de IVA.
5. **Guardar borrador:** el servidor calcula importes usando los datos vigentes del
   catálogo al guardar. Se puede editar y volver a guardar. No tiene consecutivo.
6. **Confirmar documento interno:** exige total positivo y cambios guardados.
   Asigna número, conserva los datos del emisor y cliente, productos, tarifas,
   condiciones y cuentas; sus importes quedan protegidos contra modificación.
7. **Recaudos:** registrar fecha, medio, importe, referencia y elaborado por.
   Permite pagos parciales o totales y rechaza sobrepagos. Registrar no ejecuta
   cobros bancarios. Anular conserva el recaudo original, motivo, fecha y autor
   declarado; restablece el saldo sin ejecutar una devolución bancaria.
8. **Notas débito:** abrir una venta confirmada y elegir Crear nota débito.
   El documento suplementario conserva el cliente y la referencia al original;
   tiene sus propios conceptos, consecutivo, saldo y recaudos. No modifica el
   original. Debe guardarse y confirmarse como cualquier venta interna.
9. **Notas crédito:** abrir un documento confirmado sin recaudos activos y elegir
   Revertir total con nota crédito. Si hay recaudos, anularlos primero. La nota
   conserva los importes históricos y anula el original sin eliminarlo. Este flujo
   es de reversión total; no admite créditos parciales ni saldos a favor/reembolsos.
   Revertir una venta no revierte automáticamente sus notas débito independientes.
10. **Cartera:** consultar ventas y notas débito confirmadas, recaudado, saldo y
    vencidos. El resumen abarca toda la empresa y la búsqueda filtra el listado.
    La fecha de referencia clasifica vencimientos; no reconstruye saldos históricos.
11. **Imprimir / PDF:** abrir un documento guardado y usar el diálogo del navegador.
    La impresión identifica borradores, anulados y el carácter interno del documento.

Las búsquedas de clientes, conceptos y cuentas ofrecen hasta 50 opciones; filtrar
por nombre, código o identificación para encontrar otras. Los listados principales
paginan de 25 en 25. Actualizar ante conflictos de versión antes de volver a editar.

## Contabilización opcional

Está desactivada inicialmente para permitir registrar ventas sin configurar el plan
contable. Para activarla se requieren cuatro cuentas distintas, activas, sin hijas y
aptas para movimientos: cartera, ingresos, IVA generado y caja/bancos para recaudos.
Crear auxiliares o habilitar cuentas apropiadas en el plan de cuentas. Su elección
corresponde al marco de cada empresa; no se asignan códigos PUC automáticamente.

- Venta o nota débito: débito a cartera; créditos a ingresos e IVA, si lo hay.
- Recaudo: débito a caja/bancos y crédito a cartera.
- Nota crédito: reversión del asiento del documento original.
- Anulación del recaudo: reversión de su asiento.

Documento y comprobante se guardan en una sola transacción: si falla la
contabilización no queda una venta o pago confirmado a medias ni se consume su
consecutivo. Consultar el comprobante desde el enlace del documento.
Los pagos y las reversiones usan las cuentas históricas del documento confirmado,
no las elegidas después. Estas cuentas y el tercero deben seguir habilitados para
contabilizar; reactivarlos si es necesario. Todos los medios de pago usan la única
cuenta de recaudo configurada, sin conciliación ni pasarela de pagos.

Activar contabilidad no contabiliza retroactivamente documentos anteriores. Si un
documento se confirmó con la opción desactivada, sus pagos y su nota crédito también
quedan sin asiento automático. La interfaz informa esta situación.

## Alcance colombiano

Esta entrega es un circuito comercial interno funcional. No sustituye la obligación
legal de facturar ni es emisión electrónica DIAN. No genera XML firmado, CUFE/CUDE,
resoluciones de numeración, transmisión, validación, contingencias ni entrega fiscal.
Tampoco calcula retenciones, ICA o responsabilidades tributarias; no implementa
inventarios, conciliación bancaria, créditos parciales, devoluciones de dinero,
compras o cierre fiscal. Elaborado por y autor de anulación son datos declarados,
no firmas autenticadas. La autenticación propia sigue pendiente.

Consultar [fuentes, cálculo y alcance normativo](facturacion/alcance-colombia.md).
La clasificación y vigencia tributaria de cada concepto requiere definición de la
empresa y revisión contable antes del uso operativo.

## Tablas

- `facturacion_productos`: conceptos, precios, impuestos y versión del catálogo.
- `facturacion_configuracion`: ajustes comerciales y cuentas por empresa.
- `facturacion_prefijos`: reserva histórica de prefijos por tipo de documento.
- `facturacion_consecutivos`: último número por empresa, tipo y año.
- `facturacion_documentos`: encabezados, importes para consulta y contenido histórico
  de líneas, emisor, cliente y configuración en JSONB.
- `facturacion_referencias`: referencias a productos de la misma empresa.
- `facturacion_solicitudes`: claves y huellas para evitar creación duplicada por reintentos.
- `facturacion_pagos`: recaudos, anulación trazable y comprobantes asociados.
- `audit.entity_id`: nuevo campo opcional para identificar la entidad de la operación.

## API

Base: `/api/v1/companies/{empresa}/facturacion`.

| Recurso | Operaciones |
|---|---|
| `/productos` | GET paginado; POST creación |
| `/productos/{id}` | PUT edición con versión |
| `/clientes?q=...` | GET clientes activos elegibles |
| `/cuentas?q=...` | GET cuentas aptas para contabilizar |
| `/configuracion` | GET y PUT con versión |
| `/documentos` | GET paginado; POST borrador con Idempotency-Key |
| `/documentos/{id}` | GET detalle; PUT borrador con versión |
| `/documentos/{id}/confirmar` | POST `{version}` |
| `/documentos/{id}/nota-credito` | POST `{fecha,motivo,elaboradoPor,version}` |
| `/documentos/{id}/pagos` | POST `{fecha,importe,medio,referencia,elaboradoPor,version}` con Idempotency-Key |
| `/documentos/{id}/pagos/{pago}/anular` | POST `{fecha,motivo,elaboradoPor,version}` |
| `/cartera?corte=YYYY-MM-DD` | GET saldos actuales y vencimientos |

Dinero y cantidades viajan como cadenas decimales. El listado admite `q`, `page`,
`size`, `tipo`, `estado`, `desde`, `hasta`; `tipo=VENTAS` agrupa ventas y notas débito.
Las claves idempotentes tienen entre 16 y 128 caracteres alfanuméricos, guion o guion
bajo. Reutilizarlas con otro contenido se rechaza. Confirmar de nuevo devuelve el
mismo documento confirmado. Una nota crédito total nunca duplica la reversión.
