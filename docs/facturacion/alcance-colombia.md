# Facturación comercial interna — Colombia

Consulta de fuentes: 2026-11-17. Ámbito: empresas comerciales y de servicios.

Fuentes oficiales:
- [DIAN, Resolución 000227 de 2025](https://www.dian.gov.co/normatividad/Normatividad/Resoluci%C3%B3n%20000227%20de%2023-09-2025.pdf), Parte 1, Título 5: sistema de facturación, generación, transmisión y validación de documentos electrónicos. Deben considerarse sus modificaciones y anexos al integrar DIAN.
- [DIAN, concepto 017077 de 2026](https://normograma.dian.gov.co/dian/compilacion/docs/oficio_dian_17077_2026.htm): la validación técnica no sustituye la responsabilidad sobre el contenido de los documentos.
- [Contexto colombiano de WorkSoft](../architecture/colombia.md).

Decisión de este incremento: documentos comerciales internos en COP, sin afirmar
cumplimiento de la obligación de facturar. No hay XML UBL, firma, CUFE/CUDE,
numeración autorizada, transmisión DIAN, contingencia ni entrega electrónica.
La interfaz y la impresión deben identificar expresamente este alcance.

El usuario define tratamiento y tarifa de IVA por concepto (gravado, exento o
excluido); el programa no determina la responsabilidad tributaria del emisor ni
la clasificación legal del bien. Una tarifa cero gravada no equivale a exento o
excluido. No aplicar impuestos predeterminados ni retenciones/ICA automáticos.
Se admite tarifa configurable entre 0 y 100 con dos decimales, para no fijar en
código una tabla fiscal supuestamente universal. Su aplicabilidad y vigencia
requieren definición contable por la empresa; cada documento conserva la tarifa.

Política de cálculo del producto: cantidad con tres decimales, precio y porcentajes
con dos; redondeo HALF_UP a dos decimales por línea en bruto, descuento e IVA;
totales como suma de importes ya redondeados. Esta política es interna y tendrá
que contrastarse con el anexo técnico al incorporar emisión electrónica.

No incorporar datos reales hasta habilitar autenticación y autorización operativa.
Revisión profesional fiscal y contable del uso productivo: pendiente.
