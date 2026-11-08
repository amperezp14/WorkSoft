# ADR 0004 — Frontend Angular para empresas

Estado: implementado por prioridad del usuario antes del catálogo/inventario.

## Decisiones

- Angular 21 LTS con componentes standalone, Signals y rutas lazy; Node.js 24 LTS.
- Organización por core (identidad, HTTP, configuración), features/companies y shared.
- Contratos TypeScript alineados con OpenAPI; nunca convertir NIT/DV a number.
- Servicio HTTP tipado; componentes contienen estado de presentación y formulario.
- Keycloak JS con Authorization Code + PKCE S256, cliente público sin secreto.
- Tokens solo en memoria; el SDK verifica el estado de la redirección OAuth.
- Guardas de navegación para experiencia de usuario; el backend sigue validando
  cada petición. Ocultar el botón no se considera autorización de datos.
- Interceptor restringido al origen y ruta base de API; sin reintentos automáticos
  de POST. El formulario conserva la misma clave para recuperar un resultado incierto.
- Sin modo demo, datos empresariales inventados en pantalla ni bypass de identidad.
- Configuración pública al iniciar; proxy local evita ampliar CORS del backend.
- Español es-CO, COP como contexto de moneda y fechas por Intl con America/Bogota.
- Estilos propios adaptables, foco visible, etiquetas y estados anunciados.

## Alcance

Acceso, listado paginado, filtro local, registro y detalle. Sin módulos futuros,
roles inventados, persistencia de tokens ni gestión administrativa de membresías.
El cambio de cliente Keycloak necesita importación manual si el realm ya existe;
no se elimina ni recrea el volumen para aplicar esta actualización.
