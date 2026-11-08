import { computed, inject, Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { RuntimeConfigService } from './runtime-config';
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly config = inject(RuntimeConfigService);
  private client?: Keycloak;
  readonly authenticated = signal(false);
  readonly error = signal('');
  readonly username = signal('');
  readonly canProvision = signal(false);
  readonly initials = computed(() => (this.username().slice(0, 2) || 'WS').toUpperCase());
  async initialize(): Promise<void> {
    try {
      const config = await this.config.load();
      this.client = new Keycloak(config.keycloak);
      this.client.onAuthSuccess = () => this.sync();
      this.client.onAuthRefreshSuccess = () => this.sync();
      this.client.onAuthLogout = () => this.sync();
      this.client.onAuthRefreshError = () => {
        this.client?.clearToken();
        this.sync();
      };
      await this.client.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false });
      this.sync();
    } catch {
      this.error.set(
        'No pudimos conectar con el servicio de acceso. Comprueba la configuración y vuelve a intentarlo.',
      );
    }
  }
  private sync(): void {
    this.authenticated.set(!!this.client?.authenticated);
    this.username.set(this.client?.tokenParsed?.['preferred_username'] ?? 'Usuario');
    this.canProvision.set(
      !!this.client?.authenticated && !!this.client.hasRealmRole('company:provision'),
    );
  }
  async login(): Promise<void> {
    if (!this.client || this.error()) {
      window.location.reload();
      return;
    }
    try {
      await this.client.login({ redirectUri: window.location.origin + '/empresas' });
    } catch {
      this.error.set('No se pudo iniciar sesión. Vuelve a intentarlo.');
    }
  }
  async logout(): Promise<void> {
    try {
      await this.client?.logout({ redirectUri: window.location.origin + '/inicio' });
    } catch {
      this.error.set('No se pudo cerrar la sesión. Vuelve a intentarlo.');
    }
  }
  async accessToken(): Promise<string> {
    if (!this.client?.authenticated) throw new Error('SESSION_EXPIRED');
    try {
      await this.client.updateToken(30);
      this.sync();
      if (!this.client.token) throw new Error('SESSION_EXPIRED');
      return this.client.token;
    } catch {
      this.client.clearToken();
      this.sync();
      throw new Error('SESSION_EXPIRED');
    }
  }
}
