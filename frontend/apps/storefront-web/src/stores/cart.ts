import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Product } from '../services/api'

export interface CartLine {
  productId: string
  sku: string
  name: string
  priceCents: number
  currency: string
  quantity: number
}

const CART_KEY = 'store.cart'

function loadCart(): CartLine[] {
  const raw = localStorage.getItem(CART_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw) as CartLine[]
  } catch {
    localStorage.removeItem(CART_KEY)
    return []
  }
}

export const useCartStore = defineStore('cart', () => {
  const lines = ref<CartLine[]>(loadCart())
  const count = computed(() => lines.value.reduce((sum, line) => sum + line.quantity, 0))
  const subtotalCents = computed(() =>
    lines.value.reduce((sum, line) => sum + line.priceCents * line.quantity, 0),
  )

  function persist() {
    localStorage.setItem(CART_KEY, JSON.stringify(lines.value))
  }

  function add(product: Product, quantity: number) {
    const line = lines.value.find((item) => item.productId === product.id)
    if (line) {
      line.quantity += quantity
    } else {
      lines.value.push({
        productId: product.id,
        sku: product.sku,
        name: product.name,
        priceCents: product.priceCents,
        currency: product.currency,
        quantity,
      })
    }
    persist()
  }

  function setQuantity(productId: string, quantity: number) {
    const line = lines.value.find((item) => item.productId === productId)
    if (!line) return
    if (quantity <= 0) {
      remove(productId)
      return
    }
    line.quantity = quantity
    persist()
  }

  function remove(productId: string) {
    lines.value = lines.value.filter((item) => item.productId !== productId)
    persist()
  }

  function clear() {
    lines.value = []
    persist()
  }

  return { lines, count, subtotalCents, add, setQuantity, remove, clear }
})
