# Frontend local sin servicios externos

Requiere Node.js 24 LTS y npm 11. Desde la raíz:

```bash
cd frontend/worksoft-web
npm ci
npm start
```

Abrir http://localhost:4200. No necesitas Java, Docker, PostgreSQL, Keycloak,
variables .env, usuarios ni contraseñas. El registro y consulta de empresas se
resuelven en memoria del navegador, sin peticiones a la API.

## Configuración predeterminada

public/config.json:

```json
{ "mode": "demo", "apiBaseUrl": "/api/v1" }
```

El banner Modo local explica que los datos se pierden al recargar. No se guardan
registros empresariales ni contraseñas en localStorage. NIT y DV siguen siendo
texto; país CO, moneda COP y zona horaria America/Bogota.

El formulario valida datos, conserva la clave de reintentos y rechaza NIT duplicado.
También funcionan el listado, filtro, paginación y detalle. Las identificaciones
solo tienen validación estructural: el registro no equivale a habilitación DIAN.

## Conectar opcionalmente el backend local

1. Arrancar Java siguiendo la guía del backend; no requiere base de datos.
2. Cambiar mode a api en public/config.json.
3. Mantener npm start y abrir nuevamente la app.

El proxy /api conecta con http://localhost:8081. Para otro puerto definir
API_PROXY_TARGET antes de iniciar Angular. En modo api, los registros viven en
memoria del servidor y sobreviven a recargar el navegador, hasta reiniciar Java.
Los modos demo y api tienen almacenes distintos; no sincronizan datos.

## Verificar y compilar

```bash
npm test
npm run build
```

Pruebas de navegador sin credenciales ni servicios externos:

```bash
npx playwright install chromium
npm run e2e
```

La suite inicia Angular, registra y consulta empresas y verifica que no haya
peticiones externas. PLAYWRIGHT_CHROMIUM_EXECUTABLE permite un Chromium ya
instalado. E2E_BASE_URL permite probar contra un frontend ya arrancado.

npm run build genera dist/worksoft-web/browser. No hace falta Node.js en el
servidor que sirve esos archivos estáticos. Este modo local es temporal; la
siguiente etapa de acceso utilizará usuarios propios almacenados en BD.
