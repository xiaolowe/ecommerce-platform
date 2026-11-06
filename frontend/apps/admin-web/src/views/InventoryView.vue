<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createStock,
  getStock,
  listProducts,
  type Product,
  type Stock,
} from '../services/api'

const products = ref<Product[]>([])
const stock = ref<Stock | null>(null)
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  productId: '',
  availableStock: 100,
})
const selected = computed(() => products.value.find((item) => item.id === form.productId))

async function loadProducts() {
  products.value = (await listProducts()).content
}

async function queryStock() {
  if (!form.productId) return
  loading.value = true
  try {
    stock.value = await getStock(form.productId)
  } catch {
    stock.value = null
  } finally {
    loading.value = false
  }
}

async function initializeStock() {
  if (!form.productId) {
    ElMessage.warning('请先选择商品')
    return
  }
  saving.value = true
  try {
    stock.value = await createStock(form.productId, form.availableStock)
    ElMessage.success('库存已初始化')
  } catch {
    ElMessage.error('库存初始化失败，可能已经存在库存记录')
    await queryStock()
  } finally {
    saving.value = false
  }
}

onMounted(loadProducts)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <p class="eyebrow">INVENTORY</p>
        <h2>库存管理</h2>
      </div>
    </div>

    <div class="split-grid">
      <div class="panel">
        <h3>选择商品</h3>
        <el-form label-position="top">
          <el-form-item label="商品">
            <el-select v-model="form.productId" filterable placeholder="选择商品" @change="queryStock">
              <el-option
                v-for="product in products"
                :key="product.id"
                :label="`${product.name} · ${product.sku}`"
                :value="product.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="初始可售库存">
            <el-input-number v-model="form.availableStock" :min="0" />
          </el-form-item>
          <div class="button-row">
            <el-button :loading="saving" type="primary" @click="initializeStock">
              初始化库存
            </el-button>
            <el-button :disabled="!form.productId" :loading="loading" @click="queryStock">
              刷新库存
            </el-button>
          </div>
        </el-form>
      </div>

      <div class="panel">
        <h3>库存状态</h3>
        <p class="muted">{{ selected ? `${selected.name} · ${selected.sku}` : '尚未选择商品' }}</p>
        <el-skeleton v-if="loading" :rows="3" animated />
        <el-descriptions v-else-if="stock" :column="1" border>
          <el-descriptions-item label="可售库存">{{ stock.availableStock }}</el-descriptions-item>
          <el-descriptions-item label="预占库存">{{ stock.reservedStock }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ new Date(stock.updatedAt).toLocaleString('zh-CN') }}
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="该商品还没有库存记录" />
      </div>
    </div>
  </section>
</template>
