# Ejecutar el frontend de WorkSoft

Este incremento adelanta el frontend antes del catálogo/inventario, por solicitud
del usuario. Implementa el flujo de empresas ya disponible en el backend.

## 1. Herramientas

- Node.js 24 LTS (ver frontend/worksoft-web/.nvmrc) y npm 11.
- JDK 25 y Docker para el backend y los servicios locales.
- Angular 21 está en LTS según el calendario oficial consultado para este trabajo:
  https://angular.dev/reference/releases. Se fijaron Angular 21.2.26 y CLI 21.2.29;
  package-lock.json fija el árbol instalado. Node 24 es compatible con esta serie.

## 2. Preparar el backend

Seguir [Ejecutar el servicio de empresas](run-company-service.md): completar las
variables de platform/local/.env, iniciar Compose y arrancar company-service.
La API debe responder en http://localhost:8081/actuator/health.

## 3. Preparar el cliente de navegador en Keycloak

El realm actualizado contiene el cliente público worksoft-web con Authorization
Code y PKCE S256. No comparte el cliente password grant del ejemplo de terminal.

**Si ya tenías un volumen de Keycloak:** el import de arranque no modifica un
realm existente. No borres sus datos. Entra a la consola, selecciona worksoft-local,
ve a Clients y usa Import client con platform/local/worksoft-web-client.json.
Si el cliente ya existe, revisa o actualiza sus opciones:

- Client ID: worksoft-web.
- Client authentication: Off (cliente público, sin secreto).
- Standard flow: On; Direct access grants e Implicit flow: Off.
- Valid redirect URIs: http://localhost:4200/*.
- Valid post logout redirect URIs: http://localhost:4200/*.
- Web origins: http://localhost:4200.
- PKCE method: S256.
- Mapper de audiencia: company-service en access token, no en ID token.

El archivo JSON contiene estas opciones y el mapper. En un volumen nuevo se
importan automáticamente desde worksoft-realm.json.

Usar localhost tanto para Angular como para las URLs configuradas; no alternar
con 127.0.0.1 sin actualizar los redirect URIs y Web origins.

Para probar el registro asigna company:provision a un usuario local con perfil
completo y contraseña no temporal. Un usuario sin ese rol podrá consultar solo
las empresas donde tenga membresía activa; no verá el formulario de registro.
Crear usuarios y asignar accesos sigue siendo una operación administrativa.

## 4. Arrancar Angular

Desde la raíz del repositorio:

```bash
cd frontend/worksoft-web
npm ci
npm start
```

Abrir http://localhost:4200 y pulsar Iniciar sesión. La contraseña se introduce en
Keycloak; la aplicación no implementa su propio formulario de credenciales.

En otro terminal, el backend debe seguir funcionando. No hacen falta cambios de
CORS en Java: el navegador llama /api y el proxy local conecta con la API.
Para otro puerto de backend, en Bash: `API_PROXY_TARGET=http://localhost:8082 npm start`.
En PowerShell configurar `$env:API_PROXY_TARGET` antes de `npm start`.

## Pantallas y comportamiento

- Acceso: conexión con Keycloak y opción de reintento si falla la configuración.
- Mis empresas: solo empresas autorizadas, lista vacía, carga, errores y paginación.
- Búsqueda: filtra únicamente las empresas ya cargadas, por nombre o NIT.
- Registrar empresa: razón social, NIT como texto y DV separado; validación local
  y errores del servidor. Solo disponible para company:provision.
- Detalle: datos de la empresa y hora de registro mostrada en America/Bogota.
- Cerrar sesión: redirección al cierre de sesión del proveedor.

El mismo formulario conserva la clave de idempotencia para un mismo contenido
normalizado durante los reintentos. Deshabilita el envío mientras hay una petición.
Las claves viven en memoria del formulario, no sobreviven a una recarga.
El backend conserva la autoridad sobre permisos, unicidad y validación.

La aplicación nunca interpreta un NIT como número, nunca calcula montos con float
ni presenta el registro como acreditación DIAN. El estado Activa es interno.
Todavía no hay pantallas de productos, inventario, facturación ni gestión de usuarios.

## Configuración de despliegue

public/config.json es configuración pública de ejecución: apiBaseUrl y datos
públicos del cliente Keycloak. No contiene contraseñas, secretos ni tokens.
Se carga al iniciar sin caché. Configurar allí las direcciones reales al desplegar.

`npm run build` genera dist/worksoft-web/browser. El servidor deberá servir los
archivos estáticos, redirigir rutas Angular a index.html, servir config.json y
enrutar /api a la API. Las rutas API no deben responder con index.html. Usar HTTPS,
registrar los orígenes/redirecciones exactos del despliegue y mantener Authorization
Code + PKCE. La configuración localhost no es una configuración de producción.

Los tokens se mantienen en memoria del SDK. El SDK puede guardar estado temporal
de la redirección OAuth/PKCE para verificar el retorno; no se guardan tokens de
acceso o refresh en localStorage ni en archivos. El interceptor solo añade el token
al origen y prefijo de la API configurada y renueva la sesión antes de enviarlo.

## Pruebas

```bash
npm test
npm run build
```

Para navegador, usa una base de pruebas y dos usuarios de prueba con perfiles
completos: uno con company:provision y otro sin membresías ni ese permiso.
La suite crea empresas sintéticas; no usar la base operativa.

Instalar Chromium de Playwright una sola vez:

```bash
npx playwright install chromium
```

Configurar E2E_USERNAME, E2E_PASSWORD, E2E_READER_USERNAME y E2E_READER_PASSWORD en
el entorno del proceso y ejecutar `npm run e2e`. No guardar estos valores en Git.
Por defecto la suite arranca Angular en localhost:4200 y usa el config.json local.
E2E_KEYCLOAK_URL permite otro servidor de pruebas sin modificar el archivo público.
E2E_BASE_URL permite apuntar a un servidor frontend ya arrancado; en ese caso la
suite no inicia ng serve. PLAYWRIGHT_CHROMIUM_EXECUTABLE permite usar un Chromium
instalado en el sistema. Los resultados y capturas se excluyen de Git.

Las pruebas de navegador realizan login real con Authorization Code + PKCE. No
existe un modo de autenticación falso ni un bypass dentro de la aplicación.
