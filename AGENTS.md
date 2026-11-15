# WorkSoft

- Diseñar todos los módulos para el entorno empresarial y las leyes colombianas.
- Consultar docs/architecture/colombia.md y registrar fuentes y vigencias antes
  de implementar requisitos fiscales, contables, laborales o de datos personales.
- Trabajar por fases: no añadir módulos fuera de la fase solicitada.
- Mantener dominio y aplicación independientes de frameworks y adaptadores.
- Verificar company-service con su Maven Wrapper y JDK 25.
- No guardar credenciales, datos personales reales ni archivos de compilación.

- El arranque predeterminado es local, sin BD ni Keycloak. Angular mode=demo
  debe funcionar con npm start solamente; Java local debe arrancar sin secretos.
- La futura autenticación propia usará BD; no introducir proveedores externos
  sin una nueva instrucción del usuario.
