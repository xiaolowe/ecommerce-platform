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

export async function createOrder(productId: string, quantity: number) {
  const { data } = await http.post<Order>('/orders', { productId, quantity })
  return data
}

export async function getOrder(id: string) {
  const { data } = await http.get<Order>(`/orders/${id}`)
  return data
}
