# Validación de menús por empresa

- Compilación Angular de producción correcta, sin advertencias de presupuesto.
- 10 pruebas unitarias existentes correctas.
- 6 pruebas de navegador correctas, incluyendo 3 nuevas para el espacio de empresa.
- Verificado el flujo crear/abrir empresa → selector → Contabilidad → Plan de
  cuentas → Facturación → Facturas de venta → Datos de la empresa → selector.
- Verificado cambio de empresa: todas las rutas de menú contienen el nuevo UUID.
- Al volver al listado se elimina el contexto y desaparece el menú de empresa.
- Un UUID inexistente no muestra módulos ni información de una empresa anterior.
- Capturas revisadas en escritorio y selector móvil de 390 px, sin desbordamiento
  horizontal de página. Artefactos visuales excluidos de Git.

Se mantienen datos en memoria y acceso local. No se modificó el backend ni se
implementaron asientos, impuestos, documentos fiscales o integraciones DIAN.
