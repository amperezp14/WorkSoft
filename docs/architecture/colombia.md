# Contexto colombiano obligatorio

Todas las fases de WorkSoft deben considerar empresas que operan en Colombia.
Este documento fija requisitos de diseño; no certifica cumplimiento normativo.

## Localización y datos

- Interfaz y mensajes funcionales en español de Colombia (es-CO).
- COP como moneda predeterminada. Importes con BigDecimal y moneda explícita;
  no usar float/double para dinero. Escala y redondeo según cada operación.
- America/Bogota para fechas de negocio y visualización. Instantes técnicos
  en UTC; fechas civiles sin conversión implícita. No forzar una zona global JVM.
- NIT y dígito de verificación separados; no usar valores numéricos que eliminen
  ceros iniciales de identificaciones. Admitir tipos documentales aplicables.
- Departamentos y municipios con códigos oficiales DANE cuando se implementen.
- Empresas y establecimientos se modelarán tras definir su alcance legal.

## Requisitos por validar antes de cada módulo

- DIAN: RUT, responsabilidades tributarias, facturación electrónica, notas,
  documento soporte y nómina electrónica, cuando sean aplicables.
- Impuestos: IVA, retenciones e ICA, según régimen, operación y municipio.
- Contabilidad: marcos técnicos normativos aplicables a la empresa.
- Nómina: legislación laboral, prestaciones, seguridad social y PILA.
- Datos personales: Ley 1581 de 2012, reglamentación y lineamientos de la SIC;
  finalidades, autorizaciones o bases aplicables, derechos y conservación.

Las tarifas, topes y reglas variables tendrán vigencia desde/hasta y fuente
normativa. No se modificarán documentos históricos por cambios posteriores.
Definir aislamiento entre empresas, auditoría y mínimos datos personales antes
de abrir endpoints de negocio. Evitar datos sensibles en logs y trazas.

Registrar en cada decisión normativa: fuente oficial, fecha de consulta, vigencia,
condiciones de aplicación y revisión profesional fiscal, contable o laboral.
No se han implementado cálculos tributarios ni integraciones DIAN en la fase 1.
