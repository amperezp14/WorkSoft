# Arquitectura actual y evolución

## Arranque actual

Angular → MemoryCompanies (memoria del navegador). Basta npm start.
Opcionalmente Angular mode=api → company-service local → InMemoryCompanyStore.
Este perfil de Java no utiliza base de datos ni proveedor de identidad.

Los datos se pierden al recargar el navegador en demo o al reiniciar Java en api.
La interfaz informa de ello. El contexto local único permite explorar las funciones;
no representa autorización multiusuario ni aislamiento de producción.

## Próximas etapas

1. Persistencia de empresas mediante el puerto de salida y PostgreSQL.
2. Acceso propio con usuarios almacenados en BD: hash seguro, sesiones,
   recuperación y permisos por empresa. No se requiere un proveedor externo.
3. Catálogo, inventario, ventas, facturación y pagos según procesos colombianos.
4. Comunicación entre servicios, eventos, despliegue y observabilidad al necesitarlos.

Cada servicio futuro será propietario de sus datos; se conservarán los límites
hexagonales y el contexto colombiano. La implementación PostgreSQL anterior y su
seguridad OIDC permanecen aisladas bajo connected como referencia histórica,
sin participar del arranque actual. Ver ADR 0005 para la decisión que sustituye
el requisito previo de Keycloak.
