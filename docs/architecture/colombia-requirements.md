# Registro inicial de requisitos colombianos

Este registro identifica trabajo de validación. Las referencias son puntos de
entrada oficiales; no se ha realizado una revisión jurídica ni verificado en esta
fase el contenido vigente de cada página. No se implementan tarifas, fórmulas ni
plazos legales a partir de este registro.

| Tema | Decisión o trabajo pendiente | Fuente oficial inicial | Estado |
| --- | --- | --- | --- |
| Identidad tributaria | NIT y DV como texto separado; confirmar reglas de validación con fuentes DIAN antes de implementar. | [DIAN / RUT](https://www.dian.gov.co/impuestos/RUT/Paginas/Inicio.aspx) | Validación normativa pendiente. |
| Facturación | Definir obligación, habilitación, anexos técnicos, numeración y estados para el contribuyente concreto. | [DIAN / Factura electrónica](https://www.dian.gov.co/impuestos/factura-electronica/Paginas/inicio.aspx) | Fuera del primer flujo. |
| Protección de datos | Definir finalidades, responsabilidades, autorizaciones aplicables, derechos, encargados, acceso y conservación. | [Ley 1581 de 2012](https://www.secretariasenado.gov.co/senado/basedoc/ley_1581_2012.html), [SIC](https://www.sic.gov.co/) | Revisión antes de operar con datos reales. |
| Ubicación | Usar catálogo territorial oficial cuando se añadan domicilios. | [DANE](https://www.dane.gov.co/) | Catálogo y actualización pendientes. |
| Impuestos nacionales y locales | Reglas por vigencia, operación, régimen y municipio; sin tasas constantes en código. | [DIAN / Normatividad](https://www.dian.gov.co/normatividad/Paginas/default.aspx) y autoridades territoriales correspondientes | Fuera del primer flujo. |
| Contabilidad y nómina | Validar marcos aplicables y legislación vigente cuando se diseñen estos módulos. | [MinTrabajo](https://www.mintrabajo.gov.co/), [MinHacienda](https://www.minhacienda.gov.co/) | Fuera del primer flujo. |

## Alcance técnico del registro de empresa

El primer contrato recoge únicamente nombre legal, NIT y DV de personas jurídicas.
CO, COP y America/Bogota son constantes iniciales de producto; no se aceptan
valores arbitrarios del cliente. No se solicita copia del RUT, información de
representantes, teléfonos, datos bancarios ni direcciones en esta fase.

Los límites de tamaño y patrones del contrato son restricciones técnicas del
producto, no afirmaciones sobre todas las identificaciones admitidas en Colombia.
Aceptar un formato no demuestra que el NIT exista ni que la empresa esté habilitada
por DIAN. La validación aritmética del DV requiere confirmar y documentar su fuente
antes de implementarla. No se consultan registros oficiales automáticamente.

Cada futura regla normativa tendrá identificador, fuente precisa, artículo o anexo,
fecha de consulta, vigencia desde/hasta, ámbito de aplicación, interpretación,
revisor y casos de prueba. Los importes usarán decimal exacto; redondeos, conservación
y fechas fiscales se definirán por operación, sin trasladar supuestos genéricos.
