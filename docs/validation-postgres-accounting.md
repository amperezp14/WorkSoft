# Validación de PostgreSQL y plan de cuentas

- Maven verify: BUILD SUCCESS; 31 pruebas, sin fallos, errores ni omisiones.
- Incluye 8 pruebas nuevas sobre PostgreSQL 17.6 con Testcontainers: migraciones,
  referencia de 108 códigos, creación y edición real, versión, código fijo,
  jerarquía, unicidad concurrente, consulta entre empresas, tipos y paginación.
- El perfil postgres no crea JwtDecoder ni requiere proveedor de identidad.
- Las pruebas históricas connected conservan autorización por membresía y revocación.
- npm test: 7 pruebas unitarias correctas; se retiraron las 5 pruebas del almacén
  demo eliminado y se añadieron 2 del cliente de cuentas.
- npm run build: compilación de producción correcta, sin advertencias de presupuesto.
- 7 pruebas Chromium correctas contra API Java y PostgreSQL reales, sin mocks de BD.
- Navegador: consulta de CAJA GENERAL, creación de 11050501, edición del nombre,
  código deshabilitado en edición y conservación de cambios tras recargar.
- Una prueba adicional del JAR crea y actualiza una cuenta, detiene Java, lo
  reinicia con la misma BD y comprueba que el nombre y la versión permanecen.
- Revisada captura del formulario/tabla de escritorio y comprobado el viewport
  móvil sin desbordamiento horizontal. Capturas excluidas de Git.
- Contratos OpenAPI de empresas y contabilidad validados contra esquema 3.1 oficial.

PostgreSQL y los procesos de prueba son efímeros y se detuvieron al finalizar.
Las contraseñas de prueba se generaron en memoria y no se incluyeron en el repositorio.
No se implementaron autenticación de usuarios, asientos, impuestos ni emisión DIAN.
La referencia comercial es parcial y tiene fuentes y alcance en docs/accounting.
