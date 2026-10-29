export type Role = 'ADMIN' | 'CUSTOMER';

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface RegisterRequest {
  email: string;
  fullName: string;
  password: string;
}

export interface ProductResponse {
  id: number;
  sku: string;
  name: string;
  description: string | null;
  category: string;
  price: number;
  active: boolean;
  stock: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductUpdateRequest {
  sku: string;
  name: string;
  description: string;
  category: string;
  price: number;
  active: boolean;
}

export interface ProductRequest extends ProductUpdateRequest {
  initialStock: number;
}

export interface InventoryResponse {
  productId: number;
  sku: string;
  productName: string;
  quantity: number;
  updatedAt: string;
}

export interface OrderLineRequest {
  productId: number;
  quantity: number;
}

export interface OrderItemResponse extends OrderLineRequest {
  productName: string;
  productSku: string;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderResponse {
  id: number;
  customerId: number;
  customerEmail: string;
  items: OrderItemResponse[];
  subtotal: number;
  discountPercent: number;
  total: number;
  randomOrder: boolean;
  status: 'COMPLETED' | 'CANCELLED';
  createdAt: string;
}

export interface UserUpdateRequest {
  fullName: string;
  role: Role;
  active: boolean;
  password?: string | null;
}

export interface TopSellingProduct {
  productId: number;
  name: string;
  sku: string;
  unitsSold: number;
}

export interface FrequentCustomer {
  customerId: number;
  fullName: string;
  email: string;
  completedOrders: number;
}

export interface AuditResponse {
  id: number;
  actorEmail: string;
  action: string;
  entityType: string;
  entityId: string;
  details: string;
  occurredAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface PageQuery {
  page: number;
  size: number;
  sort?: string;
}

export interface ProductQuery extends PageQuery {
  q?: string;
  category?: string;
  active?: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
