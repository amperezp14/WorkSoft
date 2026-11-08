# WorkSoft

WorkSoft es una aplicación completa de gestión empresarial en construcción,
diseñada para el entorno empresarial y la normativa colombiana.

## Estado

Primer flujo implementado: crear, listar y consultar empresas con arquitectura
hexagonal, PostgreSQL, Flyway y autorización JWT/OIDC. Incluye aislamiento por
membresía, idempotencia, auditoría y outbox transaccional.

## Inicio rápido

Requiere JDK 25 y Docker. Primero configura PostgreSQL, Keycloak y las variables
locales siguiendo la [guía de ejecución](docs/run-company-service.md).

```bash
cd services/company-service
./mvnw verify
./mvnw spring-boot:run
```

En Windows usar `mvnw.cmd`. Las pruebas necesitan Docker (Testcontainers).

## Documentación

- [Ejecutar el flujo implementado](docs/run-company-service.md)
- [Decisiones de implementación](docs/adr/0003-implementacion-empresas.md)
- [Guía de la fase 1](docs/phase-1.md)
- [Diseño y contratos de la fase 2](docs/phase-2.md)
- [Decisión arquitectónica](docs/adr/0001-base-arquitectonica.md)
- [Contexto colombiano](docs/architecture/colombia.md)

El desarrollo se realiza en `develop`. Los módulos posteriores incluirán empresas,
catálogos, inventario, ventas, facturación y pagos, según el roadmap acordado.
