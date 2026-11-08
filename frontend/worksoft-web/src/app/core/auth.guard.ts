import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
export const authenticatedGuard: CanActivateFn = () =>
  inject(AuthService).authenticated() || inject(Router).createUrlTree(['/inicio']);
export const provisionGuard: CanActivateFn = () =>
  inject(AuthService).canProvision() || inject(Router).createUrlTree(['/empresas']);
