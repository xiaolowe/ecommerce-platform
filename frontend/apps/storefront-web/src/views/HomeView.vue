<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listProducts, type Product } from '../services/api'
import { apiErrorMessage } from '../services/http'
import { formatMoney } from '../utils/money'

const products = ref<Product[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    products.value = (await listProducts()).content
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="hero">
    <div>
      <p class="kicker">NEW SEASON · 2026</p>
      <h1>把日常所需，<br />做得更好一点。</h1>
      <p class="hero-copy">精选品质商品，清晰价格，可靠库存。从这里开始一次简单的购物体验。</p>
      <a href="#collection" class="primary-button">浏览本季精选</a>
    </div>
    <div class="hero-object" aria-hidden="true">
      <span>N</span>
      <small>Objects for better days.</small>
    </div>
  </section>

  <section id="collection" class="collection">
    <div class="section-heading">
      <div>
        <p class="kicker">CURATED COLLECTION</p>
        <h2>本周精选</h2>
      </div>
      <span>{{ products.length }} 件商品</span>
    </div>

    <p v-if="error" class="notice">{{ error }}</p>
    <div v-if="loading" class="product-grid">
      <div v-for="index in 6" :key="index" class="product-card skeleton"></div>
    </div>
    <div v-else-if="products.length" class="product-grid">
      <RouterLink
        v-for="(product, index) in products"
        :key="product.id"
        :to="`/products/${product.id}`"
        class="product-card"
      >
        <div class="product-visual" :class="`tone-${index % 4}`">
          <span>{{ product.name.slice(0, 1).toUpperCase() }}</span>
        </div>
        <div class="product-body">
          <small>{{ product.sku }}</small>
          <h3>{{ product.name }}</h3>
          <p>{{ product.description || '为日常挑选的可靠商品。' }}</p>
          <strong>{{ formatMoney(product.priceCents, product.currency) }}</strong>
        </div>
      </RouterLink>
    </div>
    <div v-else class="empty-state">
      <h3>商店正在准备商品</h3>
      <p>管理员上架商品后，会在这里自动展示。</p>
    </div>
  </section>
</template>
