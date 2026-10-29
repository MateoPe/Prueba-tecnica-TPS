import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { App } from './app';
import { AuthService, authInterceptor } from './auth.service';
import { OrderResponse, Page, ProductResponse, Role } from './api.models';

const product: ProductResponse = {
  id: 1,
  sku: 'SKU-001',
  name: 'Camiseta TPS',
  description: 'Talla M',
  category: 'Ropa',
  price: 49900,
  active: true,
  stock: 25,
  createdAt: '2026-10-29T12:00:00Z',
  updatedAt: '2026-10-29T12:00:00Z',
};
const order: OrderResponse = {
  id: 7,
  customerId: 2,
  customerEmail: 'ana@example.com',
  items: [
    {
      productId: 1,
      productName: 'Camiseta TPS',
      productSku: 'SKU-001',
      quantity: 2,
      unitPrice: 49900,
      lineTotal: 99800,
    },
  ],
  subtotal: 99800,
  discountPercent: 15,
  total: 84830,
  randomOrder: false,
  status: 'COMPLETED',
  createdAt: '2026-10-29T12:00:00Z',
};

function page<T>(content: T[], totalElements = content.length, number = 0, size = 20): Page<T> {
  const totalPages = Math.ceil(totalElements / size);
  return {
    content,
    totalElements,
    totalPages,
    number,
    size,
    numberOfElements: content.length,
    first: number === 0,
    last: number + 1 >= totalPages,
    empty: !content.length,
  };
}

describe('API-backed application', () => {
  let fixture: ComponentFixture<App>;
  let app: App;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(App);
    app = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    TestBed.inject(AuthService).logout();
    http.verify();
  });

  function flushDashboard(role: Role): void {
    http.expectOne((request) => request.url === '/api/orders').flush(page([order]));
    if (role === 'ADMIN') {
      http
        .expectOne((request) => request.url === '/api/reports/products/active')
        .flush(page([product], 1, 0, 1));
      http
        .expectOne('/api/reports/products/top-selling')
        .flush([{ productId: 1, name: product.name, sku: product.sku, unitsSold: 42 }]);
      http
        .expectOne('/api/reports/customers/frequent')
        .flush([
          { customerId: 2, fullName: 'Ana Pérez', email: 'ana@example.com', completedOrders: 7 },
        ]);
    }
  }

  async function signIn(role: Role = 'ADMIN'): Promise<void> {
    app.loginEmail = 'ana@example.com';
    app.loginPassword = 'Password123!';
    const pending = app.submitAuth();
    http.expectOne('/api/auth/login').flush({
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: { id: 2, email: 'ana@example.com', fullName: 'Ana Pérez', role, active: true },
    });
    await Promise.resolve();
    flushDashboard(role);
    await pending;
    fixture.detectChanges();
  }

  it('starts on login, without fabricated store data or requests', () => {
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Bienvenido');
    expect(app.productPage()).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Modo demo');
    http.expectNone((request) => request.url.startsWith('/api/'));
  });

  it('shows real report counts, names and order amounts for administrators', async () => {
    await signIn();
    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('Ana Pérez');
    expect(text).toContain('42 uds.');
    expect(text).toContain('Auditoría');
    expect(app.recentRevenue()).toBe(84830);
  });

  it('hides admin navigation and does not request admin endpoints for customers', async () => {
    await signIn('CUSTOMER');
    const text: string = fixture.nativeElement.textContent;
    expect(text).not.toContain('Inventario');
    expect(text).not.toContain('Auditoría');
    expect(text).not.toContain('Reportes');
    await app.setView('users');
    expect(app.activeView()).toBe('dashboard');
    expect(app.error()).toContain('permisos');
    http.expectNone((request) => request.url === '/api/users');
  });

  it('registers a customer without silently treating the response as a session', async () => {
    app.setAuthMode('register');
    app.loginEmail = 'new@example.com';
    app.loginPassword = 'Password123!';
    app.registerName = 'Nuevo Cliente';
    const pending = app.submitAuth();
    const request = http.expectOne('/api/auth/register');
    expect(request.request.body).toEqual({
      email: 'new@example.com',
      password: 'Password123!',
      fullName: 'Nuevo Cliente',
    });
    request.flush({ id: 3, role: 'CUSTOMER' });
    await pending;
    expect(app.auth.user()).toBeNull();
    expect(app.authMode()).toBe('login');
    expect(app.loginPassword).toBe('');
    expect(app.auth.notice()).toContain('Cuenta creada');
  });

  it('keeps login errors visible', async () => {
    app.loginEmail = 'ana@example.com';
    app.loginPassword = 'wrong';
    const pending = app.submitAuth();
    http
      .expectOne('/api/auth/login')
      .flush({ message: 'Credenciales inválidas' }, { status: 401, statusText: 'Unauthorized' });
    await pending;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain(
      'Credenciales inválidas',
    );
    expect(app.auth.user()).toBeNull();
  });

  it('sends product filters and page numbers to the API', async () => {
    await signIn();
    const navigation = app.setView('products');
    http.expectOne((request) => request.url === '/api/products').flush(page([product], 21));
    await navigation;
    const next = app.changePage(1);
    const request = http.expectOne((request) => request.url === '/api/products');
    expect(request.request.params.get('page')).toBe('1');
    request.flush(page([product], 21, 1));
    await next;

    app.query = 'camiseta';
    app.category = 'Ropa';
    app.productActive = 'false';
    const search = app.loadView();
    const filtered = http.expectOne((request) => request.url === '/api/products');
    expect(filtered.request.params.get('q')).toBe('camiseta');
    expect(filtered.request.params.get('category')).toBe('Ropa');
    expect(filtered.request.params.get('active')).toBe('false');
    expect(filtered.request.params.get('page')).toBe('0');
    filtered.flush(page([]));
    await search;
  });

  it('does not change stock when editing a product and respects active independently', async () => {
    await signIn();
    app.openProduct({ ...product, stock: 0 });
    const pending = app.saveProduct();
    const request = http.expectOne('/api/products/1');
    expect(request.request.body.active).toBe(true);
    expect(request.request.body.stock).toBeUndefined();
    expect(request.request.body.initialStock).toBeUndefined();
    request.flush(product);
    await Promise.resolve();
    flushDashboard('ADMIN');
    await pending;
  });

  it('retains the product form and API error after a duplicate SKU', async () => {
    await signIn();
    app.openProduct(product);
    const pending = app.saveProduct();
    http
      .expectOne('/api/products/1')
      .flush({ message: 'SKU duplicado' }, { status: 409, statusText: 'Conflict' });
    await pending;
    expect(app.modal()).toBe('product');
    expect(app.modalError()).toBe('SKU duplicado');
    expect(app.productDraft.name).toBe(product.name);
  });

  it('merges cart quantities and sends only identifiers and quantities', async () => {
    await signIn('CUSTOMER');
    const opening = app.openOrder();
    http.expectOne((request) => request.url === '/api/products').flush(page([product]));
    await opening;
    app.pickerQuantity = 1;
    app.addOrderItem();
    app.addOrderItem();
    expect(app.orderItems()).toEqual([{ productId: 1, name: product.name, quantity: 2 }]);
    const pending = app.saveOrder();
    const request = http.expectOne('/api/orders');
    expect(request.request.body).toEqual({ items: [{ productId: 1, quantity: 2 }] });
    request.flush(order);
    await Promise.resolve();
    flushDashboard('CUSTOMER');
    await pending;
    expect(app.selectedOrder()?.discountPercent).toBe(15);
    expect(app.selectedOrder()?.total).toBe(84830);
    expect(app.modal()).toBe('detail');
  });

  it('uses server random selection and the returned 55% discount', async () => {
    await signIn('CUSTOMER');
    const opening = app.openOrder();
    http.expectOne((request) => request.url === '/api/products').flush(page([]));
    await opening;
    app.randomOrder = true;
    app.randomProductCount = 2;
    const pending = app.saveOrder();
    const request = http.expectOne('/api/orders/random');
    expect(request.request.body).toEqual({ productCount: 2 });
    request.flush({ ...order, randomOrder: true, discountPercent: 55, total: 44910 });
    await Promise.resolve();
    flushDashboard('CUSTOMER');
    await pending;
    expect(app.selectedOrder()?.discountPercent).toBe(55);
  });

  it('surfaces insufficient stock without creating a local order', async () => {
    await signIn('CUSTOMER');
    app.orderItems.set([{ productId: 1, name: product.name, quantity: 2 }]);
    const pending = app.saveOrder();
    http
      .expectOne('/api/orders')
      .flush({ message: 'Stock insuficiente' }, { status: 400, statusText: 'Bad Request' });
    await pending;
    expect(app.modalError()).toBe('Stock insuficiente');
    expect(app.orderPage()?.totalElements).toBe(1);
    expect(app.selectedOrder()).toBeNull();
  });

  it('cancels an order and renders the server cancellation state', async () => {
    await signIn('CUSTOMER');
    app.confirmAction('order', order.id, 'Pedido #7');
    const pending = app.executeConfirmation();
    http.expectOne('/api/orders/7/cancel').flush({ ...order, status: 'CANCELLED' });
    await Promise.resolve();
    http
      .expectOne((request) => request.url === '/api/orders')
      .flush(page([{ ...order, status: 'CANCELLED' }]));
    await pending;
    expect(app.selectedOrder()?.status).toBe('CANCELLED');
    expect(app.recentRevenue()).toBe(0);
  });

  it('rejects invalid adjustment, quantity and random-count inputs visibly', async () => {
    await signIn();
    app.openAdjustment({
      productId: 1,
      sku: product.sku,
      productName: product.name,
      quantity: 25,
      updatedAt: product.updatedAt,
    });
    app.adjustmentDelta = 0;
    app.adjustmentReason = 'Conteo';
    await app.saveAdjustment();
    expect(app.modalError()).toContain('distinto de cero');
    app.randomOrder = true;
    app.randomProductCount = 11;
    await app.saveOrder();
    expect(app.modalError()).toContain('1 a 10');
    http.expectNone(
      (request) => request.url.includes('adjustments') || request.url.includes('random'),
    );
  });

  it('ignores a stale list response after switching views', async () => {
    await signIn();
    const products = app.setView('products');
    const productRequest = http.expectOne((request) => request.url === '/api/products');
    const orders = app.setView('orders');
    http.expectOne((request) => request.url === '/api/orders').flush(page([order]));
    await orders;
    productRequest.flush(page([product]));
    await products;
    expect(app.activeView()).toBe('orders');
    expect(app.productPage()).toBeNull();
    expect(app.loading()).toBe(false);
  });

  it('clears private data and returns to login after an authenticated 401', async () => {
    await signIn();
    const pending = app.setView('users');
    http
      .expectOne((request) => request.url === '/api/users')
      .flush({ message: 'Token vencido' }, { status: 401, statusText: 'Unauthorized' });
    await pending;
    fixture.detectChanges();
    expect(app.auth.user()).toBeNull();
    expect(app.orderPage()).toBeNull();
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Bienvenido');
    expect(fixture.nativeElement.textContent).toContain('sesión ha vencido');
  });

  it('updates inventory using the server response and refreshes the inventory page', async () => {
    await signIn();
    const inventory = {
      productId: 1,
      productName: product.name,
      sku: product.sku,
      quantity: 25,
      updatedAt: product.updatedAt,
    };
    const navigation = app.setView('inventory');
    http.expectOne((request) => request.url === '/api/inventory').flush(page([inventory]));
    await navigation;
    app.openAdjustment(inventory);
    app.adjustmentDelta = -3;
    app.adjustmentReason = ' Corrección de conteo ';
    const pending = app.saveAdjustment();
    const request = http.expectOne('/api/inventory/1/adjustments');
    expect(request.request.body).toEqual({ delta: -3, reason: 'Corrección de conteo' });
    request.flush({ ...inventory, quantity: 22 });
    await Promise.resolve();
    http
      .expectOne((request) => request.url === '/api/inventory')
      .flush(page([{ ...inventory, quantity: 22 }]));
    await pending;
    expect(app.inventoryPage()?.content[0].quantity).toBe(22);
    expect(app.modal()).toBeNull();
  });

  it('preserves the history semantics of deactivation and handles 204 without a body', async () => {
    await signIn();
    const navigation = app.setView('products');
    http.expectOne((request) => request.url === '/api/products').flush(page([product]));
    await navigation;
    app.confirmAction('product', product.id, product.name);
    const pending = app.executeConfirmation();
    const request = http.expectOne('/api/products/1');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await Promise.resolve();
    http.expectOne((request) => request.url === '/api/products').flush(page([]));
    await pending;
    expect(app.modal()).toBeNull();
    expect(app.toast()).toContain('historial e inventario se conservan');
  });

  it('updates user roles and removes administrative navigation after changing its own role', async () => {
    await signIn();
    const user = {
      id: 2,
      email: 'ana@example.com',
      fullName: 'Ana Pérez',
      role: 'ADMIN' as const,
      active: true,
      createdAt: product.createdAt,
      updatedAt: product.updatedAt,
    };
    const navigation = app.setView('users');
    http.expectOne((request) => request.url === '/api/users').flush(page([user]));
    await navigation;
    app.openUser(user);
    app.userDraft.role = 'CUSTOMER';
    const pending = app.saveUser();
    const request = http.expectOne('/api/users/2');
    expect(request.request.body.password).toBeNull();
    expect(request.request.body.role).toBe('CUSTOMER');
    request.flush({ ...user, role: 'CUSTOMER' });
    await Promise.resolve();
    flushDashboard('CUSTOMER');
    await pending;
    expect(app.auth.isAdmin()).toBe(false);
    expect(app.activeView()).toBe('dashboard');
    expect(app.visibleNavigation().some((item) => item.id === 'users')).toBe(false);
  });

  it('loads audit pages with size 50 and descending timestamps', async () => {
    await signIn();
    const pending = app.setView('audit');
    const request = http.expectOne((request) => request.url === '/api/audit');
    expect(request.request.params.get('size')).toBe('50');
    expect(request.request.params.get('sort')).toBe('occurredAt,desc');
    request.flush(
      page([
        {
          id: 1,
          actorEmail: 'ana@example.com',
          action: 'CREATE',
          entityType: 'Product',
          entityId: '1',
          details: 'Producto creado',
          occurredAt: product.createdAt,
        },
      ]),
    );
    await pending;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Producto creado');
  });

  it('does not convert an inaccessible order detail into a successful empty order', async () => {
    await signIn('CUSTOMER');
    const pending = app.openOrderDetail(99);
    http
      .expectOne('/api/orders/99')
      .flush({ message: 'Orden no encontrada' }, { status: 404, statusText: 'Not Found' });
    await pending;
    expect(app.selectedOrder()).toBeNull();
    expect(app.modalError()).toBe('Orden no encontrada');
  });
});
