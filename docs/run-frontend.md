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

## Espacio de trabajo por empresa

En Mis empresas, pulsa el nombre o Abrir empresa. Se muestra la empresa elegida
y un selector de módulos: Contabilidad y Facturación. Crear una empresa abre
este mismo espacio automáticamente.

Contabilidad incluye Plan de cuentas, Comprobantes contables, Terceros e Informes
contables. Facturación incluye Facturas de venta, Clientes, Productos y servicios,
y Notas crédito y débito. Cada opción tiene ruta y pantalla de preparación.

Los menús mantienen el UUID de empresa en todas sus rutas. Cambiar empresa vuelve
al listado y limpia el contexto anterior. Datos de la empresa conserva la consulta
de información general. Si la empresa ya no existe en memoria, no se muestran
módulos hasta seleccionar otra empresa.

Este incremento implementa selección y navegación. Los formularios, movimientos,
cálculos contables, documentos de venta y comunicación DIAN se desarrollarán
después. El arranque sigue siendo npm start, sin BD ni autenticación externa.
