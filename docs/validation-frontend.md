# Validación del frontend de empresas

Entorno: Node.js 24.22.1, npm 11.9.0, Angular 21.2.26, CLI 21.2.29.

- Instalación mediante npm y package-lock.json registrado.
- npm run build: compilación de producción correcta, sin advertencias de presupuesto.
- npm test: 11 pruebas correctas en 4 archivos, sin omisiones.
- Navegador Chromium: 7 pruebas correctas con Keycloak 26.4.7 y la API Java real,
  conectada a PostgreSQL 17.6 en un contenedor efímero.

Keycloak se ejecutó con su distribución oficial de JVM para evitar el límite de
disco del entorno de pruebas Docker. El Compose del repositorio sigue utilizando
la misma versión de Keycloak en contenedor. Los usuarios, contraseñas y empresas
de la prueba son sintéticos; no se guardaron secretos ni tokens en el repositorio.

## Casos verificados

1. Inicio sin sesión y vista móvil sin desbordamiento horizontal de página.
2. Fallo de configuración con opción de reintento.
3. Authorization Code + PKCE real, registro, detalle, búsqueda y NIT con ceros.
4. Validación de campos antes de enviar la petición.
5. Pérdida simulada de respuesta después de que el backend confirmara la creación:
   el reintento utiliza la misma clave y recupera exactamente la misma empresa.
6. Error seguro para un UUID sin acceso.
7. Usuario sin company:provision: botón ausente y formulario bloqueado por URL.

Se revisaron capturas de escritorio y de móvil de 390 px. Se corrigió la tabla
móvil para presentar bloques legibles y se comprobó el tamaño del nombre de la
empresa. Las capturas y artefactos de ejecución están excluidos de Git.

Las pruebas unitarias verifican parámetros de API, conservación del NIT como
texto, idempotencia, guardas, adjunción restringida de tokens, fallo de renovación,
mensajes seguros y hora de Bogotá independiente de la zona del navegador.

No se modificó código Java. No se desplegó el frontend en producción ni se
implementaron pantallas de inventario, catálogo o facturación.
