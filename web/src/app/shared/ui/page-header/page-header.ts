import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Page title with an optional subtitle, reused by every feature page. */
@Component({
  selector: 'app-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-header">
      <h1 class="page-header__title">{{ title() }}</h1>
      @if (subtitle(); as subtitle) {
        <p class="page-header__subtitle">{{ subtitle }}</p>
      }
    </header>
  `,
  styles: `
    .page-header {
      margin-bottom: 24px;
    }
    .page-header__title {
      font: var(--mat-sys-headline-medium);
      margin: 0;
    }
    .page-header__subtitle {
      font: var(--mat-sys-body-large);
      color: var(--mat-sys-on-surface-variant);
      margin: 8px 0 0;
    }
  `,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
}
