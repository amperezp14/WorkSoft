# Nómina: primera etapa mensual

Abrir una empresa → Nómina. Se conserva el arranque PostgreSQL sin Keycloak.
Actualizar con `git pull origin develop` y reiniciar Java: Flyway aplicará V6 sin
borrar datos anteriores. Iniciar Angular con `npm start` desde frontend/worksoft-web.

## Alcance de esta entrega

Liquidación interna del neto mensual de dependientes ordinarios del sector privado,
a jornada completa, con contrato que cubra todo el mes y salario no inferior al
mínimo de la vigencia. Moneda COP; hasta 100 contratos por periodo y 500 novedades.
Se permiten contratos indefinidos y a término fijo; registrar el tipo no valida sus
cláusulas, duración o requisitos legales.

No calcula prestaciones sociales, vacaciones, licencias, incapacidades, horas extra,
recargos, fracciones de mes, salario integral, aprendices, trabajadores pensionados,
aportes patronales, parafiscales, exoneraciones ni tributación automática. No incluye
teletrabajo/conectividad ni pagos no salariales distintos del auxilio de transporte
presencial modelado. No genera PILA, nómina electrónica DIAN, asientos contables ni
transferencias bancarias. Es una primera etapa funcional, no una nómina legal integral.

## Empezar

1. En Contabilidad → Terceros, registrar la persona natural con CC, CE o pasaporte.
   No usar NIT. La categoría del tercero no limita su vinculación como empleado.
2. Nómina → Empleados: buscar la persona, asignar código único, cargo y estado.
   La identificación se administra en Terceros; el vínculo con el empleado no cambia.
3. Contratos: elegir empleado, tipo, inicio/fin, salario ordinario mensual y si
   corresponde auxilio de transporte presencial. Los contratos de un empleado no
   pueden superponerse. El tipo fijo requiere fecha final. No se realiza una
   liquidación de retiro por registrar esa fecha.
4. Parámetros y vigencias: registrar intervalo, SMMLV, auxilio, tasas de salud y
   pensión del trabajador, fuentes y responsable de revisión. Se puede cargar una
   referencia de 2026 como ayuda; no se guarda ni se aplica automáticamente.
   Verificar la vigencia y el régimen pensional aplicable antes de marcar Revisado.
   Los intervalos no pueden superponerse y deben cubrir todo el mes a liquidar.
5. Liquidaciones → Nuevo periodo: año, mes, descripción, elaborado por y contratos.
   Los seleccionados se conservan al paginar o buscar; revisar el resumen de nombres.
   Declarar de 0 a 30 días de auxilio si el contrato lo reconoce; en otro caso usar 0.
   Esto no representa ausencias salariales: siempre se liquida el salario completo.
6. Guardar periodo. El encabezado y participantes quedan fijos; para reemplazarlos,
   anular el periodo y crear otro del mismo mes. Solo puede existir uno sin anular
   por empresa y mes. Contratos que no cubran todo el mes se rechazan al calcular.
7. Agregar novedades monetarias: bonificación salarial, comisión salarial,
   deducción autorizada o retención previamente determinada. Descripción e importe
   positivo y referencia de soporte son obligatorios. La referencia no adjunta el
   archivo ni valida automáticamente la procedencia o límites legales de la deducción.
8. Calcular / recalcular: revisar tabla y desprendibles. Cada cambio en novedades
   invalida el cálculo; los cambios de maestros también obligan a recalcular antes
   de aprobar. No se omiten participantes por errores: falla la operación completa.
9. Aprobar: congela importes, fuentes, persona, contrato, datos del emisor y novedades.
   No cambia documentos anteriores si se edita posteriormente un maestro.
10. Imprimir todos los desprendibles o uno por empleado, y guardar PDF mediante el
    diálogo del navegador. CALCULADA es provisional; el estado siempre se imprime.
11. Registrar como pagada solo después de realizar el pago externo completo: fecha
    desde el inicio del mes y referencia del soporte. Es una declaración administrativa
    del pago a todos los participantes, sin conexión bancaria ni pagos parciales.

Un periodo aprobado puede anularse con motivo, conservando su historial. Un periodo
pagado queda cerrado y no tiene flujo de reversión en esta entrega. Las correcciones
no se hacen modificando liquidaciones cerradas.

## Cálculos

- Salario completo del contrato + bonificaciones/comisiones = devengado salarial.
- Auxilio mensual × días declarados / 30, solo si el contrato lo reconoce y el
  devengado salarial no supera dos SMMLV de la vigencia. No forma parte del IBC.
- IBC salarial, limitado a 25 SMMLV. El salario base debe ser al menos un SMMLV.
- Salud y pensión del trabajador: IBC × tasas de la vigencia revisada.
- Solidaridad de la estrategia ORDINARIA_LEY100_V1: 0 % por debajo de 4 SMMLV;
  1 % desde 4 y antes de 16; 1,2 % de 16 hasta 17; 1,4 % por encima de 17 hasta
  18; 1,6 % por encima de 18 hasta 19; 1,8 % por encima de 19 hasta 20; 2 % por
  encima de 20. Se calcula sobre el IBC limitado, sin redondear primero la relación.
- Devengado = salarial + auxilio. Deducciones = salud + pensión + solidaridad +
  deducciones autorizadas + retención declarada. Neto = devengado − deducciones.
  Si es negativo, la liquidación se rechaza. Esto no valida los límites legales
  específicos de cada deducción, que deben revisarse antes de ingresarla.

BigDecimal en el dominio; importes en JSON como cadenas con dos decimales.
Redondeo HALF_UP por concepto a dos decimales, con suma de valores redondeados.
No aplica las reglas de redondeo y reporte propias de una planilla PILA.

La estrategia es explícita y conserva su identificación histórica; no determina
si una persona está sometida a otro régimen o excepción. Fuentes, fechas y alcance
registrados en [contexto colombiano del módulo](nomina/alcance-colombia.md).

## Persistencia y API

Tablas: nomina_empleados, nomina_contratos, nomina_parametros, nomina_periodos y
nomina_periodo_contratos. Se reutilizan third_parties, companies y audit.
Los periodos conservan instantáneas JSONB y referencias de contratos por empresa.
No se incorporan datos personales reales, afiliaciones ni cuentas bancarias de prueba.

Base HTTP: `/api/v1/companies/{empresa}/nomina`.

| Ruta | Operaciones |
|---|---|
| `/empleados` | GET paginado; POST |
| `/empleados/{id}` | PUT con versión |
| `/contratos` | GET paginado; POST |
| `/contratos/{id}` | PUT con versión |
| `/parametros` | GET; POST |
| `/parametros/{id}` | PUT con versión |
| `/periodos` | GET paginado; POST con UUID idempotente en el cuerpo |
| `/periodos/{id}` | GET detalle, totales y contratos seleccionados |
| `/periodos/{id}/novedades` | POST con UUID idempotente de novedad y versión del periodo |
| `/periodos/{id}/novedades/{novedad}?version=...` | DELETE en borrador/calculada; invalida cálculo |
| `/periodos/{id}/calcular` | POST `{version}` |
| `/periodos/{id}/aprobar` | POST `{version}` |
| `/periodos/{id}/anular` | POST `{version,motivo}` |
| `/periodos/{id}/pago` | POST `{version,fecha,referencia}` |

Listados con q/page/size (hasta 100), por defecto 25. Se rechazan versiones antiguas.
Reintentar creación o novedad con el mismo UUID y contenido devuelve lo existente;
reutilizar el UUID con otros datos se rechaza. Eliminar una novedad solo opera antes
de aprobar. La consulta del periodo usa instantáneas después del cálculo, conservadas
al aprobar; una recalculación explícita actualiza únicamente el periodo editable.
