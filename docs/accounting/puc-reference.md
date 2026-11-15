# Referencia contable comercial — colección 2026

Ámbito confirmado por el usuario: empresa comercial o de servicios general.
Consulta de fuentes: 16 de noviembre de 2026.

## Base y precisión de alcance

La colección incluye 108 clases, grupos, cuentas y subcuentas seleccionados del
catálogo del Decreto 2650 de 1993, con sus ascendientes. Es una carga inicial parcial,
no una transcripción completa ni un catálogo legal nuevo llamado PUC 2026.
El año 2026 identifica esta colección del software y su revisión, no la fecha del decreto.

[Texto oficial compilado — CREG, Decreto 2650 de 1993](https://gestornormativo.creg.gov.co/gestor/entorno/docs/decreto_2650_1993.htm).
Se contrastaron códigos/nombres con el catálogo y la estructura con los artículos
7 y 14: clase, grupo, cuenta, subcuenta y posibilidad de auxiliares.

El [Concepto CTCP 2024-0061, documento del Consejo alojado por INCP](https://incp.org.co/wp-content/uploads/2024/05/Concepto-No.-0061-del-2024-%E2%80%93-CTCP.pdf)
explica la facultad de definir el catálogo de la entidad según su marco técnico y
reconoce que ciertos supervisores establecen catálogos específicos. También indica
que el CTCP no es competente para pronunciarse sobre la vigencia del decreto.
Por tanto, no se afirma aquí su derogatoria ni su obligatoriedad universal bajo NIIF.

La entidad debe adecuar el catálogo a sus políticas y marco aplicable. No se trata
del catálogo de entidades financieras, aseguradoras, cooperativas o del sector público.

## Datos y reglas del producto

puc-reference-2026.csv contiene todos los registros cargados por la migración V3.
Los nombres conservan la denominación del texto consultado, retirando las notas
editoriales de modificación que aparecen intercaladas en la compilación. La naturaleza es una
configuración inicial editable; se contemplan correctoras seleccionadas, por ejemplo
1592 (crédito) y 4175 (débito), y pérdidas 3610/3710 (débito). No se infiere que todas
las cuentas de una clase deban tener siempre la misma naturaleza.

La aplicación admite auxiliares de 8 y 10 dígitos como decisión técnica inicial;
la norma consultada permite auxiliares desde el séptimo dígito, sin imponer esos
dos tamaños como únicos. No se cargan cuentas antiguas de ajustes por inflación
como modelo de reconocimiento actual ni se generan saldos o transacciones fiscales.

Cada empresa recibe una copia editable. El catálogo global queda como referencia.
Los registros iniciales son de clasificación, con movimientos desactivados para
permitir su subdivisión. Activar movimientos exige que no existan hijas y al menos
nivel subcuenta. Las cuentas inactivas mantienen su historia y su código reservado.

Los códigos quedan fijos al editar, el padre se deriva del código, y una FK compuesta
impide referencias entre empresas. La versión evita perder cambios concurrentes.
Las reglas de jerarquía se validan transaccionalmente; no implementan aún asientos,
partida doble, cierres, cálculos tributarios ni estados financieros NIIF.
