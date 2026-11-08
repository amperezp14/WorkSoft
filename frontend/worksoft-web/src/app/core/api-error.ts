import { HttpErrorResponse } from '@angular/common/http';
export function apiErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message === 'SESSION_EXPIRED')
    return 'Tu sesión venció. Inicia sesión para continuar.';
  if (!(error instanceof HttpErrorResponse))
    return 'No pudimos completar la operación. Inténtalo de nuevo.';
  if (error.status === 0)
    return 'No hay conexión con el servidor. Comprueba tu conexión y vuelve a intentarlo.';
  switch (error.error?.code) {
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
