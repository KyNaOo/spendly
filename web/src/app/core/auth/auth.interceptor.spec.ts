import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FakeAuthService } from '../../../testing/fake-auth.service';
import { API_BASE_URL } from '../api/api-base-url';
import { authInterceptor, isApiUrl } from './auth.interceptor';
import { AuthService } from './auth.service';

function setup(auth: FakeAuthService) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
      { provide: AuthService, useValue: auth },
      { provide: API_BASE_URL, useValue: '/api' },
    ],
  });
  return { http: TestBed.inject(HttpClient), httpMock: TestBed.inject(HttpTestingController) };
}

describe('authInterceptor', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('adds the Bearer token to API requests', () => {
    const { http, httpMock } = setup(new FakeAuthService().signIn(['EMPLOYEE'], 'Alice', 'jwt'));

    http.get('/api/me').subscribe();

    const req = httpMock.expectOne('/api/me');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt');
    req.flush({});
  });

  it.each([
    'http://localhost:8180/realms/spendly/protocol/openid-connect/token',
    'https://cdn.example.com/api/file.js',
    '/apix/me',
    '/assets/i18n/fr.json',
  ])('never sends the token outside the API: %s', (url) => {
    const { http, httpMock } = setup(new FakeAuthService().signIn(['EMPLOYEE'], 'Alice', 'jwt'));

    http.get(url).subscribe();

    const req = httpMock.expectOne(url);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('leaves API requests untouched while logged out', () => {
    const { http, httpMock } = setup(new FakeAuthService());

    http.get('/api/me').subscribe();

    const req = httpMock.expectOne('/api/me');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});

describe('isApiUrl', () => {
  it.each([
    ['/api', '/api', true],
    ['/api/me', '/api', true],
    ['/api?page=1', '/api', true],
    ['/api/me', '/api/', true],
    ['https://spendly.example/api/me', 'https://spendly.example/api', true],
    ['/apix', '/api', false],
    ['https://evil.example/api/me', '/api', false],
  ])('isApiUrl(%s, %s) = %s', (url, base, expected) => {
    expect(isApiUrl(url, base)).toBe(expected);
  });
});
