<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAdminPayments, type Payment, type PaymentStatus } from '../services/api'
import { formatMoney } from '../utils/money'

const payments = ref<Payment[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const loading = ref(false)
const filters = reactive({
  keyword: '',
  status: '',
})

const statusOptions: Array<{ value: PaymentStatus; label: string }> = [
  { value: 'REQUIRES_ACTION', label: '待支付' },
  { value: 'SUCCEEDED', label: '支付成功' },
  { value: 'CLOSED', label: '已关闭' },
  { value: 'FAILED', label: '支付失败' },
]

function statusLabel(status: PaymentStatus) {
  return statusOptions.find((item) => item.value === status)?.label ?? status
}

function statusType(status: PaymentStatus) {
  if (status === 'SUCCEEDED') return 'success'
  if (status === 'REQUIRES_ACTION') return 'warning'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

function formatDate(value?: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN') : '—'
}

async function load() {
  loading.value = true
  try {
    const result = await listAdminPayments({
      page: page.value - 1,
      size: pageSize,
      status: filters.status || undefined,
      keyword: filters.keyword.trim() || undefined,
    })
    payments.value = result.content
    total.value = result.page.totalElements
  } catch {
    ElMessage.error('支付记录加载失败')
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
        <p class="eyebrow">PAYMENTS</p>
        <h2>支付记录</h2>
      </div>
      <el-tag effect="plain">共 {{ total }} 笔支付</el-tag>
    </div>

    <div class="panel">
      <div class="filter-row">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="支付单号、订单号、客户 ID"
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

      <el-table v-loading="loading" :data="payments" row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="order-detail">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="支付 ID">{{ row.id }}</el-descriptions-item>
                <el-descriptions-item label="渠道交易号">
                  {{ row.providerTradeNo ?? '—' }}
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDate(row.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="过期时间">{{ formatDate(row.expiresAt) }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="paymentNo" label="支付单号" min-width="240" show-overflow-tooltip />
        <el-table-column prop="orderId" label="订单号" min-width="220" show-overflow-tooltip />
        <el-table-column prop="channel" label="渠道" width="100" />
        <el-table-column label="金额" width="140">
          <template #default="{ row }">{{ formatMoney(row.amountCents, row.currency) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单同步" width="110">
          <template #default="{ row }">
            <el-tag :type="row.orderSynced ? 'success' : 'danger'" effect="plain">
              {{ row.orderSynced ? '已同步' : '待同步' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付时间" width="180">
          <template #default="{ row }">{{ formatDate(row.paidAt) }}</template>
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
