# WorkSoft Web

Requiere Node.js 24 LTS/npm 11, API Java y PostgreSQL.

```bash
npm ci
npm start
```

Abrir http://localhost:4200. Empresas y Plan de cuentas se consultan y guardan en
PostgreSQL a través de /api/v1. No se envían credenciales de BD desde el navegador.
Ver [guía](../../docs/run-postgres-accounting.md).

npm test: unitarias. npm run build: compilación. npm run e2e: navegador con una
API y PostgreSQL de pruebas previamente levantados; crea datos sintéticos.
