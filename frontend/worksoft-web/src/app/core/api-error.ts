import { HttpErrorResponse } from '@angular/common/http';
export function apiErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message === 'SESSION_EXPIRED')
    return 'Tu sesión venció. Inicia sesión para continuar.';
  if (!(error instanceof HttpErrorResponse))
    return 'No pudimos completar la operación. Inténtalo de nuevo.';
  if (error.status === 0)
    return 'No hay conexión con el servidor. Comprueba tu conexión y vuelve a intentarlo.';
  switch (error.error?.code) {
    case 'FACTURACION_NO_ENCONTRADA':
      return 'El registro de facturación no está disponible en esta empresa.';
    case 'PRODUCTO_DUPLICADO':
      return 'Ya existe un concepto con ese código en esta empresa.';
    case 'CONFIGURACION_FACTURACION_INVALIDA':
      return 'Revisa los prefijos: no pueden reutilizarse para otro tipo de documento. Si activas contabilidad, selecciona cuatro cuentas diferentes, activas, sin hijas y que permitan movimientos.';
    case 'REFERENCIA_FACTURACION_INVALIDA':
      return 'Revisa que el cliente sea activo y tenga categoría Cliente o Cliente y proveedor, los conceptos estén activos y el documento original siga confirmado.';
    case 'DOCUMENTO_NO_EDITABLE':
      return 'El documento confirmado o anulado no admite modificaciones.';
    case 'REVERSO_FACTURACION_INVALIDO':
      return 'La reversión total requiere un documento confirmado sin recaudos activos. Anula primero los recaudos.';
    case 'PAGO_FACTURACION_INVALIDO':
      return 'Revisa el saldo y la fecha: el recaudo debe ser positivo, no superar el saldo y corresponder a un documento confirmado.';
    case 'PARTY_CONFLICT':
      return 'La identificación ya está registrada en esta empresa.';
    case 'PARTY_NOT_FOUND':
      return 'El tercero no está disponible en esta empresa.';
    case 'VERSION_CONFLICT':
      return 'El registro cambió en otra edición. Actualiza antes de guardar.';
    case 'VOUCHER_NOT_FOUND':
      return 'El comprobante no está disponible en esta empresa.';
    case 'VOUCHER_POSTED':
      return 'El comprobante ya está contabilizado y no puede modificarse.';
    case 'VOUCHER_UNBALANCED':
      return 'Se requieren al menos dos líneas y débitos y créditos iguales, con total positivo.';
    case 'INVALID_JOURNAL_LINE':
      return 'Revisa cada línea: cuenta, tercero y un solo importe positivo con máximo dos decimales.';
    case 'POSTING_REFERENCE_INVALID':
      return 'Revisa las cuentas y terceros: deben ser de esta empresa, estar activos y las cuentas permitir movimientos sin tener hijas.';
    case 'ACCOUNT_CONFLICT':
      return 'Ya existe una cuenta con ese código en esta empresa.';
    case 'ACCOUNT_VERSION_CONFLICT':
      return 'Otra edición cambió esta cuenta. Actualiza la tabla antes de editarla de nuevo.';
    case 'ACCOUNT_HIERARCHY_CONFLICT':
      return 'Revisa la cuenta superior: debe existir, estar activa y no permitir movimientos. Una cuenta con hijas no puede recibir movimientos ni desactivarse mientras tenga hijas activas.';
    case 'ACCOUNT_NOT_FOUND':
      return 'La cuenta no está disponible en esta empresa.';
    case 'IDEMPOTENCY_CONFLICT':
      return 'Este intento ya se utilizó con otros datos. Revisa el formulario.';
    case 'COMPANY_CONFLICT':
      return 'No se puede registrar esta empresa. Comprueba el NIT o consulta al administrador.';
  }
  switch (error.status) {
    case 400:
      return 'Revisa los datos enviados. Si estabas consultando otra página, actualiza el listado.';
    case 401:
      return 'Tu sesión no es válida. Cierra la sesión e ingresa nuevamente.';
    case 403:
      return 'No tienes permiso para realizar esta operación.';
    case 404:
      return 'La empresa no está disponible o no tienes acceso a ella.';
    case 503:
      return 'El servicio no está disponible por el momento. Puedes volver a intentarlo.';
    default:
      return 'No pudimos completar la operación. Inténtalo de nuevo.';
  }
}
export function bogotaDate(value: string): string {
  return new Intl.DateTimeFormat('es-CO', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'America/Bogota',
  }).format(new Date(value));
}
