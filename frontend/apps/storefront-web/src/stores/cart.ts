import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  addCartItem,
  clearCart as clearServerCart,
  getCart,
  removeCartItem,
  updateCartItemQuantity,
  type CartResponse,
} from '../services/api'
import { apiErrorMessage } from '../services/http'

export interface CartLine {
  id: string
  productId: string
  sku: string
  name: string
  priceCents: number
  currency: string
  quantity: number
  updatedAt: string
}

export const useCartStore = defineStore('cart', () => {
  const lines = ref<CartLine[]>([])
  const loading = ref(false)
  const error = ref('')
  const count = computed(() => lines.value.reduce((sum, line) => sum + line.quantity, 0))
  const subtotalCents = computed(() =>
    lines.value.reduce((sum, line) => sum + line.priceCents * line.quantity, 0),
  )

  function applyCart(cart: CartResponse) {
    lines.value = cart.items.map((item) => ({
      id: item.id,
      productId: item.productId,
      sku: item.sku,
      name: item.productName,
      priceCents: item.unitPriceCents,
      currency: item.currency,
      quantity: item.quantity,
      updatedAt: item.updatedAt,
    }))
  }

  async function load() {
    loading.value = true
    error.value = ''
    try {
      applyCart(await getCart())
    } catch (reason) {
      error.value = apiErrorMessage(reason)
    } finally {
      loading.value = false
    }
  }

  async function add(productId: string, quantity: number) {
    error.value = ''
    try {
      applyCart(await addCartItem(productId, quantity))
    } catch (reason) {
      error.value = apiErrorMessage(reason)
      throw reason
    }
  }

  async function setQuantity(productId: string, quantity: number) {
    error.value = ''
    try {
      applyCart(
        quantity <= 0
          ? await removeCartItem(productId)
          : await updateCartItemQuantity(productId, quantity),
      )
    } catch (reason) {
      error.value = apiErrorMessage(reason)
      throw reason
    }
  }

  async function remove(productId: string) {
    error.value = ''
    try {
      applyCart(await removeCartItem(productId))
    } catch (reason) {
      error.value = apiErrorMessage(reason)
      throw reason
    }
  }

  async function clear() {
    error.value = ''
    try {
      await clearServerCart()
      lines.value = []
    } catch (reason) {
      error.value = apiErrorMessage(reason)
      throw reason
    }
  }

  function reset() {
    lines.value = []
    loading.value = false
    error.value = ''
  }

  return { lines, loading, error, count, subtotalCents, load, add, setQuantity, remove, clear, reset }
})
