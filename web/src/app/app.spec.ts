import { render, screen } from '@testing-library/angular';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  it('displays the home page inside the Material layout', async () => {
    await render(App, { routes });

    expect(screen.getByRole('link', { name: 'Spendly' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Navigation principale' })).toBeInTheDocument();
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Bienvenue sur Spendly' }),
    ).toBeInTheDocument();
  });
});
