import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHeader } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-reports-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageHeader],
  template: `<app-page-header
    title="Mes notes de frais"
    subtitle="Création, saisie des lignes et soumission de vos notes de frais."
  />`,
})
export class ReportsPage {}
