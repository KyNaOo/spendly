import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { FakeAuthService } from '../../../testing/fake-auth.service';
import { authGuard, roleGuard } from './auth.guards';
import { AuthService } from './auth.service';

@Component({ template: 'page' })
class DummyPage {}

async function setup(auth: FakeAuthService) {
  TestBed.configureTestingModule({
    providers: [
      { provide: AuthService, useValue: auth },
      provideRouter([
        { path: '', component: DummyPage },
        { path: 'forbidden', component: DummyPage },
        { path: 'private', canActivate: [authGuard], component: DummyPage },
        { path: 'approvals', canActivate: [roleGuard(['MANAGER'])], component: DummyPage },
      ]),
    ],
  });
  const harness = await RouterTestingHarness.create('/');
  return { harness, router: TestBed.inject(Router) };
}

describe('authGuard', () => {
  it('lets an authenticated user through', async () => {
    const { harness, router } = await setup(new FakeAuthService().signIn(['EMPLOYEE']));

    await harness.navigateByUrl('/private');

    expect(router.url).toBe('/private');
  });

  it('starts the login with the requested URL and cancels the navigation', async () => {
    const auth = new FakeAuthService();
    const { harness, router } = await setup(auth);

    await harness.navigateByUrl('/private?tab=1');

    expect(router.url).toBe('/');
    expect(auth.loginCalls).toEqual(['/private?tab=1']);
  });
});

describe('roleGuard', () => {
  it('lets a user holding the role through', async () => {
    const { harness, router } = await setup(new FakeAuthService().signIn(['EMPLOYEE', 'MANAGER']));

    await harness.navigateByUrl('/approvals');

    expect(router.url).toBe('/approvals');
  });

  it('redirects a user without the role to the 403 page', async () => {
    const { harness, router } = await setup(new FakeAuthService().signIn(['EMPLOYEE']));

    await harness.navigateByUrl('/approvals');

    expect(router.url).toBe('/forbidden');
  });

  it('asks an anonymous user to log in first', async () => {
    const auth = new FakeAuthService();
    const { harness, router } = await setup(auth);

    await harness.navigateByUrl('/approvals');

    expect(router.url).toBe('/');
    expect(auth.loginCalls).toEqual(['/approvals']);
  });
});
