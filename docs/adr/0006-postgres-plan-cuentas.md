# ADR 0006 — PostgreSQL y plan de cuentas por empresa

Estado: implementado por solicitud del usuario. Sustituye el arranque demo del ADR 0005.

- Frontend solo API; Java postgres por defecto, sin Keycloak. No hay respaldo en memoria.
- El módulo de cuentas vive inicialmente junto a empresas para mantener el arranque
  sencillo: dominio, puertos, aplicación y adaptador JDBC separados. Una separación
  futura en microservicio requerirá revisar la propiedad y replicación de datos.
- Catálogo global de referencia y copia editable por empresa. Migraciones versionadas
  inicializan 108 códigos contrastados con fuente oficial; no son un catálogo nuevo legal 2026.
- Código único por empresa, padre derivado e inmutable en edición; FK compuesta por
  empresa impide referencias cruzadas. No hay borrado físico ni saldos ficticios.
- Transacción con bloqueo de la empresa serializa cambios de jerarquía. Restricción
  única protege creación concurrente y versión protege actualizaciones concurrentes.
- Nivel clase/grupo/cuenta/subcuenta según codificación de referencia; auxiliares de
  8 y 10 dígitos como política del producto. Naturaleza configurable por cuenta.
- Sin movimientos en cuentas con hijas; no se desactiva una superior con hijas activas.
- Las cuentas y las empresas persisten al reiniciar; la prueba incluye reinicio del JAR.
- La seguridad sigue siendo el contexto local sin login, ligado a loopback. Los
  controles de autorización multiusuario deben diseñarse con la autenticación propia.

Fuentes, límites y datos de referencia: docs/accounting/puc-reference.md.

En postgres el contexto local único consulta las empresas existentes sin depender
de membresías de un proveedor de identidad. Las pruebas OIDC históricas connected
conservan su filtro por membresía y su revocación. Todas las operaciones de cuentas
se delimitan por company_id; no se transfieren ni eliminan datos históricos.
