import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import {
  AuditResponse,
  FrequentCustomer,
  InventoryResponse,
  OrderLineRequest,
  OrderResponse,
  Page,
  PageQuery,
  ProductQuery,
  ProductRequest,
  ProductResponse,
  ProductUpdateRequest,
  TopSellingProduct,
  UserResponse,
  UserUpdateRequest,
} from './api.models';
import { API_BASE_URL } from './auth.service';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  products(query: ProductQuery) {
    let params = this.pageParams(query);
    if (query.q) params = params.set('q', query.q);
    if (query.category) params = params.set('category', query.category);
    if (query.active !== undefined) params = params.set('active', query.active);
    return this.http.get<Page<ProductResponse>>(`${this.baseUrl}/products`, { params });
  }

  product(id: number) {
    return this.http.get<ProductResponse>(`${this.baseUrl}/products/${id}`);
  }

  createProduct(request: ProductRequest) {
    return this.http.post<ProductResponse>(`${this.baseUrl}/products`, request);
  }

  updateProduct(id: number, request: ProductUpdateRequest) {
    return this.http.put<ProductResponse>(`${this.baseUrl}/products/${id}`, request);
  }

  deactivateProduct(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/products/${id}`);
  }

  inventory(query: PageQuery) {
    return this.http.get<Page<InventoryResponse>>(`${this.baseUrl}/inventory`, {
      params: this.pageParams(query),
    });
  }

  adjustInventory(productId: number, delta: number, reason: string) {
    return this.http.post<InventoryResponse>(`${this.baseUrl}/inventory/${productId}/adjustments`, {
      delta,
      reason,
    });
  }

  orders(query: PageQuery) {
    return this.http.get<Page<OrderResponse>>(`${this.baseUrl}/orders`, {
      params: this.pageParams(query),
    });
  }

  order(id: number) {
    return this.http.get<OrderResponse>(`${this.baseUrl}/orders/${id}`);
  }

  createOrder(items: OrderLineRequest[]) {
    return this.http.post<OrderResponse>(`${this.baseUrl}/orders`, { items });
  }

  createRandomOrder(productCount: number) {
    return this.http.post<OrderResponse>(`${this.baseUrl}/orders/random`, { productCount });
  }

  cancelOrder(id: number) {
    return this.http.post<OrderResponse>(`${this.baseUrl}/orders/${id}/cancel`, {});
  }

  users(query: PageQuery) {
    return this.http.get<Page<UserResponse>>(`${this.baseUrl}/users`, {
      params: this.pageParams(query),
    });
  }

  user(id: number) {
    return this.http.get<UserResponse>(`${this.baseUrl}/users/${id}`);
  }

  updateUser(id: number, request: UserUpdateRequest) {
    return this.http.put<UserResponse>(`${this.baseUrl}/users/${id}`, request);
  }

  deactivateUser(id: number) {
    return this.http.delete<void>(`${this.baseUrl}/users/${id}`);
  }

  activeProducts(query: Pick<PageQuery, 'page' | 'size'>) {
    return this.http.get<Page<ProductResponse>>(`${this.baseUrl}/reports/products/active`, {
      params: this.pageParams(query),
    });
  }

  topSelling() {
    return this.http.get<TopSellingProduct[]>(`${this.baseUrl}/reports/products/top-selling`);
  }

  frequentCustomers() {
    return this.http.get<FrequentCustomer[]>(`${this.baseUrl}/reports/customers/frequent`);
  }

  audit(query: PageQuery) {
    return this.http.get<Page<AuditResponse>>(`${this.baseUrl}/audit`, {
      params: this.pageParams(query),
    });
  }

  private pageParams(query: PageQuery): HttpParams {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.sort) params = params.set('sort', query.sort);
    return params;
  }
}

export function apiErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0)
      return 'No se pudo conectar con la API. Comprueba que el backend esté disponible.';
    const body: unknown = error.error;
    if (
      typeof body === 'object' &&
      body !== null &&
      'message' in body &&
      typeof body.message === 'string'
    ) {
      return body.message;
    }
    switch (error.status) {
      case 401:
        return 'Credenciales inválidas o sesión vencida. Inicia sesión nuevamente.';
      case 403:
        return 'No tienes permisos para realizar esta operación.';
      case 404:
        return 'El recurso solicitado no existe o no está disponible.';
      case 409:
        return 'La operación entra en conflicto con el estado actual del recurso.';
      default:
        return `La API no pudo completar la operación (HTTP ${error.status}).`;
    }
  }
  return 'Ocurrió un error inesperado. Intenta nuevamente.';
}
