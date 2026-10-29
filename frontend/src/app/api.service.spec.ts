import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Observable } from 'rxjs';
import { ApiService, apiErrorMessage } from './api.service';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService, authInterceptor } from './auth.service';
import { Page, ProductResponse } from './api.models';

describe('API contract and authentication', () => {
  let api: ApiService;
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    api = TestBed.inject(ApiService);
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    auth.logout();
    http.verify();
    vi.useRealTimers();
  });

  function login(expiresIn = 3600): void {
    auth.login('user@example.com', 'Password123!').subscribe();
    const request = http.expectOne('/api/auth/login');
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(request.request.body).toEqual({ email: 'user@example.com', password: 'Password123!' });
    request.flush({
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresIn,
      user: {
        id: 2,
        email: 'user@example.com',
        fullName: 'Ana Pérez',
        role: 'CUSTOMER',
        active: true,
      },
    });
  }

  it('sends product search and pagination to the server and unwraps no metadata', () => {
    const page: Page<ProductResponse> = {
      content: [],
      totalElements: 25,
      totalPages: 3,
      number: 1,
      size: 10,
      first: false,
      last: false,
      empty: true,
      numberOfElements: 0,
    };
    api
      .products({
        page: 1,
        size: 10,
        q: 'camiseta',
        category: 'Ropa',
        active: false,
        sort: 'name,asc',
      })
      .subscribe((result) => expect(result).toEqual(page));
    const request = http.expectOne((request) => request.url === '/api/products');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('q')).toBe('camiseta');
    expect(request.request.params.get('category')).toBe('Ropa');
    expect(request.request.params.get('active')).toBe('false');
    expect(request.request.params.get('sort')).toBe('name,asc');
    request.flush(page);
  });

  it('creates a product with initialStock but never sends stock in product updates', () => {
    const data = {
      name: 'Camiseta',
      sku: 'SKU-001',
      description: 'Talla M',
      category: 'Ropa',
      price: 49900,
      active: true,
    };
    api.createProduct({ ...data, initialStock: 25 }).subscribe();
    const create = http.expectOne('/api/products');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual({ ...data, initialStock: 25 });
    create.flush({ id: 1, ...data, stock: 25 });

    api.updateProduct(1, data).subscribe();
    const update = http.expectOne('/api/products/1');
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(data);
    expect(update.request.body.stock).toBeUndefined();
    expect(update.request.body.initialStock).toBeUndefined();
    update.flush({ id: 1, ...data, stock: 25 });
  });

  it('handles a product deactivation with a bodyless 204 response', () => {
    api.deactivateProduct(1).subscribe((response) => expect(response).toBeNull());
    const request = http.expectOne('/api/products/1');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('sends inventory adjustments with delta and reason', () => {
    api.adjustInventory(1, -3, 'Corrección de conteo').subscribe();
    const request = http.expectOne('/api/inventory/1/adjustments');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ delta: -3, reason: 'Corrección de conteo' });
    request.flush({ productId: 1, quantity: 22 });
  });

  it('sends only product IDs and quantities for normal orders', () => {
    api
      .createOrder([
        { productId: 1, quantity: 2 },
        { productId: 3, quantity: 1 },
      ])
      .subscribe();
    const request = http.expectOne('/api/orders');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      items: [
        { productId: 1, quantity: 2 },
        { productId: 3, quantity: 1 },
      ],
    });
    request.flush({ id: 1, discountPercent: 15, total: 89820 });
  });

  it('delegates random selection to the dedicated backend endpoint', () => {
    api.createRandomOrder(2).subscribe();
    const request = http.expectOne('/api/orders/random');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ productCount: 2 });
    request.flush({ id: 1, randomOrder: true, discountPercent: 55 });
  });

  it('cancels a numeric order ID using POST', () => {
    api.cancelOrder(7).subscribe();
    const request = http.expectOne('/api/orders/7/cancel');
    expect(request.request.method).toBe('POST');
    request.flush({ id: 7, status: 'CANCELLED' });
  });

  it('updates users without altering a password when the draft is empty', () => {
    api
      .updateUser(2, { fullName: 'Ana Pérez', role: 'CUSTOMER', active: true, password: null })
      .subscribe();
    const request = http.expectOne('/api/users/2');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({
      fullName: 'Ana Pérez',
      role: 'CUSTOMER',
      active: true,
      password: null,
    });
    request.flush({ id: 2 });
  });

  it('does not request a role during public registration', () => {
    auth
      .register({ email: 'ana@example.com', fullName: 'Ana Pérez', password: 'Password123!' })
      .subscribe();
    const request = http.expectOne('/api/auth/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'ana@example.com',
      fullName: 'Ana Pérez',
      password: 'Password123!',
    });
    request.flush({ id: 2, role: 'CUSTOMER' });
    expect(auth.user()).toBeNull();
  });

  it('adds bearer auth to the API but not unrelated destinations', () => {
    login();
    api.orders({ page: 0, size: 20 }).subscribe();
    const orders = http.expectOne((request) => request.url === '/api/orders');
    expect(orders.request.headers.get('Authorization')).toBe('Bearer test-token');
    orders.flush({ content: [] });
    TestBed.inject(HttpClient).get('https://unrelated.invalid/data').subscribe();
    const unrelated = http.expectOne('https://unrelated.invalid/data');
    expect(unrelated.request.headers.has('Authorization')).toBe(false);
    unrelated.flush({});
  });

  it('expires the session on authenticated 401 and propagates the error', () => {
    login();
    let status: number | undefined;
    api
      .orders({ page: 0, size: 20 })
      .subscribe({ error: (error: HttpErrorResponse) => (status = error.status) });
    http
      .expectOne((request) => request.url === '/api/orders')
      .flush({ message: 'Token inválido' }, { status: 401, statusText: 'Unauthorized' });
    expect(status).toBe(401);
    expect(auth.user()).toBeNull();
    expect(auth.notice()).toContain('sesión ha vencido');
  });

  it('keeps the session on 403 and propagates the API message', () => {
    login();
    let message = '';
    api
      .users({ page: 0, size: 20 })
      .subscribe({ error: (error: unknown) => (message = apiErrorMessage(error)) });
    http
      .expectOne((request) => request.url === '/api/users')
      .flush(
        { message: 'Acceso de administrador requerido' },
        { status: 403, statusText: 'Forbidden' },
      );
    expect(auth.user()).not.toBeNull();
    expect(message).toBe('Acceso de administrador requerido');
  });

  it('expires the token at expiresIn and clears it on logout', () => {
    vi.useFakeTimers();
    login(1);
    expect(auth.accessToken()).toBe('test-token');
    vi.advanceTimersByTime(1000);
    expect(auth.accessToken()).toBeNull();
    expect(auth.user()).toBeNull();
  });

  it.each([
    ['inventory', '/api/inventory'],
    ['orders', '/api/orders'],
    ['users', '/api/users'],
    ['audit', '/api/audit'],
  ] as const)('paginates %s on the server', (method, url) => {
    const response: Observable<unknown> = api[method]({ page: 2, size: 50, sort: 'id,desc' });
    response.subscribe();
    const request = http.expectOne((request) => request.url === url);
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('50');
    expect(request.request.params.get('sort')).toBe('id,desc');
    request.flush({ content: [] });
  });

  it('uses the three report endpoints without adding unsupported sort to active products', () => {
    api.activeProducts({ page: 1, size: 20 }).subscribe();
    const active = http.expectOne((request) => request.url === '/api/reports/products/active');
    expect(active.request.params.get('page')).toBe('1');
    expect(active.request.params.has('sort')).toBe(false);
    active.flush({ content: [] });
    api.topSelling().subscribe();
    http.expectOne('/api/reports/products/top-selling').flush([]);
    api.frequentCustomers().subscribe();
    http.expectOne('/api/reports/customers/frequent').flush([]);
  });

  it('surfaces network and structured conflict errors', () => {
    expect(apiErrorMessage(new HttpErrorResponse({ status: 0 }))).toContain('conectar con la API');
    expect(
      apiErrorMessage(new HttpErrorResponse({ status: 409, error: { message: 'SKU duplicado' } })),
    ).toBe('SKU duplicado');
  });
});
