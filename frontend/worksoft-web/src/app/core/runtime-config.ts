import { Injectable } from '@angular/core';
export interface RuntimeConfig {
  mode: 'demo' | 'api';
  apiBaseUrl: string;
}
@Injectable({ providedIn: 'root' })
export class RuntimeConfigService {
  private value?: RuntimeConfig;
  async load(): Promise<RuntimeConfig> {
    const response = await fetch('/config.json', { cache: 'no-store' });
    if (!response.ok) throw new Error('Configuración no disponible.');
    const config = (await response.json()) as RuntimeConfig;
    if (!['demo', 'api'].includes(config.mode) || !config.apiBaseUrl)
      throw new Error('Configuración inválida.');
    const url = new URL(config.apiBaseUrl, location.origin);
    if (
      !['http:', 'https:'].includes(url.protocol) ||
      url.username ||
      url.password ||
      url.search ||
      url.hash
    )
      throw new Error('URL inválida.');
    config.apiBaseUrl = config.apiBaseUrl.replace(/\/$/, '');
    this.value = config;
    return config;
  }
  get config(): RuntimeConfig {
    if (!this.value) throw new Error('Configuración no disponible.');
    return this.value;
  }
}
