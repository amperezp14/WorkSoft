import { CompanyContext } from '../workspace/company-context';
import { Component, inject } from '@angular/core';
import { bogotaDate } from '../../core/api-error';
import { Icon } from '../../shared/icon';
@Component({
  selector: 'app-company-detail-page',
  imports: [Icon],
  template: `@if (company(); as c) {
    <div class="page-heading">
      <div>
        <span class="eyebrow">INFORMACIÓN GENERAL</span>
        <h2>Datos de la empresa</h2>
        <p>NIT {{ c.nit }}-{{ c.verificationDigit }}</p>
      </div>
      <span class="status"><span></span> Activa en WorkSoft</span>
    </div>
    <div class="form-layout">
      <section class="panel detail-panel">
        <div class="form-intro">
          <span class="summary-icon"><app-icon /></span>
          <div>
            <h2>Datos de la empresa</h2>
            <p>La base de tu espacio de gestión.</p>
          </div>
        </div>
        <dl class="detail-list">
          <div>
            <dt>Razón social</dt>
            <dd>{{ c.legalName }}</dd>
          </div>
          <div>
            <dt>NIT</dt>
            <dd>{{ c.nit }}-{{ c.verificationDigit }}</dd>
          </div>
          <div>
            <dt>País</dt>
            <dd>Colombia</dd>
          </div>
          <div>
            <dt>Moneda</dt>
            <dd>Peso colombiano (COP)</dd>
          </div>
          <div>
            <dt>Zona horaria</dt>
            <dd>{{ c.timeZone }}</dd>
          </div>
          <div>
            <dt>Fecha de registro · Colombia</dt>
            <dd>{{ date(c.createdAt) }}</dd>
          </div>
        </dl>
      </section>
      <aside class="context-panel">
        <app-icon name="lock" />
        <h2>Datos guardados</h2>
        <p>
          Los datos de esta empresa se guardan en PostgreSQL y permanecen al cerrar la aplicación.
        </p>
        <p class="muted">
          “Activa” describe su estado dentro de WorkSoft y no acredita su situación jurídica o
          tributaria ante la DIAN.
        </p>
      </aside>
    </div>
  }`,
})
export class CompanyDetailPage {
  readonly company = inject(CompanyContext).company;
  readonly date = bogotaDate;
}
