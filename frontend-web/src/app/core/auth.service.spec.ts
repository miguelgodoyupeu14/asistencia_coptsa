import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigateByUrl: vi.fn() } }
      ]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('keeps the authenticated session after login', () => {
    const payload = btoa(JSON.stringify({preferred_username:'admin',roles:['ADMIN'],exp:Math.floor(Date.now()/1000)+900}));
    const token = `header.${payload}.signature`;

    service.login('admin','admin').subscribe();
    http.expectOne('/api/v1/auth/login').flush({accessToken:token,tokenType:'Bearer',expiresAt:new Date(Date.now()+900000).toISOString()});

    expect(service.authenticated()).toBe(true);
    expect(service.username()).toBe('admin');
    expect(service.roles()).toEqual(['ADMIN']);
    expect(sessionStorage.getItem('asistencia.accessToken')).toBe(token);
  });
});
