# PostgreSQL y plan de cuentas

## Configurar una vez

Desde la raíz, copiar platform/local/.env.example a platform/local/.env si aún no
existe. Completar DB_PASSWORD con la contraseña real del usuario worksoft.
DB_URL y DB_USERNAME tienen valores predeterminados locales. No guardar .env en Git.

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/worksoft
DB_USERNAME=worksoft
DB_PASSWORD=TU_CONTRASENA_REAL
```

Si la base ya fue inicializada, usar su contraseña actual. Modificar .env no cambia
la contraseña almacenada por PostgreSQL. Las variables de entorno explícitas de
IntelliJ o de la terminal tienen precedencia sobre el archivo; deben coincidir.

Con Docker Desktop en ejecución:

```bash
docker compose --env-file platform/local/.env -f platform/local/compose.yml up -d postgres
docker compose --env-file platform/local/.env -f platform/local/compose.yml ps
```

También puedes usar un PostgreSQL instalado y cambiar DB_URL. La aplicación solo
usa PostgreSQL; Docker es una forma opcional de ejecutarlo.

## Arrancar Java y Angular

Terminal 1:

```bash
cd services/company-service
./mvnw spring-boot:run
```

PowerShell: .\mvnw.cmd spring-boot:run. En IntelliJ, JDK 25 y perfil postgres.
Quitar SPRING_PROFILES_ACTIVE=local si estaba en la configuración anterior.
El perfil predeterminado es postgres. No hace falta CURSOR_SECRET para arrancar;
si se configura, estabiliza las firmas de paginación entre reinicios.

Spring lee opcionalmente platform/local/.env desde la raíz o desde el directorio
del servicio. Flyway crea las tablas y carga la referencia al iniciar. Las cuentas
se agregan a las empresas existentes durante la migración y a las nuevas en la
misma transacción que crea la empresa.

Terminal 2, desde la raíz:

```bash
cd frontend/worksoft-web
npm ci
npm start
```

Abrir http://localhost:4200. El frontend siempre usa /api/v1 y el proxy al backend
localhost:8081. Ya no tiene modo demo ni almacén de empresas en memoria.

## Usar el formulario

1. Crear o abrir una empresa.
2. Elegir Contabilidad y Plan de cuentas.
3. Buscar por código o nombre; la tabla pagina de 25 en 25.
4. Para crear un auxiliar, escribir por ejemplo 11050501, nombre y naturaleza.
   Su superior 110505 debe existir y no permitir movimientos.
5. Guardar cuenta: se inserta en PostgreSQL para esa empresa.
6. Editar en una fila permite cambiar nombre, naturaleza, estado, movimientos y
   observaciones. El código queda fijo para conservar su jerarquía.
7. Guardar cambios: actualiza solo esa cuenta y aumenta su versión.
8. Recargar el navegador o reiniciar Java conserva los registros.

Los códigos admitidos por el producto tienen 1, 2, 4, 6, 8 o 10 dígitos. Los cuatro
primeros niveles siguen el esquema de referencia y los dos últimos son auxiliares
configurables del producto. No se afirma que 8/10 sean las únicas longitudes legales.
El catálogo inicial tiene Permite movimientos desactivado; puedes activarlo en una
subcuenta o auxiliar sin hijas. Para crear hijas debe desactivarse en la superior.
No se puede desactivar una cuenta con hijas activas ni agregar hijas a una cuenta
que recibe movimientos. Las naturalezas pueden diferir para representar correctoras.

Un código repetido se rechaza dentro de una misma empresa y puede existir en otra.
Cada actualización exige la versión leída. Si alguien ya cambió la fila, se rechaza
la edición con 409 y se debe actualizar la tabla antes de intentarlo nuevamente.

## Tablas

- puc_reference_accounts: 108 códigos y nombres de referencia, padre, naturaleza,
  año de la colección (2026) y fuente histórica.
- chart_accounts: cuentas propias por empresa, con UUID, código único por empresa,
  padre de la misma empresa, nombre, naturaleza, estado, movimientos, notas,
  referencia de origen, versión y fecha de actualización.

Las modificaciones del formulario afectan chart_accounts, no el catálogo global.
No se crean saldos, facturas ni asientos ficticios. Los comprobantes se registran
desde [su formulario](run-third-parties-journals.md); la operación fiscal sigue pendiente.

## Consultas de comprobación

```sql
SELECT code, name, parent_code, nature, reference_year
FROM puc_reference_accounts ORDER BY code;

SELECT company_id, code, name, nature, active, version
FROM chart_accounts ORDER BY company_id, code;
```

## Verificación

./mvnw verify necesita Docker para las pruebas PostgreSQL de Testcontainers.
En el frontend, npm test y npm run build. Las pruebas e2e requieren un backend de
pruebas con PostgreSQL real; no usar la base operativa, ya que generan empresas y
cuentas sintéticas. API_PROXY_TARGET permite otro backend y E2E_BASE_URL otro frontend.
La autenticación permanece fuera del alcance de este incremento local.

Contrato HTTP: docs/contracts/accounting.openapi.json. El espacio local sin login
muestra las empresas de esta base; la selección de empresa delimita todas las
consultas y actualizaciones de cuentas. La autorización multiusuario no se incorpora
en este incremento.
