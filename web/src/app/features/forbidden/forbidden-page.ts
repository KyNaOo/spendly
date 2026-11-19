import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { PageHeader } from '../../shared/ui/page-header/page-header';

/** Target of `roleGuard` when the user lacks the required role (HTTP 403 equivalent). */
@Component({
  selector: 'app-forbidden-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PageHeader, MatButtonModule, RouterLink],
  template: `
    <app-page-header
      title="Accès refusé"
      subtitle="Votre profil ne vous donne pas accès à cette page."
    />
    <a mat-flat-button routerLink="/">Retour à l'accueil</a>
  `,
})
export class ForbiddenPage {}
