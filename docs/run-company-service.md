# Backend PostgreSQL

El perfil predeterminado es postgres: requiere PostgreSQL y no requiere Keycloak.
Ver [configuración, comandos y formulario](run-postgres-accounting.md).

```bash
cd services/company-service
./mvnw spring-boot:run
```

PowerShell: .\mvnw.cmd spring-boot:run. JDK 25. En IntelliJ quitar el perfil local
y usar postgres. El backend carga opcionalmente platform/local/.env desde la raíz
o desde el servicio. DB_PASSWORD debe coincidir con la contraseña real de la BD.

Las migraciones V1–V3 crean empresas, catálogo de referencia y cuentas por empresa.
Flyway se ejecuta al iniciar; no se recrea el volumen para aplicar este incremento.
El perfil postgres no crea JwtDecoder y usa el contexto local de trabajo existente.
Las tablas de negocio se guardan en PostgreSQL; los usuarios/login propios siguen pendientes.

Los perfiles local (pruebas históricas en memoria) y connected (pruebas OIDC
históricas) son explícitos y no constituyen alternativas automáticas al fallar la BD.
El frontend ya no ofrece modo demo. ./mvnw verify ejecuta la suite completa y
necesita Docker para los contenedores PostgreSQL de prueba.
