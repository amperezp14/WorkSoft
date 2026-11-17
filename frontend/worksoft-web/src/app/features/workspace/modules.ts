export interface ModuleSection {
  id: string;
  title: string;
  description: string;
}
export interface CompanyModule {
  path: string;
  title: string;
  icon: string;
  description: string;
  sections: readonly ModuleSection[];
}
export const COMPANY_MODULES: readonly CompanyModule[] = [
  {
    path: 'contabilidad',
    title: 'Contabilidad',
    icon: 'ledger',
    description: 'Organiza las cuentas, los movimientos y la información contable de tu empresa.',
    sections: [
      {
        id: 'plan-de-cuentas',
        title: 'Plan de cuentas',
        description: 'Consulta y organiza el catálogo de cuentas de la empresa.',
      },
      {
        id: 'comprobantes',
        title: 'Comprobantes contables',
        description: 'Prepara el espacio para registrar y consultar asientos contables.',
      },
      {
        id: 'terceros',
        title: 'Terceros',
        description: 'Organiza la identificación de personas y empresas relacionadas.',
      },
      {
        id: 'informes',
        title: 'Informes contables',
        description: 'Accede al espacio de balances, auxiliares y movimientos.',
      },
    ],
  },
  {
    path: 'facturacion',
    title: 'Facturación',
    icon: 'invoice',
    description: 'Reúne la gestión comercial y los documentos de venta en un mismo lugar.',
    sections: [
      {
        id: 'facturas-de-venta',
        title: 'Facturas de venta',
        description: 'Crea ventas internas, registra recaudos y consulta su historial.',
      },
      {
        id: 'clientes',
        title: 'Clientes',
        description: 'Organiza la información comercial de tus clientes.',
      },
      {
        id: 'productos-y-servicios',
        title: 'Productos y servicios',
        description: 'Reúne los conceptos que tu empresa ofrece a sus clientes.',
      },
      {
        id: 'notas',
        title: 'Notas crédito y débito',
        description: 'Crea notas débito y revierte documentos con notas crédito internas.',
      },
      {
        id: 'cartera',
        title: 'Cartera y recaudos',
        description: 'Consulta saldos, documentos vencidos y pagos de tus clientes.',
      },
      {
        id: 'configuracion',
        title: 'Configuración',
        description: 'Configura consecutivos internos y contabilización automática.',
      },
    ],
  },
];
