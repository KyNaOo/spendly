import { render, screen } from '@testing-library/angular';
import { FakeAuthService } from '../testing/fake-auth.service';
import { App } from './app';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';

describe('App', () => {
  it('displays the home page inside the Material layout', async () => {
    await render(App, {
      routes,
      providers: [{ provide: AuthService, useValue: new FakeAuthService() }],
    });

    expect(screen.getByRole('link', { name: 'Spendly' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Navigation principale' })).toBeInTheDocument();
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Bienvenue sur Spendly' }),
    ).toBeInTheDocument();
  });
});
