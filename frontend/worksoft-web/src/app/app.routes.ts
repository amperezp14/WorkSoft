import { Routes } from '@angular/router';
const modulePage = () => import('./features/workspace/module-page').then((m) => m.ModulePage);
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
      import('./features/workspace/company-workspace').then((m) => m.CompanyWorkspace),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/workspace/module-selector').then((m) => m.ModuleSelector),
      },
      {
        path: 'informacion',
        loadComponent: () =>
          import('./features/companies/company-detail-page').then((m) => m.CompanyDetailPage),
      },
      { path: 'contabilidad', loadComponent: modulePage, data: { module: 'contabilidad' } },
      {
        path: 'contabilidad/:section',
        loadComponent: modulePage,
        data: { module: 'contabilidad' },
      },
      { path: 'facturacion', loadComponent: modulePage, data: { module: 'facturacion' } },
      { path: 'facturacion/:section', loadComponent: modulePage, data: { module: 'facturacion' } },
      { path: '**', redirectTo: '' },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'empresas' },
  { path: '**', redirectTo: 'empresas' },
];
