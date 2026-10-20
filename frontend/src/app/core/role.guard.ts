import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './models';

export const roleGuard = (allowed: Role[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.role && allowed.includes(auth.role)) {
    return true;
  }
  return router.createUrlTree(['/']);
};
