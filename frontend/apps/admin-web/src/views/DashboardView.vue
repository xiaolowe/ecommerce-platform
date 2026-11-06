<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listProducts, type Product } from '../services/api'
import { formatMoney } from '../utils/money'

const products = ref<Product[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    products.value = (await listProducts()).content
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
        <span>在售商品</span>
        <strong>{{ products.filter((item) => item.status === 'ACTIVE').length }}</strong>
        <small>来自商品服务</small>
      </article>
      <article class="metric-card">
        <span>商品总价均值</span>
        <strong>
          {{
            products.length
              ? formatMoney(products.reduce((sum, item) => sum + item.priceCents, 0) / products.length)
              : '—'
          }}
        </strong>
        <small>按当前页数据计算</small>
      </article>
      <article class="metric-card">
        <span>交易链路</span>
        <strong>PENDING</strong>
        <small>订单等待支付模块接入</small>
      </article>
    </div>

    <div class="panel">
      <div class="panel-heading">
        <h3>当前商品</h3>
        <RouterLink to="/products" class="text-link">管理商品</RouterLink>
      </div>
      <el-empty v-if="!loading && products.length === 0" description="还没有商品" />
      <el-table v-else :data="products.slice(0, 5)">
        <el-table-column prop="sku" label="SKU" width="150" />
        <el-table-column prop="name" label="商品名称" min-width="180" />
        <el-table-column label="价格" width="130">
          <template #default="{ row }">{{ formatMoney(row.priceCents, row.currency) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
