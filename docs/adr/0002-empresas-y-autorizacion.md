# ADR 0002 — Empresas, identidad y autorización

Estado: decisión inicial de diseño, revisable antes de implementar fase 3.

## Decisión

Una empresa persona jurídica colombiana equivale a un tenant identificado por
UUID. NIT único dentro de esta instalación; un establecimiento no es otro tenant.
No se fusionan empresas ni se transfieren automáticamente por conocer su NIT.
Un usuario puede pertenecer a varias empresas y sus roles son locales a cada una.

Identidad se delega a un proveedor OIDC. Se validan firma con algoritmos
permitidos, issuer, audience, expiración y demás condiciones del access token.
Se identifica al actor por (issuer, subject), nunca por email mutable.
Los permisos de empresa se resuelven contra membresías activas en company-service;
no se confía en un companyId ni roles enviados por el cliente.

| Operación | Autorización |
| --- | --- |
| Crear empresa | Permiso de plataforma company:provision, concedido fuera del autoservicio. |
| Listar empresas | Token válido; solo membresías activas propias. |
| Consultar empresa | Membresía activa ADMIN o READER; ambas tienen company:read. |
| Conceder permisos de plataforma | Administración del proveedor; fuera de la API de empresas. |

La creación otorga ADMIN al operador autenticado dentro de la misma transacción.
Ser ADMIN no concede company:provision ni acceso a otras empresas. Se necesita
un operador inicial aprovisionado por un administrador confiable, con asignación
auditada. Invitaciones, transferencia de administración y gestión de membresías
requieren un diseño y contratos posteriores; no se exponen implícitamente.

Las peticiones de detalle ajeno y las inexistentes devuelven el mismo 404 sin
información del tenant. No se distingue si falta membresía o está revocada.
El servicio aplica autorización incluso detrás de un gateway.

## Datos y capas de defensa

PostgreSQL por servicio, con company_id obligatorio en los datos de negocio y
consultas restringidas por empresa autorizada. Las claves compuestas impedirán
referencias entre empresas donde corresponda. Se probarán lecturas y escrituras
cruzadas. RLS se evaluará antes de producción como defensa adicional; no se
considera implementada ni sustituye autorización de aplicación.

Para servicios futuros, el permiso debe verificarse en cada petición: inicialmente
mediante un puerto de consulta a empresas con autenticación servicio a servicio,
validando también el actor original. Ante fallo de esa comprobación se deniega
la operación. No se usa una cabecera de identidad aportada por el navegador como
prueba. La optimización con cachés requiere decidir revocación y ventanas de riesgo.

## Consecuencias y alternativas

La consulta de permisos entre servicios añade disponibilidad y latencia a resolver
antes de desplegarlos. El modelo de un NIT único no soporta copias independientes
de la misma entidad para distintos clientes del SaaS; si ese producto es necesario,
se introducirá un tenant de cliente distinto de la entidad legal mediante otro ADR.
No se asume que una membresía autorice todas las acciones fiscales o laborales;
se definirán permisos específicos y separación de funciones al construir módulos.
