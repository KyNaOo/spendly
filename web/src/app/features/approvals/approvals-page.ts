import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHeader } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-approvals-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageHeader],
  template: `<app-page-header
    title="Validations"
    subtitle="Notes de frais de votre équipe en attente de validation."
  />`,
})
export class ApprovalsPage {}
