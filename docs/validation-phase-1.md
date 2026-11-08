# Validación de fase 1

Realizada el 8 de noviembre de 2026 sobre el esqueleto de company-service.

- JDK: Eclipse Temurin 25 LTS, instalado fuera del repositorio.
- Maven: 3.10.0, descargado mediante Wrapper.
- Spring Boot: 4.1.2, versión estable comprobada en Maven Central.
- ArchUnit: 1.5.1.
- Maven verify: BUILD SUCCESS; 4 pruebas, sin fallos, errores ni omisiones.
- JAR ejecutable: arranque real con Java 25 en 127.0.0.1:18081.
- GET /actuator/health: HTTP 200, status UP y grupos liveness/readiness.
- GET /actuator/health/liveness: HTTP 200, status UP.
- GET /actuator/health/readiness: HTTP 200, status UP.
- Proceso detenido después de las comprobaciones.

El entorno cloud necesitó caché Maven en un directorio escribible y configuración
local de proxy y certificados para resolver dependencias. Esos ajustes son del
entorno y no se incorporan al proyecto. No se usó Java 21 para compilar.

Las reglas de arquitectura verifican el esqueleto documentado; aún no existen
casos de uso de negocio. No se ha validado cumplimiento fiscal ni desplegado
ningún servicio en producción.
