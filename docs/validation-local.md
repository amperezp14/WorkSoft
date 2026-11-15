# Validación del arranque simplificado

- Angular: npm run build correcto; SDK Keycloak retirado de dependencias.
- Frontend: 10 pruebas unitarias correctas y 3 de navegador Chromium correctas.
- Navegador: acceso directo, creación, detalle, listado, validaciones, NIT duplicado,
  vista móvil y pérdida de datos al recargar. Verificado que no hubo solicitudes
  fuera del origen Angular: sin backend, BD ni proveedor de identidad.
- Java: Maven verify correcto, 23 pruebas sin omisiones. Incluye 3 del arranque
  local y las 20 previas; PostgreSQL solo se utiliza en los tests conectados.
- JAR iniciado sin DB_*, OIDC_*, CURSOR_SECRET ni perfiles activos: salud UP,
  creación 201 y consulta 200 sin cabecera Authorization. Proceso detenido al terminar.
- El contexto local no contiene DataSource ni JwtDecoder. Escucha en loopback.
- OpenAPI y Compose opcional pasan validación de estructura.

La futura autenticación propia contra base de datos no está implementada en este
incremento. La memoria de navegador y la memoria de Java son independientes y
volátiles. El modo local no genera auditoría durable ni publica eventos.
