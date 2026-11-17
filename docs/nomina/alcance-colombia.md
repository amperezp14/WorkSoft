# Nómina ordinaria mensual — fuentes y alcance

Consulta: 2026-11-17. Primera etapa: trabajadores dependientes del sector privado,
salario ordinario, jornada completa y contrato que cubra el mes completo.

Fuentes oficiales de referencia (revisar vigencia al usar):
- [Decreto 0159 de 2026](https://dapre.presidencia.gov.co/normativa/normativa/DECRETO%20No.%200159%20DEL%2019%20DE%20FEBRERO%20DE%202026.pdf): fijación transitoria del SMMLV 2026 en COP 1.750.905; no se presume una regla permanente para años posteriores.
- [Decreto 1470 de 2025](https://cancilleria.gov.co/normograma/compilacion/docs/decreto_1470_2025.htm): auxilio de transporte 2026, COP 249.095, y condiciones de acceso.
- [Ministerio de Salud, cotización al régimen contributivo](https://www2.minsalud.gov.co/Lists/FAQ/DispForm.aspx?ID=15): aporte del trabajador a salud del 4 %.
- [Ministerio de Salud, sistema general de pensiones](https://www2.minsalud.gov.co/salud/Documents/Contenidos/sistema-general-pensiones.aspx): aporte pensional y contribuciones adicionales.
- [Decreto 3771 de 2007](https://www.funcionpublica.gov.co/eva/gestornormativo/norma.php?i=26892): tramos adicionales de solidaridad.
- [UGPP, ABC Empleadores 2026](https://www.ugpp.gov.co/wp-content/uploads/2026/01/ABC-Empleadores-2026.pdf): clasificación de pagos, novedades y bases de aportes.

Decisión: la empresa debe registrar vigencias, fuente y responsable de revisión
antes de calcular. Las referencias de 2026 del formulario no se aplican hasta
que se guarden y se declare su revisión. El motor aplica la estrategia explícita
ORDINARIA_LEY100_V1; no determina afiliación, régimen pensional o exoneraciones.
Salud y pensión del trabajador se parametrizan; la solidaridad sigue los tramos
de la estrategia identificada en la instantánea del cálculo.

Solo se admiten novedades monetarias salariales (comisión/bonificación) y
retenciones o deducciones ya determinadas y documentadas. No se clasifican pagos
no salariales arbitrarios ni se calcula su incorporación al IBC. No se calculan
horas extra, recargos, incapacidades, licencias, vacaciones, ingresos/retiros
parciales, salarios integrales, aprendices, pensionados, prestaciones sociales,
aportes patronales, parafiscales ni retención tributaria automática.

El cálculo entrega el neto del trabajador y deducciones estimadas, con redondeo
HALF_UP a dos decimales para el documento interno. No equivale a liquidación PILA
ni nómina electrónica DIAN; no transmite aportes ni realiza transferencias.
La revisión profesional laboral, pensional y fiscal del uso operativo está pendiente.
El tratamiento mínimo de datos se rige por el contexto colombiano del repositorio;
no introducir datos reales en el espacio local compartido sin autenticación.
