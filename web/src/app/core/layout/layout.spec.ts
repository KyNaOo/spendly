import { render, screen, waitFor } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { FakeAuthService } from '../../../testing/fake-auth.service';
import { routes } from '../../app.routes';
import { AuthService } from '../auth/auth.service';
import { Role } from '../auth/role';
import { Layout } from './layout';

async function renderAs(auth: FakeAuthService) {
  return render(Layout, { routes, providers: [{ provide: AuthService, useValue: auth }] });
}

const menuLinks = () => screen.getAllByRole('link').map((link) => link.textContent?.trim());

describe('Layout', () => {
  it('shows only the home entry and a login button to anonymous visitors', async () => {
    const auth = new FakeAuthService();
    const user = userEvent.setup();
    await renderAs(auth);

    expect(screen.getByRole('link', { name: 'Spendly' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Accueil' })).toHaveAttribute('href', '/');
    expect(screen.queryByRole('link', { name: 'Mes notes de frais' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Se connecter' }));

    expect(auth.loginCalls).toHaveLength(1);
  });

  it.each<[string, Role[], string[]]>([
    ['EMPLOYEE', ['EMPLOYEE'], ['Accueil', 'Mes notes de frais']],
    ['MANAGER', ['EMPLOYEE', 'MANAGER'], ['Accueil', 'Mes notes de frais', 'Validations']],
    ['ACCOUNTANT', ['EMPLOYEE', 'ACCOUNTANT'], ['Accueil', 'Mes notes de frais', 'Comptabilité']],
    ['ADMIN', ['ADMIN'], ['Accueil', 'Administration']],
  ])('adapts the menu to the %s role', async (_label, roles, expected) => {
    await renderAs(new FakeAuthService().signIn(roles));

    const nav = screen.getByRole('navigation', { name: 'Navigation principale' });
    const entries = [...nav.querySelectorAll('a')].map((a) => a.textContent?.trim());
    expect(entries).toEqual(expected.map((label) => expect.stringContaining(label)));
  });

  it('shows the user name and logs out', async () => {
    const auth = new FakeAuthService().signIn(['EMPLOYEE'], 'Alice Martin');
    const user = userEvent.setup();
    await renderAs(auth);

    expect(screen.getByText('Alice Martin')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Se déconnecter' }));

    expect(auth.logoutCalls).toBe(1);
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Se déconnecter' })).not.toBeInTheDocument(),
    );
    expect(screen.getByRole('button', { name: 'Se connecter' })).toBeInTheDocument();
    expect(menuLinks()).not.toContain('Mes notes de frais');
  });

  it.each([
    ['/reports', ['EMPLOYEE'], 'Mes notes de frais'],
    ['/approvals', ['MANAGER'], 'Validations'],
    ['/accounting', ['ACCOUNTANT'], 'Comptabilité'],
    ['/admin', ['ADMIN'], 'Administration'],
  ] satisfies [string, Role[], string][])(
    'lazy-loads %s for an allowed role',
    async (url, roles, heading) => {
      const { navigate } = await renderAs(new FakeAuthService().signIn(roles));

      await navigate(url);

      expect(await screen.findByRole('heading', { level: 1, name: heading })).toBeInTheDocument();
    },
  );

  it.each(['/approvals', '/accounting', '/admin'])(
    'shows the 403 page when an EMPLOYEE navigates to %s',
    async (url) => {
      const { navigate } = await renderAs(new FakeAuthService().signIn(['EMPLOYEE']));

      await navigate(url);

      expect(
        await screen.findByRole('heading', { level: 1, name: 'Accès refusé' }),
      ).toBeInTheDocument();
    },
  );

  it('asks an anonymous visitor to log in before opening a feature', async () => {
    const auth = new FakeAuthService();
    const { navigate } = await renderAs(auth);

    await navigate('/reports');

    expect(auth.loginCalls).toEqual(['/reports']);
    expect(screen.queryByRole('heading', { level: 1 })).not.toBeInTheDocument();
  });

  it('navigates when a menu entry is clicked', async () => {
    const user = userEvent.setup();
    await renderAs(new FakeAuthService().signIn(['EMPLOYEE', 'MANAGER']));

    await user.click(screen.getByRole('link', { name: 'Validations' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Validations' }),
    ).toBeInTheDocument();
  });

  it('redirects unknown URLs to the home page', async () => {
    const { navigate } = await renderAs(new FakeAuthService());

    await navigate('/does-not-exist');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Bienvenue sur Spendly' }),
    ).toBeInTheDocument();
  });
});
