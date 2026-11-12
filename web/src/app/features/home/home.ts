import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { PageHeader } from '../../shared/ui/page-header/page-header';

@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageHeader, MatCardModule, MatButtonModule, RouterLink],
  template: `
    <app-page-header
      title="Bienvenue sur Spendly"
      subtitle="Gestion des notes de frais avec circuit de validation."
    />
    <mat-card appearance="outlined">
      <mat-card-content>
        Déclarez vos dépenses, faites-les valider par votre manager et suivez leur remboursement.
      </mat-card-content>
      <mat-card-actions>
        <a mat-flat-button routerLink="/reports">Mes notes de frais</a>
      </mat-card-actions>
    </mat-card>
  `,
})
export class Home {}
