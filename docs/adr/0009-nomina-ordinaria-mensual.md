# ADR 0009: primera etapa de nómina ordinaria mensual

Fecha: 2026-11-17. Estado: implementado.

Se agrega un módulo al servicio existente para reutilizar empresa, terceros,
transacciones y auditoría. No se introduce un proveedor externo de identidad ni
persistencia en memoria. El código nuevo usa nombres en español.

## Separación hexagonal

- Dominio: EmpleadoNomina, ContratoNomina, ParametrosNomina, NovedadNomina,
  PeriodoNomina, LiquidacionNomina y MotorNominaOrdinaria, sin Spring/JDBC/Jackson.
- CasosUsoNomina define la entrada; ServicioNomina coordina autorización por empresa,
  consultas de terceros, versiones y transacciones mediante AlmacenNomina.
- ControladorNomina traduce HTTP. PostgresAlmacenNomina implementa el puerto con
  JDBC, JSONB y TransactionTemplate. ConfiguracionModuloNomina conecta dependencias.

Las referencias compuestas mantienen aislamiento por empresa. El bloqueo de fila de
empresa es compartido con los otros módulos y serializa cambios de maestros y cierre.
El periodo permite hasta 100 contratos, uno por empleado. Contratos y vigencias no
se superponen según validación transaccional; una restricción única impide periodos
mensuales simultáneos sin anular. UUID de solicitud y de novedad controlan reintentos.

## Estados y conservación

BORRADOR → CALCULADA → APROBADA → PAGADA. Las novedades invalidan CALCULADA y
vuelven a BORRADOR. Antes de aprobar se reconstruye el cálculo y se compara con la
instantánea: cambios de terceros, empleados, contratos, parámetros o emisor exigen
recalcular. Las versiones previenen pérdida de ediciones.

Anular conserva el contenido y libera el mes para otro periodo. No se permite
anular una nómina PAGADA en esta etapa. Triggers impiden borrar periodos y modificar
cálculos aprobados, permitiendo únicamente transiciones trazables a PAGADA/ANULADA.
Las referencias a contratos permanecen aunque se modifiquen sus condiciones.

El motor utiliza BigDecimal, porcentajes explícitos, redondeo por concepto y una
estrategia identificada. No infiere la aplicabilidad normativa: exige parámetros
revisados que cubran el mes completo y rechaza los casos que no modela.
[Fuentes y alcance](../nomina/alcance-colombia.md) se registraron antes de implementar.

## Límites deliberados

Es cálculo del neto interno del dependiente ordinario, no motor laboral integral.
No se implementan aportes patronales, prestaciones, PILA, DIAN, conciliación bancaria
ni contabilización automática. Los soportes y nombres de responsables son referencias
declaradas en un espacio local sin autenticación, no firmas verificadas.
Conservar esta distinción en formularios y desprendibles.

Se mantiene la instrucción del usuario de omitir suites de pruebas y e2e en esta
etapa; comprobar compilación, arranque y una operación sintética contra PostgreSQL.
