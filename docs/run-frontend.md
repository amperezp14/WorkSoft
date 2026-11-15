# Frontend con PostgreSQL

La aplicación ahora consulta exclusivamente la API; se retiró el almacén demo.
Para iniciar y configurar la BD y Java seguir [la guía de PostgreSQL](run-postgres-accounting.md).

Con el backend disponible en localhost:8081:

```bash
cd frontend/worksoft-web
npm ci
npm start
```

Node.js 24 LTS y npm 11. Abrir http://localhost:4200. No requiere Keycloak ni login.
Seleccionar empresa → Contabilidad → Plan de cuentas abre la tabla y el formulario
para crear y editar registros en PostgreSQL. Los datos persisten al recargar.

public/config.json solo contiene apiBaseUrl. No contiene contraseñas; las credenciales
de PostgreSQL pertenecen al backend. Si la API o la BD falla se muestra un error:
no se utilizan datos locales como sustituto.

Facturación y las otras opciones contables conservan su navegación inicial; la
emisión DIAN y los comprobantes aún no están implementados.

npm test ejecuta las pruebas unitarias; npm run build genera los archivos estáticos.
Las pruebas de navegador necesitan la API y una BD de pruebas; no usan mocks de
persistencia. Para desplegar el frontend compilado no hace falta Node.js en el
servidor estático, y el acceso productivo requerirá la autenticación de una fase posterior.
