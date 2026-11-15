import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { COMPANY_MODULES } from './modules';
import { CompanyContext } from './company-context';
import { Icon } from '../../shared/icon';
@Component({
  selector: 'app-module-page',
  imports: [RouterLink, RouterLinkActive, Icon],
  template: ` <div class="module-intro">
      <span class="module-icon" [class.billing]="module.path === 'facturacion'"
        ><app-icon [name]="module.icon"
      /></span>
      <div>
        <h2>{{ module.title }}</h2>
        <p>{{ module.description }}</p>
      </div>
      <span class="module-stage">En preparación</span>
    </div>
    <div class="module-layout">
      <nav
        class="panel module-menu"
        [attr.aria-label]="'Menú de ' + module.title.toLocaleLowerCase('es-CO')"
      >
        <span class="eyebrow">OPCIONES DEL MÓDULO</span>
        @for (section of module.sections; track section.id) {
          <a
            [routerLink]="['/empresas', context.company()!.id, module.path, section.id]"
            routerLinkActive="selected"
            ariaCurrentWhenActive="page"
            ><span>{{ section.title }}</span
            ><app-icon name="arrow"
          /></a>
        }
      </nav>
      <section class="panel module-content">
        @if (selected(); as section) {
          <span class="eyebrow">{{ module.title }}</span>
          <h3>{{ section.title }}</h3>
          <p>{{ section.description }}</p>
          <div class="module-placeholder">
            <span class="summary-icon"><app-icon [name]="module.icon" /></span>
            <h4>Este espacio será el siguiente paso</h4>
            <p>
              La opción ya tiene su lugar en el menú. Su registro y consulta estarán disponibles
              cuando desarrollemos esta sección.
            </p>
          </div>
        } @else if (params().get('section')) {
          <h3>Opción no disponible</h3>
          <p>Selecciona una de las opciones del menú para continuar.</p>
        } @else {
          <span class="eyebrow">COMIENZA AQUÍ</span>
          <h3>Todo en su lugar</h3>
          <p>
            Selecciona una opción del menú de {{ module.title.toLocaleLowerCase('es-CO') }} para
            explorar el espacio de trabajo.
          </p>
          <div class="module-options">
            @for (section of module.sections; track section.id) {
              <a [routerLink]="['/empresas', context.company()!.id, module.path, section.id]"
                ><strong>{{ section.title }}</strong
                ><span>{{ section.description }}</span
                ><app-icon name="arrow"
              /></a>
            }
          </div>
        }
        <p class="module-scope">
          {{
            module.path === 'facturacion'
              ? 'La emisión electrónica y la validación ante la DIAN se incorporarán en una etapa posterior.'
              : 'El registro de comprobantes, los cálculos y los informes se incorporarán en una etapa posterior.'
          }}
        </p>
      </section>
    </div>`,
})
export class ModulePage {
  private readonly route = inject(ActivatedRoute);
  readonly context = inject(CompanyContext);
  readonly module = COMPANY_MODULES.find(
    (module) => module.path === this.route.snapshot.data['module'],
  )!;
  readonly params = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });
  readonly selected = computed(() =>
    this.module.sections.find((section) => section.id === this.params().get('section')),
  );
}
