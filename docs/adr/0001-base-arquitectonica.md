# ADR 0001 — Base arquitectónica

Estado: aceptada para la fase 1.

## Contexto

WorkSoft es un ERP para empresas en Colombia. El repositorio comienza con un
servicio de empresas; los demás límites se validarán con procesos reales.

## Decisión

- Monorepositorio y unidades de construcción y despliegue independientes.
- Java 25 LTS, Spring Boot 4.1.2, Maven 3.10.0 mediante Wrapper.
- Arquitectura hexagonal, inicialmente con un módulo Maven por servicio.
- Dominio y aplicación sin dependencias de Spring, JPA, HTTP ni adaptadores.
- Spring compone las implementaciones en la capa externa.
- Cada servicio será dueño de sus datos; sin tablas ni entidades compartidas.
- PostgreSQL se incorporará al implementar persistencia.
- OIDC con proveedor de identidad; no se construirá autenticación propia.
- La fase 1 no incluye operaciones de negocio ni acceso a datos.

## Consecuencias

Los paquetes marcan los límites y ArchUnit verifica dependencias. Los archivos
package-info documentan los espacios previstos sin inventar clases de negocio.
Las reglas pasan sobre el esqueleto, pero no acreditan casos de uso aún inexistentes.
No se publicará este servicio sin autorización, control multiempresa y revisión de
exposición de endpoints. La salud queda disponible para desarrollo local.

Angular, Node.js, Jenkins y Kubernetes se fijarán al introducirlos. La elección de
Angular debe cumplir el requisito de la LTS más reciente y su matriz de Node.js.
