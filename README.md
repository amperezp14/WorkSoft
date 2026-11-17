# WorkSoft

ERP en construcción para empresas colombianas. La aplicación guarda empresas y
su plan de cuentas, terceros, comprobantes contables y documentos de venta en PostgreSQL, sin Keycloak ni autenticación externa.

## Arranque

Requiere JDK 25, Node.js 24 LTS/npm 11 y PostgreSQL. Si utilizas Docker:

```bash
docker compose --env-file platform/local/.env -f platform/local/compose.yml up -d postgres
```

Configura DB_PASSWORD en platform/local/.env con la contraseña real de la base.
El backend carga ese archivo desde la raíz o desde services/company-service.

En otra terminal:

```bash
cd services/company-service
./mvnw spring-boot:run
```

En PowerShell usar .\mvnw.cmd spring-boot:run. En IntelliJ seleccionar el perfil
postgres o dejar los perfiles vacíos; retirar local si estaba configurado.

Para Angular:

```bash
cd frontend/worksoft-web
npm ci
npm start
```

Abrir http://localhost:4200. Crear o seleccionar una empresa, entrar a Contabilidad
→ Plan de cuentas. Cada empresa recibe 108 cuentas iniciales de referencia y
permite crear auxiliares y editar cuentas. Los cambios permanecen al recargar y
reiniciar la aplicación. No hay respaldo automático en memoria si la BD falla.

La referencia es parcial, basada en el PUC para comerciantes del Decreto 2650 de
1993 y revisada para este desarrollo en 2026; no representa un PUC oficial nuevo
ni sustituye la definición del catálogo propio según el marco contable aplicable.

- [Nómina: empleados, contratos y liquidación mensual](docs/run-nomina.md)
- [Facturación: ventas, conceptos, notas, recaudos y cartera](docs/run-facturacion.md)
- [Terceros y comprobantes: formularios, contabilización y PDF](docs/run-third-parties-journals.md)
- [Conexión y uso del plan de cuentas](docs/run-postgres-accounting.md)
- [Fuentes y alcance de la referencia](docs/accounting/puc-reference.md)
- [Frontend](docs/run-frontend.md)
- [Contexto colombiano](docs/architecture/colombia.md)

La aplicación sigue siendo un espacio local sin login; la autenticación propia
contra BD y la emisión fiscal se implementarán en etapas posteriores.
