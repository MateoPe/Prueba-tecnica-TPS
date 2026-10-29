import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { computed, inject, Injectable, InjectionToken, signal } from '@angular/core';
import { catchError, tap, throwError } from 'rxjs';
import { LoginResponse, RegisterRequest, UserResponse } from './api.models';

export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', { factory: () => '/api' });

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  private readonly session = signal<LoginResponse | null>(null);
  private expiresAt = 0;
  private expiryTimer?: ReturnType<typeof setTimeout>;

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly notice = signal('');

  login(email: string, password: string) {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, { email, password }).pipe(
      tap((response) => {
        this.logout();
        this.expiresAt = Date.now() + response.expiresIn * 1000;
        this.session.set(response);
        this.expiryTimer = setTimeout(() => this.expire(), response.expiresIn * 1000);
      }),
    );
  }

  register(request: RegisterRequest) {
    return this.http.post<UserResponse>(`${this.baseUrl}/auth/register`, request);
  }

  accessToken(): string | null {
    if (this.session() && Date.now() >= this.expiresAt) this.expire();
    return this.session()?.accessToken ?? null;
  }

  updateUser(user: UserResponse): void {
    this.session.update((session) => (session ? { ...session, user } : null));
  }

  logout(): void {
    clearTimeout(this.expiryTimer);
    this.session.set(null);
    this.expiresAt = 0;
    this.notice.set('');
  }

  expire(): void {
    this.logout();
    this.notice.set('Tu sesión ha vencido. Inicia sesión nuevamente.');
  }
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const baseUrl = inject(API_BASE_URL);
  if (!request.url.startsWith(`${baseUrl}/`)) return next(request);

  const auth = inject(AuthService);
  const isPublicAuth =
    request.url === `${baseUrl}/auth/login` || request.url === `${baseUrl}/auth/register`;
  const token = isPublicAuth ? null : auth.accessToken();
  const authenticatedRequest = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      // An old request must not invalidate a newer session.
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        token &&
        auth.accessToken() === token
      ) {
        auth.expire();
      }
      return throwError(() => error);
    }),
  );
};
