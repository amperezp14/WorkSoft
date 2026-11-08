# Validación del primer flujo implementado

Entorno: Java 25 LTS, Maven Wrapper 3.9.16 y Docker local.

`./mvnw clean verify`: BUILD SUCCESS. 20 pruebas sin fallos, errores ni omisiones:

- 2 de dominio: normalización, identificación como texto y rechazo de formatos.
- 1 de cursor: rechazo por expiración.
- 3 reglas de arquitectura: independencia de dominio, aplicación y arranque.
- 14 de integración con PostgreSQL 17.6 real mediante Testcontainers.

La integración levanta un endpoint JWKS local con una clave RSA efímera y envía
JWT firmados a la cadena real de seguridad. No sustituye JWT por un mock.
Comprueba audiencia, issuer, firma, expiración, exp obligatorio y tipo de token;
permisos de plataforma y de empresa; creación y consulta; reintentos idénticos;
conflictos de idempotencia y NIT; membresías revocadas; paginación ligada al actor;
entradas y tipos estrictos; rollback al fallar outbox; y carreras concurrentes.

Compose pasó `docker compose config --quiet` con variables efímeras. No se
publican contraseñas, tokens ni configuraciones sensibles de la verificación.

No se han validado cálculos o cumplimiento tributario colombiano: el incremento
verifica formatos de NIT y DV, sin algoritmo de DV ni comunicación con DIAN.

## Prueba del JAR con proveedor real

Comprobado además con el JAR ejecutable, Keycloak 26.4.7 y PostgreSQL 17.6
levantados en contenedores efímeros independientes de Testcontainers:

- Importación del realm local y creación de un operador con perfil completo.
- Asignación de company:provision y obtención de access token de Keycloak real.
- Migración Flyway y salud UP.
- Creación HTTP 201; reintento idéntico HTTP 201 con el mismo cuerpo.
- Consulta y listado HTTP 200.
- Credenciales y claves aleatorias generadas en memoria, sin incluirlas en Git.
- Proceso y contenedores de verificación detenidos al finalizar.
