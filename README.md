# WorkSoft

WorkSoft es una aplicación completa de gestión empresarial en construcción,
diseñada para el entorno empresarial y la normativa colombiana.

## Estado

Fase 1: base del servicio de empresas con Java 25 LTS, Spring Boot 4.1.2 y
arquitectura hexagonal. El esqueleto aún no implementa operaciones del ERP.

## Inicio rápido

Requiere JDK 25. Desde la raíz del repositorio:

```bash
cd services/company-service
./mvnw verify
./mvnw spring-boot:run
```

Salud: http://localhost:8081/actuator/health.
En Windows usar `mvnw.cmd`.

## Documentación

- [Guía de la fase 1](docs/phase-1.md)
- [Decisión arquitectónica](docs/adr/0001-base-arquitectonica.md)
- [Contexto colombiano](docs/architecture/colombia.md)

El desarrollo se realiza en `develop`. Los módulos posteriores incluirán empresas,
catálogos, inventario, ventas, facturación y pagos, según el roadmap acordado.
