import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from './api-base-url';

describe('API_BASE_URL', () => {
  it('defaults to the relative /api path (proxied to :8080 in development)', () => {
    expect(TestBed.inject(API_BASE_URL)).toBe('/api');
  });
});
