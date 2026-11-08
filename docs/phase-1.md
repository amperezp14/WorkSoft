# Fase 1 — Base técnica

## Requisitos

Git y un JDK 25 en JAVA_HOME y PATH. Maven se descarga mediante Wrapper y se
requiere Internet para la primera compilación. Docker no es necesario todavía.

## Arranque desde una copia nueva

```bash
git clone --branch develop https://github.com/amperezp14/WorkSoft.git
cd WorkSoft/services/company-service
java --version
./mvnw --version
./mvnw verify
./mvnw spring-boot:run
```

En Windows usar `mvnw.cmd` en lugar de `./mvnw`.

En otra terminal:

```bash
curl --fail http://localhost:8081/actuator/health
curl --fail http://localhost:8081/actuator/health/liveness
curl --fail http://localhost:8081/actuator/health/readiness
```

Cada consulta debe devolver estado `UP`. La salud global incluye además los
grupos `liveness` y `readiness`. Detener con Ctrl+C.
Para cambiar el puerto: `SERVER_PORT=8082 ./mvnw spring-boot:run` (Bash).

## Estructura

- services/company-service: primer servicio, construcción Maven independiente.
- frontend: espacio para Angular, sin aplicación generada en esta fase.
- platform: documentación de infraestructura futura.
- docs/architecture: arquitectura y contexto colombiano.
- docs/adr: decisiones y consecuencias.
- scripts: espacio reservado para automatización real cuando sea necesaria.

## Límites hexagonales

HTTP → puerto de entrada → caso de uso → dominio.
El caso de uso invoca puertos de salida, implementados por adaptadores externos.
Los adaptadores pueden depender del núcleo; el núcleo no depende de ellos.

Los paquetes domain, application, adapter y configuration están documentados con
package-info.java. No hay controladores ni repositorios ficticios. El primer caso
funcional será CreateCompany, después de definir requisitos e aislamiento.

## Verificación

`./mvnw verify` ejecuta la prueba de contexto Spring y tres reglas ArchUnit:
independencia del dominio, independencia de aplicación y aislamiento del arranque.
La validación HTTP comprueba el artefacto ejecutable y sus tres probes de salud.
No hay aún pruebas de negocio porque no hay comportamiento de negocio.

## Alcance

Sin base de datos, usuarios, Angular, contenedores, pipeline Jenkins ni despliegue.
No exponer a Internet como aplicación empresarial: falta implementar seguridad.
Los siguientes módulos deben seguir docs/architecture/colombia.md.
