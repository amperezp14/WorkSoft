import { Injectable, signal } from '@angular/core';
@Injectable({ providedIn: 'root' })
export class Notifications {
  readonly message = signal('');
  success(message: string): void {
    this.message.set(message);
  }
  clear(): void {
    this.message.set('');
  }
}
