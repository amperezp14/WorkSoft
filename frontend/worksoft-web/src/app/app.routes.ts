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
      {
        path: 'contabilidad/plan-de-cuentas',
        loadComponent: () =>
          import('./features/accounting/accounts-page').then((m) => m.AccountsPage),
      },
      {
        path: 'contabilidad/terceros',
        loadComponent: () => import('./features/parties/parties-page').then((m) => m.PartiesPage),
      },
      {
        path: 'contabilidad/comprobantes',
        canDeactivate: [
          (component: import('./features/journals/journals-page').JournalsPage) =>
            component.canLeave(),
        ],
        loadComponent: () =>
          import('./features/journals/journals-page').then((m) => m.JournalsPage),
      },
      { path: 'contabilidad', loadComponent: modulePage, data: { module: 'contabilidad' } },
      {
        path: 'contabilidad/:section',
        loadComponent: modulePage,
        data: { module: 'contabilidad' },
      },
      {
        path: 'facturacion/facturas-de-venta',
        canDeactivate: [
          (componente: import('./features/facturacion/documentos-pagina').DocumentosPagina) =>
            componente.canLeave(),
        ],
        loadComponent: () =>
          import('./features/facturacion/documentos-pagina').then((m) => m.DocumentosPagina),
      },
      {
        path: 'facturacion/notas',
        data: { notas: true },
        canDeactivate: [
          (componente: import('./features/facturacion/documentos-pagina').DocumentosPagina) =>
            componente.canLeave(),
        ],
        loadComponent: () =>
          import('./features/facturacion/documentos-pagina').then((m) => m.DocumentosPagina),
      },
      {
        path: 'facturacion/clientes',
        data: { clientes: true },
        loadComponent: () => import('./features/parties/parties-page').then((m) => m.PartiesPage),
      },
      {
        path: 'facturacion/productos-y-servicios',
        loadComponent: () =>
          import('./features/facturacion/productos-pagina').then((m) => m.ProductosPagina),
      },
      {
        path: 'facturacion/configuracion',
        loadComponent: () =>
          import('./features/facturacion/configuracion-pagina').then((m) => m.ConfiguracionPagina),
      },
      {
        path: 'facturacion/cartera',
        loadComponent: () =>
          import('./features/facturacion/cartera-pagina').then((m) => m.CarteraPagina),
      },
      { path: 'facturacion', loadComponent: modulePage, data: { module: 'facturacion' } },
      { path: 'facturacion/:section', loadComponent: modulePage, data: { module: 'facturacion' } },
      { path: 'nomina', redirectTo: 'nomina/periodos', pathMatch: 'full' },
      {
        path: 'nomina/empleados',
        data: { contratos: false },
        loadComponent: () =>
          import('./features/nomina/personal-pagina').then((m) => m.PersonalPagina),
      },
      {
        path: 'nomina/contratos',
        data: { contratos: true },
        loadComponent: () =>
          import('./features/nomina/personal-pagina').then((m) => m.PersonalPagina),
      },
      {
        path: 'nomina/parametros',
        loadComponent: () =>
          import('./features/nomina/parametros-pagina').then((m) => m.ParametrosPagina),
      },
      {
        path: 'nomina/periodos',
        canDeactivate: [
          (componente: import('./features/nomina/periodos-pagina').PeriodosPagina) =>
            componente.canLeave(),
        ],
        loadComponent: () =>
          import('./features/nomina/periodos-pagina').then((m) => m.PeriodosPagina),
      },
      { path: '**', redirectTo: '' },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'empresas' },
  { path: '**', redirectTo: 'empresas' },
];
