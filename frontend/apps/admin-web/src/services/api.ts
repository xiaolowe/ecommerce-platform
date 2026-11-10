import { http } from './http'

export interface User {
  id: string
  email: string
  displayName: string
  role: 'ADMIN' | 'CUSTOMER'
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

export interface Stock {
  productId: string
  availableStock: number
  reservedStock: number
  updatedAt: string
}

export interface MediaAsset {
  id: string
  objectKey: string
  url: string
  originalName: string
  contentType: string
  sizeBytes: number
  width: number
  height: number
  createdAt: string
}

export type OrderStatus = 'PENDING_STOCK' | 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED'

export interface Order {
  id: string
  customerId: string
  status: OrderStatus
  totalCents: number
  currency: string
  paymentNo?: string | null
  paidAt?: string | null
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

export type PaymentStatus = 'REQUIRES_ACTION' | 'SUCCEEDED' | 'CLOSED' | 'FAILED'

export interface Payment {
  id: string
  paymentNo: string
  orderId: string
  amountCents: number
  currency: string
  channel: string
  status: PaymentStatus
  providerTradeNo?: string | null
  orderSynced: boolean
  confirmPath?: string | null
  expiresAt: string
  paidAt?: string | null
  createdAt: string
}

export async function login(email: string, password: string) {
  const { data } = await http.post<TokenResponse>('/auth/login', { email, password })
  return data
}

export async function listProducts(page = 0, size = 50) {
  const { data } = await http.get<PageResponse<Product>>('/products', {
    params: { page, size },
  })
  return data
}

export async function createProduct(payload: {
  sku: string
  name: string
  description: string
  priceCents: number
  currency: string
  imageUrl?: string
}) {
  const { data } = await http.post<Product>('/products', payload)
  return data
}

export async function updateProductImage(productId: string, imageUrl: string | null) {
  const { data } = await http.patch<Product>(`/products/${productId}/image`, { imageUrl })
  return data
}

export async function listMediaAssets(page = 0, size = 24) {
  const { data } = await http.get<PageResponse<MediaAsset>>('/media/images', {
    params: { page, size },
  })
  return data
}

export async function uploadImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<MediaAsset>('/media/images', form, {
    timeout: 30_000,
  })
  return data
}

export async function deleteMediaAsset(id: string) {
  await http.delete(`/media/images/${id}`)
}

export async function createStock(productId: string, availableStock: number) {
  const { data } = await http.post<Stock>('/inventory/stocks', {
    productId,
    availableStock,
  })
  return data
}

export async function getStock(productId: string) {
  const { data } = await http.get<Stock>(`/inventory/stocks/${productId}`)
  return data
}

export async function listAdminOrders(params: {
  page?: number
  size?: number
  status?: string
  keyword?: string
}) {
  const { data } = await http.get<PageResponse<Order>>('/admin/orders', { params })
  return data
}

export async function listAdminPayments(params: {
  page?: number
  size?: number
  status?: string
  keyword?: string
}) {
  const { data } = await http.get<PageResponse<Payment>>('/admin/payments', { params })
  return data
}
