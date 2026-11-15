import { Routes } from '@angular/router';
export const routes: Routes = [
  {
    path: 'empresas',
    loadComponent: () => import('./features/companies/companies-page').then((m) => m.CompaniesPage),
  },
  {
    path: 'empresas/nueva',
    loadComponent: () =>
      import('./features/companies/create-company-page').then((m) => m.CreateCompanyPage),
  },
  {
    path: 'empresas/:id',
    loadComponent: () =>
      import('./features/companies/company-detail-page').then((m) => m.CompanyDetailPage),
  },
  { path: '', pathMatch: 'full', redirectTo: 'empresas' },
  { path: '**', redirectTo: 'empresas' },
];
