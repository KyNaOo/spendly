import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHeader } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-admin-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageHeader],
  template: `<app-page-header
    title="Administration"
    subtitle="Utilisateurs, catégories et plafonds de dépenses."
  />`,
})
export class AdminPage {}
