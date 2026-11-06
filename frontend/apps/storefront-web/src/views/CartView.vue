<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createOrder } from '../services/api'
import { apiErrorMessage } from '../services/http'
import { useCartStore, type CartLine } from '../stores/cart'
import { formatMoney } from '../utils/money'

const cart = useCartStore()
const router = useRouter()
const orderingId = ref('')
const error = ref('')

async function checkout(line: CartLine) {
  orderingId.value = line.productId
  error.value = ''
  try {
    const order = await createOrder(line.productId, line.quantity)
    cart.remove(line.productId)
    await router.push({ name: 'order', params: { id: order.id } })
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  } finally {
    orderingId.value = ''
  }
}
</script>

<template>
  <section class="cart-page">
    <div class="section-heading">
      <div>
        <p class="kicker">YOUR BAG</p>
        <h1>购物袋</h1>
      </div>
      <span>{{ cart.count }} 件商品</span>
    </div>

    <p v-if="error" class="notice">{{ error }}</p>
    <div v-if="cart.lines.length" class="cart-list">
      <article v-for="line in cart.lines" :key="line.productId" class="cart-line">
        <div class="cart-thumb">{{ line.name.slice(0, 1).toUpperCase() }}</div>
        <div class="cart-info">
          <small>{{ line.sku }}</small>
          <RouterLink :to="`/products/${line.productId}`">{{ line.name }}</RouterLink>
          <span>{{ formatMoney(line.priceCents, line.currency) }}</span>
        </div>
        <div class="quantity-row compact">
          <button @click="cart.setQuantity(line.productId, line.quantity - 1)">−</button>
          <span>{{ line.quantity }}</span>
          <button @click="cart.setQuantity(line.productId, line.quantity + 1)">+</button>
        </div>
        <button
          class="primary-button small"
          :disabled="orderingId === line.productId"
          @click="checkout(line)"
        >
          {{ orderingId === line.productId ? '提交中…' : '下单' }}
        </button>
        <button class="text-button remove" @click="cart.remove(line.productId)">移除</button>
      </article>
    </div>
    <div v-else class="empty-state">
      <h2>购物袋还是空的</h2>
      <p>从精选商品中挑一件带走吧。</p>
      <RouterLink to="/" class="primary-button">继续购物</RouterLink>
    </div>
  </section>
</template>
