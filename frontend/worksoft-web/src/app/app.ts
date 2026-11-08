import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { Notifications } from './core/notifications';
import { Icon } from './shared/icon';
import { Welcome } from './welcome';
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon, Welcome],
  templateUrl: './app.html',
})
export class App {
  readonly auth = inject(AuthService);
  readonly notices = inject(Notifications);
}
