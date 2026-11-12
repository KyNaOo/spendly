import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHeader } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-accounting-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageHeader],
  template: `<app-page-header
    title="Comptabilité"
    subtitle="Remboursements et exports comptables mensuels."
  />`,
})
export class AccountingPage {}
