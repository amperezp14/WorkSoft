import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CompanyContext } from '../workspace/company-context';
@Component({
  selector: 'app-navegacion-facturacion',
  imports: [RouterLink, RouterLinkActive],
  template: `<nav class="billing-nav no-print" aria-label="Facturación">
    @for (opcion of opciones; track opcion.ruta) {
      <a
        [routerLink]="['/empresas', contexto.company()!.id, 'facturacion', opcion.ruta]"
        routerLinkActive="selected"
        ariaCurrentWhenActive="page"
        >{{ opcion.nombre }}</a
      >
    }
  </nav>`,
})
export class NavegacionFacturacion {
  readonly contexto = inject(CompanyContext);
  readonly opciones = [
    { ruta: 'facturas-de-venta', nombre: 'Ventas' },
    { ruta: 'clientes', nombre: 'Clientes' },
    { ruta: 'productos-y-servicios', nombre: 'Productos y servicios' },
    { ruta: 'notas', nombre: 'Notas' },
    { ruta: 'cartera', nombre: 'Cartera' },
    { ruta: 'configuracion', nombre: 'Configuración' },
  ];
}
