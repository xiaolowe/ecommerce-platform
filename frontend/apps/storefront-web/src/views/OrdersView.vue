<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listMyOrders, type Order } from '../services/api'
import { apiErrorMessage } from '../services/http'
import { formatMoney } from '../utils/money'

const orders = ref<Order[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const error = ref('')
const status = ref('')

const statusOptions = [
  { value: '', label: '全部' },
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'PAID', label: '已支付' },
  { value: 'CANCELLED', label: '已取消' },
]

function statusLabel(value: string) {
  const labels: Record<string, string> = {
    PENDING_STOCK: '处理中',
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    CANCELLED: '已取消',
  }
  return labels[value] ?? value
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await listMyOrders({
      page: page.value - 1,
      size: pageSize,
      status: status.value || undefined,
    })
    orders.value = result.content
    total.value = result.page.totalElements
    totalPages.value = result.page.totalPages
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  } finally {
    loading.value = false
  }
}

async function filter(value: string) {
  status.value = value
  page.value = 1
  await load()
}

async function goToPage(nextPage: number) {
  page.value = nextPage
  await load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(load)
</script>

<template>
  <section class="orders-page">
    <div class="section-heading">
      <div>
        <p class="kicker">YOUR ORDERS</p>
        <h1>我的订单</h1>
      </div>
      <span>{{ total }} 笔订单</span>
    </div>

    <div class="order-filters">
      <button
        v-for="option in statusOptions"
        :key="option.value"
        :class="{ active: status === option.value }"
        @click="filter(option.value)"
      >
        {{ option.label }}
      </button>
    </div>

    <p v-if="error" class="notice">{{ error }}</p>
    <div v-if="loading" class="orders-loading">正在读取订单…</div>
    <div v-else-if="orders.length" class="customer-order-list">
      <article v-for="order in orders" :key="order.id" class="customer-order-card">
        <header>
          <div>
            <small>{{ formatDate(order.createdAt) }}</small>
            <strong>订单 {{ order.id }}</strong>
          </div>
          <span class="status-pill" :class="{ paid: order.status === 'PAID' }">
            {{ statusLabel(order.status) }}
          </span>
        </header>

        <div class="customer-order-items">
          <div v-for="item in order.items" :key="item.productId">
            <span>{{ item.productName }} × {{ item.quantity }}</span>
            <strong>{{ formatMoney(item.unitPriceCents * item.quantity, order.currency) }}</strong>
          </div>
        </div>

        <footer>
          <div>
            <span>合计</span>
            <strong>{{ formatMoney(order.totalCents, order.currency) }}</strong>
            <small v-if="order.status === 'PENDING_PAYMENT' && order.paymentExpiresAt">
              请在 {{ formatDate(order.paymentExpiresAt) }} 前支付
            </small>
            <small v-if="order.cancelledAt">
              {{ order.cancellationReason === 'PAYMENT_TIMEOUT' ? '支付超时自动取消' : '订单已取消' }}
            </small>
          </div>
          <RouterLink :to="{ name: 'order', params: { id: order.id } }" class="text-link">
            {{ order.status === 'PENDING_PAYMENT' ? '去支付' : '查看详情' }}
          </RouterLink>
        </footer>
      </article>
    </div>

    <div v-else-if="!loading" class="empty-state">
      <h2>还没有订单</h2>
      <p>从精选商品中选一件，创建你的第一笔订单。</p>
      <RouterLink to="/" class="primary-button">继续购物</RouterLink>
    </div>

    <div v-if="totalPages > 1" class="order-pagination">
      <button :disabled="page <= 1" @click="goToPage(page - 1)">上一页</button>
      <span>第 {{ page }} / {{ totalPages }} 页</span>
      <button :disabled="page >= totalPages" @click="goToPage(page + 1)">下一页</button>
    </div>
  </section>
</template>
