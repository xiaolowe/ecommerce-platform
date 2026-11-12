import { http } from './http'

export interface User {
  id: string
  email: string
  displayName: string
  role: 'CUSTOMER' | 'ADMIN'
}

export interface TokenResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface Product {
  id: string
  sku: string
  name: string
  description?: string
  priceCents: number
  currency: string
  imageUrl?: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}

export interface Order {
  id: string
  customerId: string
  status: string
  totalCents: number
  currency: string
  paymentNo?: string | null
  paidAt?: string | null
  paymentExpiresAt?: string | null
  cancelledAt?: string | null
  cancellationReason?: string | null
  stockReleased: boolean
  shippingAddress?: {
    recipientName: string
    recipientPhone: string
    region: string
    detail: string
  } | null
  items: Array<{
    productId: string
    sku: string
    productName: string
    unitPriceCents: number
    quantity: number
  }>
  createdAt: string
  updatedAt: string
}

export interface Payment {
  id: string
  paymentNo: string
  orderId: string
  amountCents: number
  currency: string
  channel: 'MOCK'
  status: 'REQUIRES_ACTION' | 'SUCCEEDED' | 'CLOSED' | 'FAILED'
  providerTradeNo?: string | null
  orderSynced: boolean
  orderSyncAttempts: number
  lastOrderSyncError?: string | null
  manualReviewRequired: boolean
  confirmPath?: string | null
  expiresAt: string
  paidAt?: string | null
  createdAt: string
}

export interface CartItem {
  id: string
  productId: string
  sku: string
  productName: string
  unitPriceCents: number
  currency: string
  quantity: number
  subtotalCents: number
  updatedAt: string
}

export interface CartResponse {
  items: CartItem[]
  totalQuantity: number
  subtotalCents: number
  updatedAt?: string | null
}

export interface Address {
  id: string
  recipientName: string
  recipientPhone: string
  region: string
  detail: string
  defaultAddress: boolean
  createdAt: string
  updatedAt: string
}

export async function login(email: string, password: string) {
  const { data } = await http.post<TokenResponse>('/auth/login', { email, password })
  return data
}

export async function register(email: string, password: string, displayName: string) {
  const { data } = await http.post<TokenResponse>('/auth/register', {
    email,
    password,
    displayName,
  })
  return data
}

export async function listProducts() {
  const { data } = await http.get<PageResponse<Product>>('/products', {
    params: { page: 0, size: 50 },
  })
  return data
}

export async function getProduct(id: string) {
  const { data } = await http.get<Product>(`/products/${id}`)
  return data
}

export async function createOrder(
  items: Array<{ productId: string; quantity: number }>,
  addressId: string,
) {
  const { data } = await http.post<Order>('/orders', { items, addressId })
  return data
}

export async function getOrder(id: string) {
  const { data } = await http.get<Order>(`/orders/${id}`)
  return data
}

export async function cancelOrder(id: string) {
  const { data } = await http.post<Order>(`/orders/${id}/cancel`)
  return data
}

export async function listMyOrders(params: {
  page?: number
  size?: number
  status?: string
}) {
  const { data } = await http.get<PageResponse<Order>>('/orders', { params })
  return data
}

export async function createPayment(orderId: string) {
  const { data } = await http.post<Payment>('/payments', { orderId, channel: 'MOCK' })
  return data
}

export async function getPayment(paymentNo: string) {
  const { data } = await http.get<Payment>(`/payments/${paymentNo}`)
  return data
}

export async function mockPay(paymentNo: string) {
  const { data } = await http.post<Payment>(`/payments/${paymentNo}/mock-pay`)
  return data
}

export async function getCart() {
  const { data } = await http.get<CartResponse>('/cart')
  return data
}

export async function addCartItem(productId: string, quantity: number) {
  const { data } = await http.post<CartResponse>('/cart/items', { productId, quantity })
  return data
}

export async function updateCartItemQuantity(productId: string, quantity: number) {
  const { data } = await http.put<CartResponse>(`/cart/items/${productId}`, { quantity })
  return data
}

export async function removeCartItem(productId: string) {
  const { data } = await http.delete<CartResponse>(`/cart/items/${productId}`)
  return data
}

export async function clearCart() {
  await http.delete('/cart')
}

export async function listAddresses() {
  const { data } = await http.get<Address[]>('/addresses')
  return data
}

export async function createAddress(payload: {
  recipientName: string
  recipientPhone: string
  region: string
  detail: string
  defaultAddress: boolean
}) {
  const { data } = await http.post<Address>('/addresses', payload)
  return data
}

export async function updateAddress(
  id: string,
  payload: {
    recipientName: string
    recipientPhone: string
    region: string
    detail: string
    defaultAddress: boolean
  },
) {
  const { data } = await http.put<Address>(`/addresses/${id}`, payload)
  return data
}

export async function setDefaultAddress(id: string) {
  const { data } = await http.post<Address>(`/addresses/${id}/default`)
  return data
}

export async function deleteAddress(id: string) {
  await http.delete(`/addresses/${id}`)
}
