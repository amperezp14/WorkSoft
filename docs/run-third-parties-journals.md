# Terceros y comprobantes contables

Requiere el backend y PostgreSQL de [la guía de arranque](run-postgres-accounting.md).
Al reiniciar Java, Flyway aplica V4 sin borrar los datos existentes. Iniciar Angular
con npm start desde frontend/worksoft-web.

## Terceros

Seleccionar empresa → Contabilidad → Terceros. Crear o editar identificación,
nombre o razón social, categoría, contacto y estado. NIT y dígito de verificación
se guardan por separado; las identificaciones conservan ceros iniciales.
La combinación tipo/número es única por empresa. La validación es estructural:
no consulta RUT, no verifica inscripción DIAN ni calcula el dígito de verificación.
Los datos opcionales de contacto deben limitarse a los necesarios para la operación.
No cargar datos personales reales en ambientes compartidos de pruebas.

## Comprobantes

1. En Plan de cuentas, habilitar Permite movimientos en cuentas activas sin hijas,
   o crear auxiliares. La referencia inicial no recibe movimientos por defecto.
2. Crear un tercero activo.
3. Entrar a Contabilidad → Comprobantes → Nuevo comprobante.
4. Completar fecha, tipo, origen, descripción, referencia de soporte y elaborado por.
5. Agregar líneas: cuenta, tercero, descripción y un débito o un crédito por línea.
   Buscar cuentas/terceros cuando no estén entre las primeras 50 opciones.
6. Guardar borrador. Puede estar desbalanceado; permite editarlo posteriormente.
7. Para contabilizar, guardar todos los cambios y equilibrar débitos y créditos.
   Se requieren al menos dos líneas y un total positivo. Contabilizar vuelve a
   validar los terceros y las cuentas contra PostgreSQL.
8. El comprobante contabilizado recibe un consecutivo por empresa y año, por
   ejemplo CC-2026-000001, y queda de solo lectura.
9. Abrirlo e imprimir o guardar PDF con el diálogo del navegador. El borrador
   se identifica como tal y no tiene consecutivo contable.

Los importes se expresan en COP con hasta dos decimales; no se redondean
silenciosamente. El formulario admite punto o coma decimal, sin separadores de
miles. Los registros persisten al recargar. Una edición concurrente rechaza la
versión anterior y obliga a volver a consultar el registro.

## Alcance

Se conservan los datos de empresa, cuenta y tercero usados al contabilizar para
que posteriores cambios del catálogo no alteren el documento histórico. No se
puede modificar ni eliminar un comprobante contabilizado. Todavía no existe un
flujo para reversión o corrección mediante nuevos asientos, cierres de periodos,
saldos iniciales, libros o estados financieros.

La referencia de soporte es texto: no adjunta ni archiva documentos. Elaborado por
es un dato declarado en un espacio local sin login, no una firma autenticada.
El consecutivo del comprobante no corresponde a numeración de factura DIAN.
Este incremento no emite facturas electrónicas ni calcula impuestos y no representa
una certificación de cumplimiento contable integral.

## Tablas nuevas

- third_parties: terceros por empresa, identificación, contacto, estado y versión.
- journal_vouchers: encabezado, estado, totales, versión, consecutivo y datos históricos.
- journal_lines: cuentas y terceros por línea, débitos/créditos y datos históricos.
- journal_sequences: último consecutivo por empresa y año.

La contabilización agrega un evento a audit y outbox en la misma
transacción. La publicación de outbox a otros servicios sigue pendiente.
