# Comprobación de nómina

Fecha: 2026-11-17. No se crearon ni ejecutaron suites de pruebas ni e2e.

- Backend empaquetado con JDK 25 y Maven Wrapper, `-Dmaven.test.skip=true package`:
  BUILD SUCCESS. Angular `npm run build`: compilación de producción correcta.
- Arranque con PostgreSQL 17.6 temporal: migraciones V1–V6 aplicadas, health UP.
- Operación HTTP con datos ficticios: tercero, empleado, contrato ordinario mensual,
  vigencia revisada de ejemplo, periodo y novedades.
- Salario COP 2.000.000, bonificación salarial COP 100.000, auxilio COP 249.095,
  salud COP 84.000, pensión COP 84.000 y deducción declarada COP 50.000.
  Devengado COP 2.349.095, deducciones COP 218.000, neto COP 2.131.095.
- Cálculo, aprobación y registro de pago externo completados. La consulta posterior
  conservó empleado, contrato e importes de la liquidación.
- Servidor Angular accesible en la ruta del módulo y proxy API HTTP 200.

El entorno efímero fue eliminado. No se utilizaron datos reales ni base operativa.
No se verificaron todos los escenarios, concurrencia, impresión o interacción de
navegador. La revisión laboral y pensional y las pruebas integrales siguen pendientes.
