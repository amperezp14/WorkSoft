import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap } from 'rxjs';
import { AuthService } from './auth.service';
import { RuntimeConfigService } from './runtime-config';
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const config = inject(RuntimeConfigService);
  const auth = inject(AuthService);
  const target = new URL(request.url, window.location.origin);
  const api = new URL(config.config.apiBaseUrl, window.location.origin);
  if (
    target.origin !== api.origin ||
    !(target.pathname === api.pathname || target.pathname.startsWith(api.pathname + '/'))
  ) {
    return next(request);
  }
  return from(auth.accessToken()).pipe(
    switchMap((token) =>
      next(
        request.clone({
          setHeaders: { Authorization: 'Bearer ' + token },
        }),
      ),
    ),
  );
};
