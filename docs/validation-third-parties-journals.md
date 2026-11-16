# Validación: terceros y comprobantes

Fecha: 2026-11-16. Datos sintéticos, PostgreSQL real efímero; sin base operativa.

- Maven Wrapper y JDK 25: 46 pruebas, sin errores. Incluyen aislamiento por empresa,
  duplicados, control de versiones, idempotencia, equilibrio, referencias inactivas,
  conservación histórica, bloqueo de edición, concurrencia de consecutivos, reinicio
  anual y restricciones SQL sobre documentos contabilizados.
- Angular: 14 pruebas unitarias; importes exactos, servicios HTTP y regresiones.
- Compilación de producción Angular correcta.
- Playwright/Chromium: 8 recorridos sobre API y PostgreSQL reales. Incluyen crear y
  editar tercero, guardar borrador desbalanceado, corregir, contabilizar, persistir
  tras recarga, vista de solo lectura, pantalla móvil y generación de PDF.

Los resultados no equivalen a validación profesional del cierre contable o fiscal.
