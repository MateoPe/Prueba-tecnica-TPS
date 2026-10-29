import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, forkJoin, Observable } from 'rxjs';
import { ApiService, apiErrorMessage } from './api.service';
import { AuthService } from './auth.service';
import {
  AuditResponse,
  FrequentCustomer,
  InventoryResponse,
  OrderLineRequest,
  OrderResponse,
  Page,
  ProductResponse,
  Role,
  TopSellingProduct,
  UserResponse,
} from './api.models';

type View =
  'dashboard' | 'products' | 'inventory' | 'orders' | 'customers' | 'users' | 'reports' | 'audit';
type Modal = 'product' | 'order' | 'user' | 'adjustment' | 'detail' | 'confirm' | null;
interface NavigationItem {
  id: View;
  label: string;
  icon: string;
  admin?: boolean;
}
interface Confirmation {
  kind: 'product' | 'user' | 'order';
  id: number;
  label: string;
}

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);
  private loadId = 0;
  private modalLoadId = 0;

  readonly activeView = signal<View>('dashboard');
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly modalError = signal('');
  readonly toast = signal('');
  readonly authMode = signal<'login' | 'register'>('login');
  readonly authError = signal('');
  readonly modal = signal<Modal>(null);
  readonly editingId = signal<number | null>(null);
  readonly selectedOrder = signal<OrderResponse | null>(null);
  readonly selectedInventory = signal<InventoryResponse | null>(null);
  readonly confirmation = signal<Confirmation | null>(null);
  readonly modalLoading = signal(false);
  readonly productPage = signal<Page<ProductResponse> | null>(null);
  readonly inventoryPage = signal<Page<InventoryResponse> | null>(null);
  readonly orderPage = signal<Page<OrderResponse> | null>(null);
  readonly userPage = signal<Page<UserResponse> | null>(null);
  readonly activeProductPage = signal<Page<ProductResponse> | null>(null);
  readonly auditPage = signal<Page<AuditResponse> | null>(null);
  readonly topSelling = signal<TopSellingProduct[]>([]);
  readonly customers = signal<FrequentCustomer[]>([]);
  readonly pickerPage = signal<Page<ProductResponse> | null>(null);
  readonly orderItems = signal<(OrderLineRequest & { name: string })[]>([]);
  private toastTimer?: ReturnType<typeof setTimeout>;

  loginEmail = '';
  loginPassword = '';
  registerName = '';
  query = '';
  category = '';
  productActive = 'true';
  productSort = 'name,asc';
  pickerQuery = '';
  pickerProductId: number | null = null;
  pickerQuantity = 1;
  randomOrder = false;
  randomProductCount = 1;
  adjustmentDelta = 1;
  adjustmentReason = '';
  readonly productDraft = {
    name: '',
    sku: '',
    description: '',
    category: '',
    price: 0,
    active: true,
    initialStock: 0,
  };
  readonly userDraft: { fullName: string; role: Role; active: boolean; password: string } = {
    fullName: '',
    role: 'CUSTOMER',
    active: true,
    password: '',
  };
  readonly currentDateLabel = new Intl.DateTimeFormat('es-CO', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date());
  private readonly navigation: NavigationItem[] = [
    { id: 'dashboard', label: 'Resumen', icon: '⌂' },
    { id: 'products', label: 'Productos', icon: '▦' },
    { id: 'inventory', label: 'Inventario', icon: '▤', admin: true },
    { id: 'orders', label: 'Pedidos', icon: '▣' },
    { id: 'customers', label: 'Clientes frecuentes', icon: '♧', admin: true },
    { id: 'users', label: 'Usuarios', icon: '♙', admin: true },
    { id: 'reports', label: 'Reportes', icon: '▥', admin: true },
    { id: 'audit', label: 'Auditoría', icon: '☷', admin: true },
  ];
  readonly visibleNavigation = computed(() =>
    this.navigation.filter((item) => !item.admin || this.auth.isAdmin()),
  );
  readonly viewLabel = computed(
    () => this.navigation.find((item) => item.id === this.activeView())?.label ?? '',
  );
  readonly currentPage = computed(() => {
    switch (this.activeView()) {
      case 'products':
        return this.productPage();
      case 'inventory':
        return this.inventoryPage();
      case 'orders':
        return this.orderPage();
      case 'users':
        return this.userPage();
      case 'reports':
        return this.activeProductPage();
      case 'audit':
        return this.auditPage();
      default:
        return null;
    }
  });
  readonly recentRevenue = computed(() =>
    (this.orderPage()?.content ?? [])
      .filter((order) => order.status === 'COMPLETED')
      .reduce((total, order) => total + order.total, 0),
  );

  constructor() {
    effect(() => {
      if (!this.auth.user()) {
        this.loadId++;
        this.modalLoadId++;
        this.modal.set(null);
        this.modalLoading.set(false);
        this.editingId.set(null);
        this.confirmation.set(null);
        this.loading.set(false);
        this.saving.set(false);
        this.productPage.set(null);
        this.inventoryPage.set(null);
        this.orderPage.set(null);
        this.userPage.set(null);
        this.activeProductPage.set(null);
        this.auditPage.set(null);
        this.topSelling.set([]);
        this.customers.set([]);
        this.pickerPage.set(null);
        this.orderItems.set([]);
        this.selectedOrder.set(null);
        this.selectedInventory.set(null);
        this.userDraft.password = '';
        this.error.set('');
        this.modalError.set('');
        this.toast.set('');
      }
    });
    this.destroyRef.onDestroy(() => {
      this.loadId++;
      this.modalLoadId++;
      clearTimeout(this.toastTimer);
    });
  }

  setAuthMode(mode: 'login' | 'register'): void {
    this.authMode.set(mode);
    this.authError.set('');
    this.auth.notice.set('');
    this.loginPassword = '';
  }

  async submitAuth(): Promise<void> {
    if (this.saving()) return;
    this.authError.set('');
    if (
      !this.loginEmail.trim() ||
      !this.loginPassword ||
      (this.authMode() === 'register' &&
        (!this.registerName.trim() ||
          this.loginPassword.length < 8 ||
          this.loginPassword.length > 72))
    ) {
      this.authError.set(
        'Completa los datos. Para registrarte, la contraseña debe tener entre 8 y 72 caracteres.',
      );
      return;
    }
    this.saving.set(true);
    try {
      if (this.authMode() === 'register') {
        await firstValueFrom(
          this.auth.register({
            email: this.loginEmail.trim(),
            fullName: this.registerName.trim(),
            password: this.loginPassword,
          }),
        );
        this.authMode.set('login');
        this.loginPassword = '';
        this.auth.notice.set('Cuenta creada. Inicia sesión con tus credenciales.');
      } else {
        await firstValueFrom(this.auth.login(this.loginEmail.trim(), this.loginPassword));
        this.loginPassword = '';
        this.activeView.set('dashboard');
        await this.loadView();
      }
    } catch (error: unknown) {
      this.authError.set(apiErrorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  logout(): void {
    this.auth.logout();
    this.loginPassword = '';
  }

  async setView(view: View): Promise<void> {
    const navigation = this.navigation.find((item) => item.id === view);
    if (!this.auth.user() || (navigation?.admin && !this.auth.isAdmin())) {
      this.error.set('No tienes permisos para acceder a esta sección.');
      return;
    }
    this.activeView.set(view);
    this.query = '';
    this.category = '';
    this.productActive = 'true';
    await this.loadView();
  }

  async loadView(page = 0): Promise<void> {
    if (!this.auth.user()) return;
    const view = this.activeView();
    const requestId = ++this.loadId;
    const query = { page, size: 20 };
    const isCurrent = () => requestId === this.loadId && !this.destroyRef.destroyed;
    this.loading.set(true);
    this.error.set('');
    try {
      switch (view) {
        case 'products': {
          const result = await firstValueFrom(
            this.api.products({
              ...query,
              q: this.query.trim(),
              category: this.category.trim(),
              active: this.productActive === 'true',
              sort: this.productSort,
            }),
          );
          if (isCurrent()) this.productPage.set(result);
          break;
        }
        case 'inventory': {
          const result = await firstValueFrom(this.api.inventory(query));
          if (isCurrent()) this.inventoryPage.set(result);
          break;
        }
        case 'orders': {
          const result = await firstValueFrom(
            this.api.orders({ ...query, sort: 'createdAt,desc' }),
          );
          if (isCurrent()) this.orderPage.set(result);
          break;
        }
        case 'users': {
          const result = await firstValueFrom(this.api.users({ ...query, sort: 'fullName,asc' }));
          if (isCurrent()) this.userPage.set(result);
          break;
        }
        case 'audit': {
          const result = await firstValueFrom(
            this.api.audit({ page, size: 50, sort: 'occurredAt,desc' }),
          );
          if (isCurrent()) this.auditPage.set(result);
          break;
        }
        case 'customers': {
          const result = await firstValueFrom(this.api.frequentCustomers());
          if (isCurrent()) this.customers.set(result);
          break;
        }
        case 'reports': {
          const result = await firstValueFrom(
            forkJoin({
              active: this.api.activeProducts(query),
              top: this.api.topSelling(),
              customers: this.api.frequentCustomers(),
            }),
          );
          if (isCurrent()) {
            this.activeProductPage.set(result.active);
            this.topSelling.set(result.top);
            this.customers.set(result.customers);
          }
          break;
        }
        case 'dashboard': {
          if (this.auth.isAdmin()) {
            const result = await firstValueFrom(
              forkJoin({
                orders: this.api.orders({ page: 0, size: 5, sort: 'createdAt,desc' }),
                active: this.api.activeProducts({ page: 0, size: 1 }),
                top: this.api.topSelling(),
                customers: this.api.frequentCustomers(),
              }),
            );
            if (isCurrent()) {
              this.orderPage.set(result.orders);
              this.activeProductPage.set(result.active);
              this.topSelling.set(result.top);
              this.customers.set(result.customers);
            }
          } else {
            const result = await firstValueFrom(
              this.api.orders({ page: 0, size: 5, sort: 'createdAt,desc' }),
            );
            if (isCurrent()) this.orderPage.set(result);
          }
          break;
        }
      }
    } catch (error: unknown) {
      if (isCurrent()) this.error.set(apiErrorMessage(error));
    } finally {
      if (isCurrent()) this.loading.set(false);
    }
  }

  async changePage(delta: number): Promise<void> {
    const page = this.currentPage();
    if (!page || this.loading()) return;
    const next = page.number + delta;
    if (next < 0 || next >= page.totalPages) return;
    await this.loadView(next);
  }

  openProduct(product?: ProductResponse): void {
    this.prepareModal('product');
    this.editingId.set(product?.id ?? null);
    Object.assign(
      this.productDraft,
      product
        ? {
            name: product.name,
            sku: product.sku,
            description: product.description ?? '',
            category: product.category,
            price: product.price,
            active: product.active,
            initialStock: product.stock,
          }
        : {
            name: '',
            sku: '',
            description: '',
            category: '',
            price: 0,
            active: true,
            initialStock: 0,
          },
    );
  }

  async saveProduct(): Promise<void> {
    const { initialStock, ...data } = this.productDraft;
    const request = {
      ...data,
      name: data.name.trim(),
      sku: data.sku.trim(),
      category: data.category.trim(),
    };
    if (
      !request.name ||
      !request.sku ||
      !request.category ||
      !Number.isFinite(request.price) ||
      request.price < 0.01 ||
      Math.abs(request.price * 100 - Math.round(request.price * 100)) > 0.00001 ||
      (this.editingId() === null && (!Number.isInteger(initialStock) || initialStock < 0))
    ) {
      this.modalError.set(
        'Completa los campos. El precio debe ser positivo, con máximo dos decimales, y el stock un entero no negativo.',
      );
      return;
    }
    const id = this.editingId();
    await this.mutate(
      id === null
        ? this.api.createProduct({ ...request, initialStock })
        : this.api.updateProduct(id, request),
      () => this.modal.set(null),
      id === null ? 'Producto creado.' : 'Producto actualizado.',
    );
  }

  openAdjustment(inventory: InventoryResponse): void {
    this.prepareModal('adjustment');
    this.selectedInventory.set(inventory);
    this.adjustmentDelta = 1;
    this.adjustmentReason = '';
  }

  async saveAdjustment(): Promise<void> {
    const inventory = this.selectedInventory();
    if (
      !inventory ||
      !Number.isInteger(this.adjustmentDelta) ||
      this.adjustmentDelta === 0 ||
      !this.adjustmentReason.trim()
    ) {
      this.modalError.set('Ingresa un ajuste entero distinto de cero y el motivo.');
      return;
    }
    await this.mutate(
      this.api.adjustInventory(
        inventory.productId,
        this.adjustmentDelta,
        this.adjustmentReason.trim(),
      ),
      () => this.modal.set(null),
      'Inventario actualizado.',
    );
  }

  openUser(user: UserResponse): void {
    this.prepareModal('user');
    this.editingId.set(user.id);
    Object.assign(this.userDraft, {
      fullName: user.fullName,
      role: user.role,
      active: user.active,
      password: '',
    });
  }

  async saveUser(): Promise<void> {
    const id = this.editingId();
    if (
      id === null ||
      !this.userDraft.fullName.trim() ||
      (this.userDraft.password &&
        (this.userDraft.password.length < 8 || this.userDraft.password.length > 72))
    ) {
      this.modalError.set(
        'El nombre es obligatorio. La nueva contraseña debe tener entre 8 y 72 caracteres.',
      );
      return;
    }
    await this.mutate(
      this.api.updateUser(id, {
        fullName: this.userDraft.fullName.trim(),
        role: this.userDraft.role,
        active: this.userDraft.active,
        password: this.userDraft.password || null,
      }),
      (user) => {
        this.modal.set(null);
        this.userDraft.password = '';
        if (this.auth.user()?.id === user.id) {
          if (!user.active) this.logout();
          else {
            this.auth.updateUser(user);
            if (!this.auth.isAdmin()) this.activeView.set('dashboard');
          }
        }
      },
      'Usuario actualizado.',
    );
  }

  async openOrder(): Promise<void> {
    this.prepareModal('order');
    this.orderItems.set([]);
    this.pickerQuery = '';
    this.pickerProductId = null;
    this.pickerQuantity = 1;
    this.randomOrder = false;
    this.randomProductCount = 1;
    this.pickerPage.set(null);
    await this.loadPicker();
  }

  async loadPicker(page = 0): Promise<void> {
    const requestId = ++this.modalLoadId;
    this.modalLoading.set(true);
    this.modalError.set('');
    this.pickerPage.set(null);
    this.pickerProductId = null;
    try {
      const result = await firstValueFrom(
        this.api.products({
          page,
          size: 20,
          q: this.pickerQuery.trim(),
          active: true,
          sort: 'name,asc',
        }),
      );
      if (requestId === this.modalLoadId && this.modal() === 'order') {
        this.pickerPage.set(result);
        this.pickerProductId = result.content.find((product) => product.stock > 0)?.id ?? null;
      }
    } catch (error: unknown) {
      if (requestId === this.modalLoadId) this.modalError.set(apiErrorMessage(error));
    } finally {
      if (requestId === this.modalLoadId) this.modalLoading.set(false);
    }
  }

  addOrderItem(): void {
    const product = this.pickerPage()?.content.find(
      (item) => item.id === Number(this.pickerProductId),
    );
    const existingQuantity =
      this.orderItems().find((item) => item.productId === product?.id)?.quantity ?? 0;
    if (
      !product ||
      !Number.isInteger(this.pickerQuantity) ||
      this.pickerQuantity < 1 ||
      existingQuantity + this.pickerQuantity > product.stock
    ) {
      this.modalError.set('Selecciona un producto y una cantidad entera que no supere su stock.');
      return;
    }
    this.orderItems.update((items) =>
      existingQuantity
        ? items.map((item) =>
            item.productId === product.id
              ? { ...item, quantity: item.quantity + this.pickerQuantity }
              : item,
          )
        : [...items, { productId: product.id, name: product.name, quantity: this.pickerQuantity }],
    );
    this.modalError.set('');
  }

  removeOrderItem(productId: number): void {
    this.orderItems.update((items) => items.filter((item) => item.productId !== productId));
  }

  async saveOrder(): Promise<void> {
    if (
      (this.randomOrder &&
        (!Number.isInteger(this.randomProductCount) ||
          this.randomProductCount < 1 ||
          this.randomProductCount > 10)) ||
      (!this.randomOrder && this.orderItems().length === 0)
    ) {
      this.modalError.set(
        'Agrega al menos un producto o indica de 1 a 10 productos para el pedido aleatorio.',
      );
      return;
    }
    const request = this.randomOrder
      ? this.api.createRandomOrder(this.randomProductCount)
      : this.api.createOrder(
          this.orderItems().map(({ productId, quantity }) => ({ productId, quantity })),
        );
    await this.mutate(
      request,
      (order) => {
        this.selectedOrder.set(order);
        this.modal.set('detail');
      },
      'Pedido registrado. Descuento y total calculados por el backend.',
    );
  }

  async openOrderDetail(id: number): Promise<void> {
    this.prepareModal('detail');
    const requestId = ++this.modalLoadId;
    this.selectedOrder.set(null);
    this.modalLoading.set(true);
    try {
      const order = await firstValueFrom(this.api.order(id));
      if (requestId === this.modalLoadId) this.selectedOrder.set(order);
    } catch (error: unknown) {
      if (requestId === this.modalLoadId) this.modalError.set(apiErrorMessage(error));
    } finally {
      if (requestId === this.modalLoadId) this.modalLoading.set(false);
    }
  }

  confirmAction(kind: Confirmation['kind'], id: number, label: string): void {
    this.prepareModal('confirm');
    this.confirmation.set({ kind, id, label });
  }

  async executeConfirmation(): Promise<void> {
    const confirmation = this.confirmation();
    if (!confirmation) {
      this.modalError.set('No hay una operación seleccionada.');
      return;
    }
    const { kind, id } = confirmation;
    if (kind === 'order') {
      await this.mutate(
        this.api.cancelOrder(id),
        (order) => {
          this.selectedOrder.set(order);
          this.modal.set('detail');
        },
        'Pedido cancelado. El stock fue reintegrado.',
      );
    } else {
      const request =
        kind === 'product' ? this.api.deactivateProduct(id) : this.api.deactivateUser(id);
      await this.mutate(
        request,
        () => {
          this.modal.set(null);
          if (kind === 'user' && this.auth.user()?.id === id) this.logout();
        },
        kind === 'product'
          ? 'Producto desactivado; su historial e inventario se conservan.'
          : 'Cuenta desactivada.',
      );
    }
  }

  closeModal(): void {
    if (this.saving()) return;
    this.modalLoadId++;
    this.modal.set(null);
    this.modalLoading.set(false);
    this.userDraft.password = '';
  }

  private prepareModal(modal: Modal): void {
    this.modalLoadId++;
    this.modalError.set('');
    this.modalLoading.set(false);
    this.modal.set(modal);
  }

  private async mutate<T>(
    request: Observable<T>,
    onSuccess: (value: T) => void,
    message: string,
  ): Promise<void> {
    if (this.saving()) return;
    const user = this.auth.user();
    if (!user) {
      this.modalError.set('Inicia sesión para continuar.');
      return;
    }
    this.saving.set(true);
    this.modalError.set('');
    try {
      const result = await firstValueFrom(request);
      if (this.destroyRef.destroyed || this.auth.user()?.id !== user.id) return;
      onSuccess(result);
      this.notify(message);
      if (this.auth.user()) await this.loadView();
    } catch (error: unknown) {
      if (this.auth.user()?.id === user.id) this.modalError.set(apiErrorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 2,
    }).format(value);
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(
      new Date(value),
    );
  }

  initials(name: string): string {
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase();
  }

  notify(message: string): void {
    clearTimeout(this.toastTimer);
    this.toast.set(message);
    this.toastTimer = setTimeout(() => this.toast.set(''), 4000);
  }
}
