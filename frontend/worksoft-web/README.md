# WorkSoft Web

Angular 21 LTS, componentes standalone, Signals, formularios reactivos y rutas
cargadas bajo demanda. Interfaz en español de Colombia y fechas America/Bogota.

## Ejecutar

Requiere Node.js 24 LTS y npm 11. Para preparar PostgreSQL, API y Keycloak seguir
la [guía completa](../../docs/run-frontend.md), incluyendo el cliente OAuth del navegador.

```bash
npm ci
npm start
```

Abrir http://localhost:4200. El proxy local envía /api a http://localhost:8081.

## Verificar

```bash
npm test
npm run build
```

Las pruebas unitarias no necesitan servicios externos. La suite de navegador sí
requiere la API y Keycloak reales; ver variables y comandos en la guía completa.
