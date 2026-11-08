# Fase 2 — Diseño funcional y contratos

Estado: diseño de referencia. El primer flujo ya está implementado; consultar
[ejecución](run-company-service.md) y [decisiones concretas](adr/0003-implementacion-empresas.md).

## Objetivo y alcance

Definir un flujo verificable: un operador autorizado registra una empresa
colombiana, obtiene membresía ADMIN y consulta únicamente las empresas a las
que tiene acceso. Este documento conserva el diseño inicial. Los controladores, persistencia y
autorización del primer flujo se describen en la guía de ejecución.

Hipótesis inicial: una empresa persona jurídica colombiana corresponde a un
ámbito de aislamiento (tenant). Los establecimientos y sucursales pertenecen a
esa empresa. Un grupo empresarial se representará inicialmente por varias
empresas con membresías independientes. Personas naturales comerciantes,
contabilidad de grupos, suscripciones y delegación entre empresas quedan para
ampliaciones del modelo. Esta hipótesis es una decisión de producto, no una
restricción de la legislación colombiana.

## Recorrido funcional inicial

1. El operador inicia sesión mediante OIDC. El servicio verifica un access token
   destinado a su API; un ID token no sirve para invocar la API.
2. El operador tiene el permiso de plataforma company:provision. No se concede
   a cualquier usuario autenticado ni se deduce de ser ADMIN de otra empresa.
3. Envía POST /api/v1/companies con nombre legal, NIT y dígito de verificación,
   usando una clave de idempotencia nueva para la operación.
4. El caso de uso valida la entrada y asigna un UUID interno. El NIT es un dato
   tributario, nunca la identidad técnica ni una credencial de acceso.
5. Una transacción crea Company, la membresía ADMIN del operador, el registro de
   idempotencia, la auditoría y el evento en outbox. Ante cualquier fallo, revierte
   todos los registros. Responde 201 con Location y representación de la empresa.
6. GET /api/v1/companies devuelve solo empresas con membresía activa del usuario.
7. GET /api/v1/companies/{companyId} exige membresía y permiso company:read.
   Un UUID inexistente y uno ajeno producen la misma respuesta 404.
8. Otro usuario, sin membresía, no puede consultar esa empresa aunque conozca
   su UUID o su NIT.

## Lenguaje del dominio

| Concepto | Significado |
| --- | --- |
| Company | Empresa administrada en WorkSoft; no implica inscripción o aprobación DIAN. |
| CompanyId | UUID interno e inmutable del ámbito de aislamiento. |
| TaxIdentification | NIT y dígito de verificación separados, tratados como texto. |
| Membership | Relación entre empresa y principal OIDC (issuer + subject), con rol y estado. |
| ADMIN | Administra la empresa dentro del alcance autorizado. |
| READER | Consulta información autorizada de la empresa. |
| ACTIVE | Registro operativo en WorkSoft; no certifica estado jurídico o fiscal. |

El nombre legal no es único. El NIT será único en esta instalación para impedir
crear dos ámbitos de la misma empresa, conforme a la hipótesis del ADR 0002.
Una respuesta 409 no revela propietarios, nombres ni otros datos del registro
existente. La consulta de duplicados nunca sustituye una restricción UNIQUE.

## Modelo conceptual y transacción

- companies: id UUID PK, legal_name, nit texto UNIQUE, verification_digit texto,
  status ACTIVE, country CO, currency COP, time_zone America/Bogota, created_at UTC.
- memberships: company_id FK, issuer, subject, role, status; UNIQUE por
  (company_id, issuer, subject). Solo roles ADMIN y READER inicialmente.
- idempotency: issuer, subject, operation, key, request_hash, company_id,
  response_status, response_body, expires_at; UNIQUE por actor + operación + key.
- audit: actor, operación, company_id, instante UTC y correlation_id; sin cuerpo
  HTTP, token, NIT ni nombre legal en el registro general de auditoría.
- outbox: event_id UUID PK, company_id, type, payload, occurred_at, published_at.

Es un modelo conceptual, no una migración SQL. companies es la raíz del ámbito;
las tablas de negocio futuras incorporarán company_id obligatorio y relaciones
que no permitan enlazar registros de distintas empresas. Cada servicio tendrá
su propia base y aplicará estas reglas sin acceder a tablas de otro servicio.

## Idempotencia y concurrencia

Idempotency-Key es obligatoria al crear; longitud técnica 16–128 y caracteres
ASCII alfanuméricos, guion y guion bajo. Nunca se registra en logs. Se guarda
por actor OIDC y operación durante al menos 24 horas tras la confirmación.
Una repetición autorizada con igual clave y carga normalizada devuelve el mismo
201, cuerpo y Location, sin otra empresa, membresía ni evento. Igual clave y
carga distinta devuelve 409 con código IDEMPOTENCY_CONFLICT.

El hash se calcula sobre los campos validados y normalizados en orden fijo,
no sobre bytes JSON. Solo se eliminan espacios exteriores del nombre; NIT y DV
exigen dígitos ASCII, sin puntos ni guiones y conservan ceros. La restricción
única resuelve llamadas simultáneas: la segunda espera a que termine la primera
y después reproduce el resultado o devuelve conflicto. Si vence el tiempo de
espera, devuelve 503 y Retry-After; el cliente reintenta con la misma clave.
Las operaciones que revierten no consumen la clave de forma permanente.
Tras expirar, la unicidad del NIT sigue evitando crear duplicados.

Antes de reproducir una respuesta se vuelve a comprobar identidad, permiso de
aprovisionamiento y membresía actual. Un cambio de autorización nunca permite
recuperar datos antiguos por idempotencia. La eliminación de registros expirados
y la retención de auditoría se definirán en operación, sin asumir plazos legales.

## Arquitectura del primer caso de uso

- Entrada: CompanyController convierte DTO HTTP a un comando validado.
- Puerto de entrada: CreateCompanyUseCase; contrato sin dependencias Spring.
- Aplicación: CreateCompanyService coordina las reglas y los puertos.
- Dominio: Company, CompanyId y TaxIdentification; sin anotaciones JPA ni HTTP.
- Puertos de salida: CompanyRepository, MembershipRepository,
  IdempotencyRepository, AuditRepository, OutboxRepository, AuthorizationPort,
  TransactionRunner, Clock e identificadores inyectables según implementación.
- Adaptadores: PostgreSQL implementa persistencia; configuración Spring compone
  las piezas. TransactionRunner asegura una única transacción local sin llevar
  @Transactional al núcleo. El reloj usa UTC para instantes.

Para las consultas de empresa, la consulta persistente incorpora empresa y
membresía del actor, de modo que no cargue primero datos de una empresa ajena.
Los DTO, entidades de dominio y modelos JPA son independientes.

## Contratos y referencias

- [API OpenAPI 3.1](contracts/company-service.openapi.json)
- [Evento CompanyCreated v1](contracts/company-created.v1.schema.json)
- [Aislamiento y autorización](adr/0002-empresas-y-autorizacion.md)
- [Servicios y comunicación](architecture/services.md)
- [Registro de requisitos colombianos](architecture/colombia-requirements.md)

## Casos de aceptación para la fase 3

| Caso | Resultado requerido |
| --- | --- |
| Operador autorizado crea datos válidos | 201, Location, membresía ADMIN y un evento en outbox. |
| Token ausente, inválido o para otra audiencia | 401; no persiste nada. |
| Usuario sin company:provision crea | 403; no persiste nada. |
| Datos vacíos, tipos erróneos o campos desconocidos | 400 sin efectos parciales. |
| NIT ya registrado, incluida carrera concurrente | 409 sin revelar la empresa existente. |
| Clave repetida con el mismo cuerpo | Mismo resultado y un solo evento. |
| Clave repetida con otro cuerpo | 409 IDEMPOTENCY_CONFLICT. |
| Usuario A consulta UUID de empresa B | 404 idéntico al de un UUID inexistente. |
| UUID mal formado | 400. |
| Membresía revocada | Desaparece del listado; detalle responde 404. |
| Usuario sin membresías | 200 con items vacío y nextCursor nulo. |
| Fallo al guardar membresía, auditoría u outbox | Rollback completo. |
| Paginar con cursor de otro usuario | 400; no hay exposición de datos. |
| Publicación duplicada del evento | Consumidor aplica deduplicación por event id. |

## Secuencia de la fase 3

1. Confirmar el alcance inicial de persona jurídica y la política de NIT único;
   verificar fuentes oficiales para la validación tributaria local.
2. Implementar modelo y pruebas de reglas de dominio.
3. Añadir PostgreSQL, migraciones y pruebas de transacción con Testcontainers.
4. Configurar OIDC y el aprovisionamiento restringido del primer operador.
5. Implementar POST y consultas contra el contrato, incluyendo autorización,
   idempotencia, auditoría y escritura outbox; probar aislamiento y concurrencia.
6. Verificar el flujo completo antes de abrir nuevos módulos. La entrega del
   evento a un broker se incorpora cuando exista un consumidor real.

## Cierre de fase

Están definidos alcance, flujo, propiedad de datos, amenazas de acceso,
contratos versionados y escenarios de aceptación. Los documentos no acreditan
cumplimiento legal ni comportamiento implementado. El incremento posterior implementa el primer flujo; ver ADR 0003.

## Validación documental realizada

- OpenAPI 3.1 validado con el esquema oficial 2025-09-15 de OpenAPI Initiative.
- Las 17 referencias internas del contrato se resuelven.
- Esquema de entrada: un caso sintético válido y cinco inválidos (nombre vacío,
  NIT numérico, NIT con separadores, DV de dos dígitos y campo desconocido).
- Esquema del evento válido bajo JSON Schema 2020-12; acepta un evento sintético
  y rechaza un campo adicional con información tributaria.
- El esquema no puede verificar por sí solo membresías, correspondencia entre
  subject y companyId, normalización ni idempotencia: requieren pruebas en fase 3.
- No se modificó código Java ni se repitió su compilación por cambios documentales.
