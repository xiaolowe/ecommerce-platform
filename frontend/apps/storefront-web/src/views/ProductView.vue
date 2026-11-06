<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createOrder, getProduct, type Product } from '../services/api'
import { apiErrorMessage } from '../services/http'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { formatMoney } from '../utils/money'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const product = ref<Product | null>(null)
const quantity = ref(1)
const loading = ref(true)
const ordering = ref(false)
const error = ref('')

onMounted(async () => {
  try {
    product.value = await getProduct(String(route.params.id))
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  } finally {
    loading.value = false
  }
})

function addToCart() {
  if (!product.value) return
  cart.add(product.value, quantity.value)
}

async function buyNow() {
  if (!product.value) return
  if (!auth.isAuthenticated) {
    await router.push({ name: 'auth', query: { redirect: route.fullPath } })
    return
  }
  ordering.value = true
  try {
    const order = await createOrder(product.value.id, quantity.value)
    await router.push({ name: 'order', params: { id: order.id } })
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  } finally {
    ordering.value = false
  }
}
</script>

<template>
  <section v-if="loading" class="product-detail skeleton-detail"></section>
  <section v-else-if="product" class="product-detail">
    <div class="detail-visual">
      <span>{{ product.name.slice(0, 1).toUpperCase() }}</span>
    </div>
    <div class="detail-copy">
      <p class="kicker">{{ product.sku }}</p>
      <h1>{{ product.name }}</h1>
      <p class="detail-description">{{ product.description || '一件经过认真挑选的日常之物。' }}</p>
      <strong class="detail-price">{{ formatMoney(product.priceCents, product.currency) }}</strong>

      <div class="quantity-row">
        <label>数量</label>
        <button @click="quantity = Math.max(1, quantity - 1)">−</button>
        <span>{{ quantity }}</span>
        <button @click="quantity += 1">+</button>
      </div>

      <p v-if="error" class="notice">{{ error }}</p>
      <div class="action-row">
        <button class="secondary-button" @click="addToCart">加入购物袋</button>
        <button class="primary-button" :disabled="ordering" @click="buyNow">
          {{ ordering ? '创建订单中…' : '立即购买' }}
        </button>
      </div>
      <p class="fine-print">当前基础版本按单个商品创建订单，购物袋批量结算将在订单接口支持多明细后接入。</p>
    </div>
  </section>
  <section v-else class="empty-state">
    <h1>商品未找到</h1>
    <p>{{ error }}</p>
    <RouterLink to="/" class="primary-button">返回首页</RouterLink>
  </section>
</template>
