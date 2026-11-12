<script setup lang="ts">
import { useCartStore, type CartLine } from '../stores/cart'
import { formatMoney } from '../utils/money'

const cart = useCartStore()

async function changeQuantity(line: CartLine, quantity: number) {
  try {
    await cart.setQuantity(line.productId, quantity)
  } catch {
    // The cart store exposes the backend error for display.
  }
}

async function removeLine(line: CartLine) {
  try {
    await cart.remove(line.productId)
  } catch {
    // The cart store exposes the backend error for display.
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

    <p v-if="cart.error" class="notice">{{ cart.error }}</p>
    <p v-if="cart.loading" class="notice neutral">正在同步购物袋…</p>
    <div v-else-if="cart.lines.length" class="cart-list">
      <article v-for="line in cart.lines" :key="line.productId" class="cart-line">
        <div class="cart-thumb">{{ line.name.slice(0, 1).toUpperCase() }}</div>
        <div class="cart-info">
          <small>{{ line.sku }}</small>
          <RouterLink :to="`/products/${line.productId}`">{{ line.name }}</RouterLink>
          <span>{{ formatMoney(line.priceCents, line.currency) }}</span>
        </div>
        <div class="quantity-row compact">
          <button @click="changeQuantity(line, line.quantity - 1)">−</button>
          <span>{{ line.quantity }}</span>
          <button @click="changeQuantity(line, line.quantity + 1)">+</button>
        </div>
        <button class="text-button remove" @click="removeLine(line)">移除</button>
      </article>
      <div class="cart-checkout-bar">
        <div>
          <span>商品合计</span>
          <strong>{{ formatMoney(cart.subtotalCents, cart.lines[0]?.currency ?? 'CNY') }}</strong>
        </div>
        <RouterLink to="/checkout" class="primary-button">去结算</RouterLink>
      </div>
    </div>
    <div v-else class="empty-state">
      <h2>购物袋还是空的</h2>
      <p>从精选商品中挑一件带走吧。</p>
      <RouterLink to="/" class="primary-button">继续购物</RouterLink>
    </div>
  </section>
</template>
