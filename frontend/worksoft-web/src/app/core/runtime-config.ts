import { Injectable } from '@angular/core';
export interface RuntimeConfig {
  apiBaseUrl: string;
  keycloak: { url: string; realm: string; clientId: string };
}
@Injectable({ providedIn: 'root' })
export class RuntimeConfigService {
  private value?: RuntimeConfig;
  async load(): Promise<RuntimeConfig> {
    const response = await fetch('/config.json', { cache: 'no-store' });
    if (!response.ok) throw new Error('No se pudo cargar la configuración.');
    const config = (await response.json()) as RuntimeConfig;
    if (
      !config.apiBaseUrl ||
      !config.keycloak?.url ||
      !config.keycloak.realm ||
      !config.keycloak.clientId
    ) {
      throw new Error('La configuración está incompleta.');
    }
    const api = new URL(config.apiBaseUrl, window.location.origin);
    const identity = new URL(config.keycloak.url);
    if (
      !['http:', 'https:'].includes(api.protocol) ||
      !['http:', 'https:'].includes(identity.protocol) ||
      api.search ||
      api.hash ||
      api.username ||
      api.password ||
      identity.username ||
      identity.password
    ) {
      throw new Error('La configuración no es válida.');
    }
    config.apiBaseUrl = config.apiBaseUrl.replace(/\/$/, '');
    this.value = config;
    return config;
  }
  get config(): RuntimeConfig {
    if (!this.value) throw new Error('Configuración no disponible.');
    return this.value;
  }
}
