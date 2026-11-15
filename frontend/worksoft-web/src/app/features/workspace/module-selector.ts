import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CompanyContext } from './company-context';
import { COMPANY_MODULES } from './modules';
import { Icon } from '../../shared/icon';
@Component({
  selector: 'app-module-selector',
  imports: [RouterLink, Icon],
  template: ` <section aria-labelledby="module-selector-title">
    <div class="module-section-heading">
      <span class="eyebrow">TU OPERACIÓN, CONECTADA</span>
      <h2 id="module-selector-title">¿En qué quieres trabajar?</h2>
      <p>Selecciona un módulo para continuar con esta empresa.</p>
    </div>
    <div class="module-grid">
      @for (module of modules; track module.path) {
        <a
          class="module-card"
          [routerLink]="['/empresas', context.company()!.id, module.path]"
          [attr.aria-label]="'Abrir módulo ' + module.title"
        >
          <div class="module-card-top">
            <span class="module-icon" [class.billing]="module.path === 'facturacion'"
              ><app-icon [name]="module.icon" /></span
            ><span class="module-category">MÓDULO EMPRESARIAL</span>
          </div>
          <h3>{{ module.title }}</h3>
          <p>{{ module.description }}</p>
          <ul>
            @for (section of module.sections; track section.id) {
              <li>{{ section.title }}</li>
            }
          </ul>
          <span class="module-open"
            >Entrar a {{ module.title.toLocaleLowerCase('es-CO') }}<app-icon name="arrow"
          /></span>
        </a>
      }
    </div>
  </section>`,
})
export class ModuleSelector {
  readonly context = inject(CompanyContext);
  readonly modules = COMPANY_MODULES;
}
