<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAdminOrders, type Order, type OrderStatus } from '../services/api'
import { formatMoney } from '../utils/money'

const orders = ref<Order[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const loading = ref(false)
const filters = reactive({
  keyword: '',
  status: '',
})

const statusOptions: Array<{ value: OrderStatus; label: string }> = [
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'PAID', label: '已支付' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'PENDING_STOCK', label: '处理中' },
]

function statusLabel(status: OrderStatus) {
  return statusOptions.find((item) => item.value === status)?.label ?? status
}

function statusType(status: OrderStatus) {
  if (status === 'PAID') return 'success'
  if (status === 'PENDING_PAYMENT' || status === 'PENDING_STOCK') return 'warning'
  if (status === 'CANCELLED') return 'info'
  return undefined
}

function formatDate(value?: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN') : '—'
}

async function load() {
  loading.value = true
  try {
    const result = await listAdminOrders({
      page: page.value - 1,
      size: pageSize,
      status: filters.status || undefined,
      keyword: filters.keyword.trim() || undefined,
    })
    orders.value = result.content
    total.value = result.page.totalElements
  } catch {
    ElMessage.error('订单加载失败')
  } finally {
    loading.value = false
  }
}

async function search() {
  page.value = 1
  await load()
}

async function reset() {
  filters.keyword = ''
  filters.status = ''
  page.value = 1
  await load()
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <p class="eyebrow">ORDERS</p>
        <h2>订单管理</h2>
      </div>
      <el-tag effect="plain">共 {{ total }} 笔订单</el-tag>
    </div>

    <div class="panel">
      <div class="filter-row">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="订单号、客户 ID 或支付单号"
          @keyup.enter="search"
        />
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option
            v-for="option in statusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="orders" row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="order-detail">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="订单 ID">{{ row.id }}</el-descriptions-item>
                <el-descriptions-item label="客户 ID">{{ row.customerId }}</el-descriptions-item>
                <el-descriptions-item label="支付单号">{{ row.paymentNo ?? '—' }}</el-descriptions-item>
                <el-descriptions-item label="支付时间">{{ formatDate(row.paidAt) }}</el-descriptions-item>
                <el-descriptions-item label="支付截止">{{ formatDate(row.paymentExpiresAt) }}</el-descriptions-item>
                <el-descriptions-item label="取消时间">{{ formatDate(row.cancelledAt) }}</el-descriptions-item>
                <el-descriptions-item label="取消原因">
                  {{ row.cancellationReason === 'PAYMENT_TIMEOUT' ? '支付超时' : row.cancellationReason ?? '—' }}
                </el-descriptions-item>
                <el-descriptions-item label="库存释放">
                  {{ row.stockReleased ? '已释放' : '未释放 / 待重试' }}
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDate(row.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ formatDate(row.updatedAt) }}</el-descriptions-item>
              </el-descriptions>
              <el-table :data="row.items" size="small">
                <el-table-column prop="sku" label="SKU" width="150" />
                <el-table-column prop="productName" label="商品" min-width="180" />
                <el-table-column prop="quantity" label="数量" width="80" />
                <el-table-column label="小计" width="140">
                  <template #default="{ row: item }">
                    {{ formatMoney(item.unitPriceCents * item.quantity, row.currency) }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="订单号" min-width="230" show-overflow-tooltip />
        <el-table-column prop="customerId" label="客户 ID" min-width="190" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140">
          <template #default="{ row }">{{ formatMoney(row.totalCents, row.currency) }}</template>
        </el-table-column>
        <el-table-column label="商品" width="90">
          <template #default="{ row }">{{ row.items.length }} 种</template>
        </el-table-column>
        <el-table-column label="下单时间" width="180">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="load"
        />
      </div>
    </div>
  </section>
</template>
