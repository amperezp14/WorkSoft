# WorkSoft

- Diseñar todos los módulos para el entorno empresarial y las leyes colombianas.
- Consultar docs/architecture/colombia.md y registrar fuentes y vigencias antes
  de implementar requisitos fiscales, contables, laborales o de datos personales.
- Trabajar por fases: no añadir módulos fuera de la fase solicitada.
- Mantener dominio y aplicación independientes de frameworks y adaptadores.
- Verificar company-service con su Maven Wrapper y JDK 25.
- No guardar credenciales, datos personales reales ni archivos de compilación.

- Por instrucción del usuario, el arranque predeterminado usa PostgreSQL y Angular
  consulta solo la API. No utilizar memoria como respaldo si falla la conexión.
- No se requiere Keycloak. La autenticación propia contra BD sigue pendiente;
  no introducir proveedores de identidad externos sin una nueva instrucción.
- La referencia PUC es parcial para comerciantes, basada en el Decreto 2650 de
  1993 y contrastada con el criterio CTCP: no presentarla como PUC oficial universal 2026.
