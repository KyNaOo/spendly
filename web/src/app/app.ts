import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Layout } from './core/layout/layout';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Layout],
  template: '<app-layout />',
})
export class App {}
