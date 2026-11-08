import { Component, inject } from '@angular/core';
import { AuthService } from './core/auth.service';
import { Icon } from './shared/icon';
@Component({
  selector: 'app-welcome',
  imports: [Icon],
  template: ` <main class="welcome">
    <section class="welcome-story">
      <a class="brand" href="/">
        <span class="brand-symbol">W</span> WorkSoft<span class="brand-dot">.</span></a
      >
      <div class="welcome-copy">
        <span class="eyebrow">TU EMPRESA, CONECTADA</span>
        <h1>Más claridad.<br />Mejores decisiones.</h1>
        <p>
          Un espacio para organizar tu empresa y dar el siguiente paso, con la información donde la
          necesitas.
        </p>
        <div class="welcome-card">
          <app-icon />
          <div>
            <strong>Gestión que empieza por tu empresa</strong>
            <p>Organiza tus empresas y accede a cada una con los permisos de tu equipo.</p>
          </div>
        </div>
      </div>
      <span class="welcome-footer">Hecho para la operación empresarial en Colombia.</span>
    </section>
    <section class="welcome-access" aria-labelledby="welcome-title">
      <span class="access-icon"><app-icon name="lock" /></span>
      <span class="eyebrow">BIENVENIDO A WORKSOFT</span>
      <h2 id="welcome-title">Tu espacio de trabajo</h2>
      <p>Ingresa con tu cuenta para acceder a las empresas de las que formas parte.</p>
      @if (auth.error()) {
        <div class="alert error" role="alert">{{ auth.error() }}</div>
      }
      <button class="button primary full" (click)="auth.login()">
        {{ auth.error() ? 'Volver a intentar' : 'Iniciar sesión' }}<app-icon name="arrow" />
      </button>
      <p class="access-note">
        ¿Aún no tienes acceso? Solicítalo al administrador de tu organización.
      </p>
      <div class="localization">
        <span class="colombia-flag" aria-hidden="true"></span> Español (Colombia) <span>·</span> COP
      </div>
    </section>
  </main>`,
})
export class Welcome {
  readonly auth = inject(AuthService);
}
