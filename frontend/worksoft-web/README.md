# WorkSoft Web — modo local

```bash
npm ci
npm start
```

Requiere Node.js 24 LTS y npm 11. Abrir http://localhost:4200.
No requiere servicios externos. Los datos se borran al recargar la página.

- npm test: pruebas unitarias.
- npm run build: compilación de producción.
- npm run e2e: navegador local, sin usuarios ni credenciales (instalar Chromium).

Configurar mode=api en public/config.json para usar el backend Java local en
memoria. Ver [guía](../../docs/run-frontend.md).
