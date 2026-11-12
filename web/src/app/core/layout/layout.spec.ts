import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { routes } from '../../app.routes';
import { Layout } from './layout';

describe('Layout', () => {
  it('renders the toolbar and every navigation entry', async () => {
    await render(Layout, { routes });

    expect(screen.getByRole('link', { name: 'Spendly' })).toHaveAttribute('href', '/');
    const expected: [string, string][] = [
      ['Accueil', '/'],
      ['Mes notes de frais', '/reports'],
      ['Validations', '/approvals'],
      ['Comptabilité', '/accounting'],
      ['Administration', '/admin'],
    ];
    for (const [name, href] of expected) {
      expect(screen.getByRole('link', { name })).toHaveAttribute('href', href);
    }
  });

  it.each([
    ['/reports', 'Mes notes de frais'],
    ['/approvals', 'Validations'],
    ['/accounting', 'Comptabilité'],
    ['/admin', 'Administration'],
  ])('lazy-loads the feature page for %s', async (url, heading) => {
    const { navigate } = await render(Layout, { routes });

    await navigate(url);

    expect(await screen.findByRole('heading', { level: 1, name: heading })).toBeInTheDocument();
  });

  it('navigates when a menu entry is clicked', async () => {
    const user = userEvent.setup();
    await render(Layout, { routes });

    await user.click(screen.getByRole('link', { name: 'Validations' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Validations' }),
    ).toBeInTheDocument();
  });

  it('redirects unknown URLs to the home page', async () => {
    const { navigate } = await render(Layout, { routes });

    await navigate('/does-not-exist');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Bienvenue sur Spendly' }),
    ).toBeInTheDocument();
  });
});
