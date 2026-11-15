# ADR 0005 — Arranque local sin dependencias externas

Estado: implementado por solicitud explícita del usuario. Sustituye el requisito
de ejecutar Keycloak y PostgreSQL para explorar la aplicación.

## Decisiones

- Angular inicia directamente en Empresas, sin SDK Keycloak ni guardas de login.
- mode=demo es el valor predeterminado de configuración: MemoryCompanies resuelve
  operaciones en memoria del navegador, sin llamadas a la API ni almacenamiento local.
- mode=api permite usar el servidor Java local con la misma interfaz de empresas.
- Java usa el perfil local por defecto: adaptador en memoria, sin DataSource,
  migraciones ni JwtDecoder. La API usa un contexto local único y no tokens ficticios.
- El perfil local escucha en loopback. No debe habilitarse como seguridad de producción.
- El puerto hexagonal y la lógica de dominio se conservan; el adaptador local no
  proporciona persistencia de auditoría ni outbox. La API anterior se conserva
  bajo un perfil conectado explícito y separado para no confundir sus garantías.
- Idempotencia y unicidad se mantienen en memoria. Los datos desaparecen al
  recargar Angular (demo) o reiniciar Java (api). No hay sincronización entre modos.
- Se retira Keycloak del Compose y del flujo de trabajo actual. No se modifica ni
  elimina ninguna base existente del usuario.

## Próxima etapa de autenticación

La autenticación propia respaldada por BD es el objetivo posterior, no una
función de este modo sin base de datos. Requiere diseñar usuarios, hash seguro de
contraseñas, recuperación, sesiones y autorización por empresa antes de habilitarla.
Las decisiones OIDC previas quedan como registro histórico, no como requisito actual.
El contexto colombiano sigue vigente para todos los módulos.
