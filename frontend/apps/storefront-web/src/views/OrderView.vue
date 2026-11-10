<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { createPayment, getOrder, mockPay, type Order } from '../services/api'
import { apiErrorMessage } from '../services/http'
import { formatMoney } from '../utils/money'

const route = useRoute()
const order = ref<Order | null>(null)
const error = ref('')
const notice = ref('')
const paying = ref(false)

const statusLabel = computed(() => {
  const labels: Record<string, string> = {
    PENDING_STOCK: '处理中',
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    CANCELLED: '已取消',
  }
  return order.value ? (labels[order.value.status] ?? order.value.status) : ''
})

function formatDate(value?: string | null) {
  return value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : ''
}

async function loadOrder() {
  order.value = await getOrder(String(route.params.id))
}

async function pay() {
  if (!order.value) {
    return
  }
  paying.value = true
  error.value = ''
  notice.value = ''
  try {
    const payment = await createPayment(order.value.id)
    const result = await mockPay(payment.paymentNo)
    await loadOrder()
    notice.value = result.orderSynced
      ? '支付成功，订单已更新为已支付。'
      : '支付成功，订单状态正在同步，请稍后刷新。'
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  } finally {
    paying.value = false
  }
}

onMounted(async () => {
  try {
    await loadOrder()
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  }
})
</script>

<template>
  <section class="order-page">
    <template v-if="order">
      <p class="kicker">ORDER CONFIRMATION</p>
      <h1>{{ order.status === 'PAID' ? '支付完成' : '订单已创建' }}</h1>
      <p class="order-number">订单号 {{ order.id }}</p>
      <div class="order-summary">
        <div v-for="item in order.items" :key="item.productId">
          <span>{{ item.productName }} × {{ item.quantity }}</span>
          <strong>{{ formatMoney(item.unitPriceCents * item.quantity, order.currency) }}</strong>
        </div>
        <div class="order-total">
          <span>合计</span>
          <strong>{{ formatMoney(order.totalCents, order.currency) }}</strong>
        </div>
      </div>
      <p class="status-pill" :class="{ paid: order.status === 'PAID' }">{{ statusLabel }}</p>

      <div v-if="order.status === 'PAID'" class="payment-result">
        <div>
          <span>支付单号</span>
          <strong>{{ order.paymentNo }}</strong>
        </div>
        <div>
          <span>支付时间</span>
          <strong>{{ formatDate(order.paidAt) }}</strong>
        </div>
      </div>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice">{{ error }}</p>
      <template v-if="order.status === 'PENDING_PAYMENT'">
        <p class="notice neutral">
          当前为本地 Mock 支付，不会产生真实扣款。支付金额以订单金额为准。
        </p>
        <button class="primary-button" :disabled="paying" @click="pay">
          {{ paying ? '支付处理中…' : `Mock 支付 ${formatMoney(order.totalCents, order.currency)}` }}
        </button>
      </template>
      <div class="order-actions">
        <RouterLink to="/orders" class="primary-button">查看我的订单</RouterLink>
        <RouterLink to="/" class="secondary-button">继续购物</RouterLink>
      </div>
    </template>
    <div v-else-if="error" class="empty-state">
      <h1>无法读取订单</h1>
      <p>{{ error }}</p>
      <RouterLink to="/" class="primary-button">返回首页</RouterLink>
    </div>
  </section>
</template>
