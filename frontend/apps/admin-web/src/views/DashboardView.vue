<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  listAdminOrders,
  listAdminPayments,
  listProducts,
  type Order,
  type OrderStatus,
} from '../services/api'
import { formatMoney } from '../utils/money'

const recentOrders = ref<Order[]>([])
const productTotal = ref(0)
const orderTotal = ref(0)
const paymentTotal = ref(0)
const loading = ref(false)

function statusLabel(status: OrderStatus) {
  const labels: Record<OrderStatus, string> = {
    PENDING_STOCK: '处理中',
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    CANCELLED: '已取消',
  }
  return labels[status]
}

function statusType(status: OrderStatus) {
  if (status === 'PAID') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'warning'
}

onMounted(async () => {
  loading.value = true
  try {
    const [productPage, orderPage, paymentPage] = await Promise.all([
      listProducts(0, 1),
      listAdminOrders({ page: 0, size: 5 }),
      listAdminPayments({ page: 0, size: 1 }),
    ])
    productTotal.value = productPage.page.totalElements
    orderTotal.value = orderPage.page.totalElements
    recentOrders.value = orderPage.content
    paymentTotal.value = paymentPage.page.totalElements
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <p class="eyebrow">OVERVIEW</p>
        <h2>运营概览</h2>
      </div>
      <el-tag type="success" effect="plain">系统运行中</el-tag>
    </div>

    <div class="metric-grid" v-loading="loading">
      <article class="metric-card accent">
        <span>商品总数</span>
        <strong>{{ productTotal }}</strong>
        <small>来自商品服务</small>
      </article>
      <article class="metric-card">
        <span>订单总数</span>
        <strong>{{ orderTotal }}</strong>
        <small>来自订单服务</small>
      </article>
      <article class="metric-card">
        <span>支付记录</span>
        <strong>{{ paymentTotal }}</strong>
        <small>来自支付服务</small>
      </article>
    </div>

    <div class="panel">
      <div class="panel-heading">
        <h3>最近订单</h3>
        <RouterLink to="/orders" class="text-link">管理订单</RouterLink>
      </div>
      <el-empty v-if="!loading && recentOrders.length === 0" description="还没有订单" />
      <el-table v-else :data="recentOrders">
        <el-table-column prop="id" label="订单号" min-width="220" show-overflow-tooltip />
        <el-table-column prop="customerId" label="客户 ID" min-width="180" show-overflow-tooltip />
        <el-table-column label="金额" width="130">
          <template #default="{ row }">{{ formatMoney(row.totalCents, row.currency) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="180">
          <template #default="{ row }">{{ new Date(row.createdAt).toLocaleString('zh-CN') }}</template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
