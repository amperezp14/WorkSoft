# Ejecutar el servicio de empresas

Requisitos: JDK 25, Docker con Compose y Git. Maven Wrapper 3.9.16 incluido;
Maven 3.9.2 también fue reportado como funcional por el usuario.

## 1. Configurar secretos locales

Desde la raíz del repositorio:

```bash
cp platform/local/.env.example platform/local/.env
```

Editar platform/local/.env y completar DB_PASSWORD, KC_ADMIN_PASSWORD y
CURSOR_SECRET con valores diferentes, generados localmente (por ejemplo con
`openssl rand -hex 32`). CURSOR_SECRET debe tener al menos 32 bytes. Nunca
publicar .env. Conservarlo entre reinicios para no cambiar claves y contraseñas.

En Bash, cargar las variables para la aplicación:

```bash
set -a
. platform/local/.env
set +a
```

En PowerShell configurar `$env:DB_PASSWORD`, `$env:CURSOR_SECRET`,
`$env:KC_ADMIN_USERNAME` y `$env:KC_ADMIN_PASSWORD` con los mismos valores del
archivo. Compose lee el archivo con --env-file; Java recibe variables del proceso.

## 2. Iniciar PostgreSQL y Keycloak

```bash
docker compose --env-file platform/local/.env -f platform/local/compose.yml up -d
```

PostgreSQL escucha en localhost:5432 y Keycloak en http://localhost:8080.
Las imágenes y puertos de este Compose son exclusivamente de desarrollo local.
Los volúmenes conservan los datos. `down` detiene los servicios; no usar `down -v`
si necesitas conservarlos. Cambiar DB_PASSWORD no cambia por sí solo la contraseña
de un PostgreSQL que ya tenga volumen inicializado.

## 3. Preparar un operador de desarrollo

1. Entrar a http://localhost:8080/admin con las credenciales bootstrap locales.
2. Seleccionar el realm worksoft-local, importado por Compose.
3. En Users crear un usuario de prueba, asignarle contraseña no temporal y
   completar los campos exigidos por el perfil local (nombre, apellido y correo de
   prueba) y comprobar que no tenga acciones obligatorias pendientes.
4. En Role mapping asignarle el rol de realm company:provision.

El realm no contiene usuarios ni contraseñas predefinidos. El cliente público
worksoft-local-cli permite password grant solo para facilitar pruebas locales.
En producción se utilizará Authorization Code + PKCE, MFA y una configuración de
Keycloak independiente; no publicar este realm de desarrollo.

## 4. Compilar y arrancar

En la terminal que cargó las variables:

```bash
cd services/company-service
./mvnw verify
./mvnw spring-boot:run
```

Las pruebas usan Testcontainers y necesitan Docker; no necesitan la base de Compose
ni usuarios locales. En Windows usar .\mvnw.cmd. En IntelliJ importar el pom del
servicio, usar Maven Wrapper, JDK 25 e introducir DB_PASSWORD y CURSOR_SECRET en
la configuración de ejecución. Las migraciones Flyway se aplican automáticamente.

Salud: http://localhost:8081/actuator/health. Las API requieren token.
Para base remota configurar DB_URL y DB_USERNAME; para el proveedor configurar
OIDC_ISSUER y OIDC_JWKS. La audiencia esperada es company-service y se acepta el
perfil de access token de Keycloak con claim typ=Bearer, no ID tokens.

## 5. Obtener token y probar (Bash)

Requiere Python 3 para extraer el token. En otra terminal:

```bash
read -r -p 'Usuario local: ' WORKSOFT_USERNAME
read -r -s -p 'Contraseña local: ' WORKSOFT_PASSWORD
printf '\n'
TOKEN=$(curl --fail --silent --show-error \
  http://localhost:8080/realms/worksoft-local/protocol/openid-connect/token \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=worksoft-local-cli' \
  --data-urlencode "username=$WORKSOFT_USERNAME" \
  --data-urlencode "password=$WORKSOFT_PASSWORD" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')
unset WORKSOFT_PASSWORD

curl --fail-with-body -i http://localhost:8081/api/v1/companies \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: company-example-0001' \
  -d '{"legalName":"Empresa de ejemplo sintética","nit":"000000001","verificationDigit":"0"}'

curl --fail-with-body http://localhost:8081/api/v1/companies \
  -H "Authorization: Bearer $TOKEN"
```

El NIT del ejemplo es sintético y solo demuestra validación estructural. No se
consulta DIAN ni se certifica el DV o la habilitación tributaria. No pegar tokens
en tickets o commits. Renovar el token cuando expire.

POST devuelve 201 y Location. Repetir la misma clave y datos reproduce el resultado;
otros datos con la misma clave devuelven 409. Un usuario sin membresía recibe 404
al consultar el UUID de otra empresa, incluso si tiene company:provision.

## Cliente Angular

Para usar el flujo desde la interfaz sigue la [guía del frontend](run-frontend.md).
El cliente de navegador worksoft-web usa Authorization Code + PKCE y requiere
importación si el realm ya existía. No utiliza el cliente local de password grant.

## Alcance implementado

Creación, consulta individual, listado paginado, roles de lectura ADMIN/READER,
idempotencia, membresía inicial ADMIN, auditoría y escritura transaccional outbox.
No hay todavía actualización o eliminación de empresas, API de membresías, envío
al broker, cálculos fiscales, validación aritmética del DV ni integración DIAN.
No se implementa legislación tributaria a partir de una validación de formato.
