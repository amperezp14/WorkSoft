import { Routes } from '@angular/router';
import { authenticatedGuard, provisionGuard } from './core/auth.guard';
export const routes: Routes = [
  { path: 'inicio', loadComponent: () => import('./welcome').then((m) => m.Welcome) },
  {
    path: 'empresas',
    canActivate: [authenticatedGuard],
    loadComponent: () => import('./features/companies/companies-page').then((m) => m.CompaniesPage),
  },
  {
    path: 'empresas/nueva',
    canActivate: [authenticatedGuard, provisionGuard],
    loadComponent: () =>
      import('./features/companies/create-company-page').then((m) => m.CreateCompanyPage),
  },
  {
    path: 'empresas/:id',
    canActivate: [authenticatedGuard],
    loadComponent: () =>
      import('./features/companies/company-detail-page').then((m) => m.CompanyDetailPage),
  },
  { path: '', pathMatch: 'full', redirectTo: 'empresas' },
  { path: '**', redirectTo: 'empresas' },
];
