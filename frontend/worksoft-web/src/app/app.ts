import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { WorkspaceService } from './core/workspace.service';
import { Notifications } from './core/notifications';
import { Icon } from './shared/icon';
import { CompanyContext } from './features/workspace/company-context';
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  templateUrl: './app.html',
})
export class App {
  readonly company = inject(CompanyContext).company;
  readonly workspace = inject(WorkspaceService);
  readonly notices = inject(Notifications);
}
