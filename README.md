# WorkSoft

ERP en construcción para empresas colombianas. El modo actual permite explorar
la aplicación, registrar empresas y consultar sus datos sin configurar servicios.

## Ver la aplicación

Requiere Node.js 24 LTS y npm 11. Desde la raíz del repositorio:

```bash
cd frontend/worksoft-web
npm ci
npm start
```

Abrir http://localhost:4200. Acceso directo, sin login, Docker, PostgreSQL,
Keycloak ni backend. Los datos viven en memoria del navegador y se borran al
recargar la página. La interfaz muestra esta condición como Modo local.

Al abrir una empresa puedes seleccionar Contabilidad o Facturación y recorrer
sus menús. Las operaciones contables y de facturación están en preparación.

## Backend opcional

Con JDK 25:

```bash
cd services/company-service
./mvnw spring-boot:run
```

En Windows usar .\mvnw.cmd. El perfil predeterminado local usa memoria, sin
credenciales ni servicios externos, y escucha en 127.0.0.1:8081.

Para conectar Angular con ese backend, cambiar mode a api en
frontend/worksoft-web/public/config.json y mantener npm start ejecutándose.
En ese caso los datos se borran al reiniciar Java, no al recargar Angular.

## Siguientes decisiones

La persistencia y los usuarios con autenticación propia en base de datos se
implementarán después. Este modo local no representa un sistema autenticado ni
un despliegue de producción. No se implementa validación DIAN en este incremento.

- [Frontend](docs/run-frontend.md)
- [Backend local](docs/run-company-service.md)
- [Decisión de simplificación](docs/adr/0005-modo-local.md)
- [Contexto colombiano](docs/architecture/colombia.md)
