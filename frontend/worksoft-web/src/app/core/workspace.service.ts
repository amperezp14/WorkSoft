import { Injectable, inject, signal } from '@angular/core';
import { RuntimeConfigService } from './runtime-config';
/** Contexto local de trabajo. No representa una identidad autenticada. */
@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly config = inject(RuntimeConfigService);
  readonly ready = signal(false);
  readonly error = signal('');
  readonly username = signal('Espacio local');
  readonly initials = signal('WS');
  readonly canProvision = signal(true);
  async initialize(): Promise<void> {
    try {
      await this.config.load();
      this.ready.set(true);
    } catch {
      this.error.set('No pudimos cargar la configuración. Vuelve a cargar la aplicación.');
    }
  }
  reload(): void {
    window.location.reload();
  }
}
