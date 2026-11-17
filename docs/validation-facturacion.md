# Comprobación de facturación

Fecha: 2026-11-17. Por instrucción del usuario no se crearon ni ejecutaron suites
unitarias, de integración ni e2e. No se realizó un recorrido de navegador.

Verificaciones realizadas:

- Backend compilado y empaquetado con JDK 25 y Maven Wrapper mediante
  `./mvnw -Dmaven.test.skip=true package`: BUILD SUCCESS.
- Angular mediante `npm run build`: compilación de producción correcta.
- Arranque del backend sobre PostgreSQL 17.6 efímero, con datos sintéticos y
  credenciales temporales; migraciones V1 a V5 aplicadas y health UP.
- Operación básica por HTTP: empresa, cliente, concepto, configuración contable,
  borrador con 2,500 unidades a COP 10.000,00, descuento del 10 % e IVA declarado
  del 19 %. Total calculado: COP 26.775,00.
- Confirmación INTFV-2026-000001 con comprobante contable; recaudo de COP 10.000,00,
  saldo COP 16.775,00; anulación trazable y nota crédito INTNC-2026-000001.
  Cartera pendiente final: COP 0,00.
- Servidor Angular accesible en la ruta del módulo y proxy API HTTP 200.

No se utilizó la base operativa. El entorno temporal se eliminó al terminar.
Estas comprobaciones de compilación y funcionamiento básico no cubren todas las
variantes, concurrencia, impresión o navegadores. La validación integral y la
revisión fiscal profesional quedan pendientes antes de un uso productivo.
