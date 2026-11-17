import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CompanyContext } from '../workspace/company-context';
@Component({
  selector: 'app-navegacion-nomina',
  imports: [RouterLink, RouterLinkActive],
  template: `<nav class="billing-nav no-print" aria-label="Nómina">
    @for (opcion of opciones; track opcion.ruta) {
      <a
        [routerLink]="['/empresas', contexto.company()!.id, 'nomina', opcion.ruta]"
        routerLinkActive="selected"
        ariaCurrentWhenActive="page"
        >{{ opcion.nombre }}</a
      >
    }
  </nav>`,
})
export class NavegacionNomina {
  readonly contexto = inject(CompanyContext);
  readonly opciones = [
    { ruta: 'empleados', nombre: 'Empleados' },
    { ruta: 'contratos', nombre: 'Contratos' },
    { ruta: 'parametros', nombre: 'Parámetros y vigencias' },
    { ruta: 'periodos', nombre: 'Liquidaciones y novedades' },
  ];
}
