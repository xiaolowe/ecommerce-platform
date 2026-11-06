<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getOrder, type Order } from '../services/api'
import { apiErrorMessage } from '../services/http'
import { formatMoney } from '../utils/money'

const route = useRoute()
const order = ref<Order | null>(null)
const error = ref('')

onMounted(async () => {
  try {
    order.value = await getOrder(String(route.params.id))
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  }
})
</script>

<template>
  <section class="order-page">
    <template v-if="order">
      <p class="kicker">ORDER CONFIRMATION</p>
      <h1>订单已创建</h1>
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
      <p class="status-pill">{{ order.status }}</p>
      <p class="notice neutral">支付模块接入后，订单会从 PENDING_PAYMENT 进入已支付状态。</p>
      <RouterLink to="/" class="primary-button">继续购物</RouterLink>
    </template>
    <div v-else-if="error" class="empty-state">
      <h1>无法读取订单</h1>
      <p>{{ error }}</p>
      <RouterLink to="/" class="primary-button">返回首页</RouterLink>
    </div>
  </section>
</template>
