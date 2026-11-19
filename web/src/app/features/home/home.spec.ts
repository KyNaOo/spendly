import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { FakeAuthService } from '../../../testing/fake-auth.service';
import { AuthService } from '../../core/auth/auth.service';
import { Home } from './home';

describe('Home', () => {
  it('offers the Keycloak login to anonymous visitors', async () => {
    const auth = new FakeAuthService();
    const user = userEvent.setup();
    await render(Home, { providers: [{ provide: AuthService, useValue: auth }] });

    await user.click(screen.getByRole('button', { name: 'Se connecter avec Keycloak' }));

    expect(auth.loginCalls).toEqual(['/']);
  });

  it('links an employee to their expense reports', async () => {
    await render(Home, {
      providers: [{ provide: AuthService, useValue: new FakeAuthService().signIn(['EMPLOYEE']) }],
    });

    expect(screen.getByRole('link', { name: 'Mes notes de frais' })).toHaveAttribute(
      'href',
      '/reports',
    );
  });
});
