import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
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
        @if (!auth.isAuthenticated()) {
          <button mat-flat-button type="button" (click)="auth.login('/')">
            Se connecter avec Keycloak
          </button>
        } @else if (auth.hasAnyRole(['EMPLOYEE'])) {
          <a mat-flat-button routerLink="/reports">Mes notes de frais</a>
        }
      </mat-card-actions>
    </mat-card>
  `,
})
export class Home {
  protected readonly auth = inject(AuthService);
}
