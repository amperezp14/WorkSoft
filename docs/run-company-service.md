# Backend local en memoria

Requiere JDK 25. No requiere PostgreSQL, Docker, Keycloak, .env ni contraseñas.
Desde la raíz de WorkSoft:

```bash
cd services/company-service
./mvnw spring-boot:run
```

En PowerShell usar .\mvnw.cmd spring-boot:run. En IntelliJ ejecutar
CompanyServiceApplication, sin perfiles activos. Si tenías un perfil explícito,
seleccionar local. El perfil predeterminado ahora es local.

Salud: http://localhost:8081/actuator/health.
API: http://localhost:8081/api/v1/companies.

En local se excluyen DataSource y Flyway. El adaptador InMemoryCompanyStore
implementa el puerto del dominio; no conecta a ninguna BD. Cada petición usa un
único contexto local de trabajo sin exigir tokens. El servidor escucha solo en
127.0.0.1 y los registros se pierden al reiniciar el proceso.

La creación conserva idempotencia, validación y unicidad de NIT en memoria.
No hay auditoría durable ni entrega de eventos en este perfil. No es un mecanismo
de autenticación ni un despliegue de producción.

## Angular

Para solo ver la interfaz basta npm start con el modo demo predeterminado.
Para probar la API Java desde la interfaz, configurar mode=api en
frontend/worksoft-web/public/config.json. No se envían cabeceras de autenticación.

## Pruebas

```bash
./mvnw verify
```

La suite completa incluye las pruebas históricas PostgreSQL/seguridad, activadas
explícitamente bajo connected, que sí usan Docker. Para probar solo el arranque
local y núcleo sin Docker:

```bash
./mvnw "-Dtest=LocalApplicationTests,DomainTests,CursorTests,ArchitectureTests" verify
```

El perfil connected y el adaptador PostgreSQL se conservan como implementación
anterior aislada. Su autenticación OIDC no forma parte del nuevo arranque ni de la
solución de usuarios en BD prevista. No activar ese perfil para ver la app local.

La gestión propia de usuarios, hash de contraseñas, login y persistencia se diseñará
en la siguiente etapa; no se almacenan contraseñas en texto plano ni se simula
que el contexto local represente un usuario autenticado.
