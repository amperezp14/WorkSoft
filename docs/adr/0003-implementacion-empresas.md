# ADR 0003 — Implementación inicial de empresas

Estado: implementado como primer incremento funcional.

## Decisiones concretas

- Adaptador PostgreSQL con Spring JDBC y Flyway; no se usa JPA porque el contrato
  de persistencia es pequeño y las consultas de aislamiento son explícitas.
- CompanyStore es el puerto de persistencia del agregado y su aprovisionamiento
  atómico. El adaptador coordina una transacción local con TransactionTemplate;
  dominio y aplicación siguen sin Spring. La separación en múltiples repositorios
  descrita en fase 2 se sustituye por este puerto cohesivo, sin exponer SQL al núcleo.
- PostgreSQL advisory lock por actor y clave serializa reintentos entre instancias;
  UNIQUE(nit) protege carreras con distintas claves. lock_timeout es 5 segundos.
- La representación de empresa es inmutable en esta API. Idempotencia guarda su
  ID, hash y expiración en vez de duplicar response_body. La reconstrucción produce
  la misma respuesta, con instantes truncados a microsegundos antes de guardarse.
  Si se añaden modificaciones, habrá que persistir la respuesta original o versionarla.
- No hay borrado físico de empresas. Los registros expirados de idempotencia se
  eliminan al reutilizar esa clave; falta el proceso general de limpieza operativa.
- Cursores HMAC-SHA256 con secreto externo de al menos 32 bytes, expiración de
  15 minutos, actor seudonimizado, límite y posición. No contienen NIT ni nombre.
- Acceso OIDC con perfil Keycloak: validación de firma RS256, issuer, audience,
  tiempos y claim typ=Bearer. company:provision se obtiene de realm_access.roles.
  Cada petición de datos se filtra por membresía activa en PostgreSQL.
- Auditoría almacena actor seudónimo, operación, empresa e instante. El ID de
  auditoría permite identificar el registro; no hay correlación distribuida aún.
- Outbox conserva CloudEvent completo sin datos tributarios; no se publica hasta
  incorporar un consumidor y diseñar entrega, reintentos y deduplicación.
- Restricciones del dominio verifican formato de NIT/DV, no legitimidad fiscal ni
  algoritmo del DV. Esas validaciones esperan evidencia normativa precisa.

## Límites

Este incremento habilita desarrollo y pruebas locales. El realm password grant,
la base con usuario propietario y las imágenes de Compose no son la configuración
de producción. Falta endurecer identidades, permisos de base, TLS, retención,
observabilidad y recuperación en las fases de plataforma.
