<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createProduct,
  listMediaAssets,
  listProducts,
  updateProductImage,
  uploadImage,
  type MediaAsset,
  type Product,
} from '../services/api'
import { formatMoney } from '../utils/money'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const products = ref<Product[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const uploadingImage = ref(false)
const imageFileInput = ref<HTMLInputElement | null>(null)
const imageDialogVisible = ref(false)
const imageProduct = ref<Product | null>(null)
const mediaAssets = ref<MediaAsset[]>([])
const mediaLoading = ref(false)
const mediaUploading = ref(false)
const mediaFileInput = ref<HTMLInputElement | null>(null)
const form = reactive({
  sku: '',
  name: '',
  description: '',
  price: 0,
  currency: 'CNY',
  imageUrl: '',
})

async function load() {
  loading.value = true
  try {
    const result = await listProducts(page.value - 1, pageSize)
    products.value = result.content
    total.value = result.page.totalElements
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    sku: '',
    name: '',
    description: '',
    price: 0,
    currency: 'CNY',
    imageUrl: '',
  })
  dialogVisible.value = true
}

function chooseCreateImage() {
  imageFileInput.value?.click()
}

async function uploadCreateImage(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 10 MB')
    return
  }
  uploadingImage.value = true
  try {
    const asset = await uploadImage(file)
    form.imageUrl = asset.url
  } catch {
    ElMessage.error('图片上传失败，仅支持 JPG、PNG 和 GIF')
  } finally {
    uploadingImage.value = false
  }
}

async function save() {
  if (!form.sku.trim() || !form.name.trim() || form.price <= 0) {
    ElMessage.warning('请填写 SKU、商品名称和有效价格')
    return
  }
  saving.value = true
  try {
    await createProduct({
      sku: form.sku.trim(),
      name: form.name.trim(),
      description: form.description.trim(),
      priceCents: Math.round(form.price * 100),
      currency: form.currency,
      imageUrl: form.imageUrl || undefined,
    })
    ElMessage.success('商品已创建')
    dialogVisible.value = false
    page.value = 1
    await load()
  } catch {
    ElMessage.error('商品创建失败')
  } finally {
    saving.value = false
  }
}

async function openImageDialog(row: unknown) {
  imageProduct.value = row as Product
  imageDialogVisible.value = true
  mediaLoading.value = true
  try {
    mediaAssets.value = (await listMediaAssets(0, 100)).content
  } finally {
    mediaLoading.value = false
  }
}

function chooseMediaFile() {
  mediaFileInput.value?.click()
}

async function uploadForProduct(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  mediaUploading.value = true
  try {
    const asset = await uploadImage(file)
    mediaAssets.value = [asset, ...mediaAssets.value]
    await setProductImage(asset.url)
  } catch {
    ElMessage.error('图片上传失败')
  } finally {
    mediaUploading.value = false
  }
}

async function setProductImage(imageUrl: string | null) {
  if (!imageProduct.value) return
  await updateProductImage(imageProduct.value.id, imageUrl)
  ElMessage.success('商品主图已更新')
  imageDialogVisible.value = false
  await load()
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <p class="eyebrow">CATALOG</p>
        <h2>商品管理</h2>
      </div>
      <el-button type="primary" @click="openCreate">新增商品</el-button>
    </div>

    <div class="panel">
      <div class="panel-meta">
        <span>共 {{ total }} 个商品</span>
        <el-button text @click="load">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="products">
        <el-table-column label="主图" width="86">
          <template #default="{ row }">
            <div class="table-thumb">
              <img v-if="row.imageUrl" :src="row.imageUrl" :alt="row.name" />
              <span v-else>无</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sku" label="SKU" width="160" />
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="价格" width="140">
          <template #default="{ row }">{{ formatMoney(row.priceCents, row.currency) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openImageDialog(row)">设置主图</el-button>
          </template>
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

    <el-dialog v-model="dialogVisible" title="新增商品" width="560px">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="SKU">
            <el-input v-model="form.sku" placeholder="例如 PHONE-001" />
          </el-form-item>
          <el-form-item label="币种">
            <el-select v-model="form.currency">
              <el-option label="人民币" value="CNY" />
              <el-option label="美元" value="USD" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="商品名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="销售价格">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" />
          <span class="unit">{{ form.currency }}</span>
        </el-form-item>
        <el-form-item label="商品主图">
          <div class="image-picker">
            <img v-if="form.imageUrl" :src="form.imageUrl" alt="商品主图预览" />
            <div v-else class="image-placeholder">尚未上传图片</div>
            <el-button :loading="uploadingImage" @click="chooseCreateImage">上传图片</el-button>
            <el-button v-if="form.imageUrl" link type="danger" @click="form.imageUrl = ''">移除</el-button>
          </div>
          <input
            ref="imageFileInput"
            class="hidden-file-input"
            type="file"
            accept=".jpg,.jpeg,.png,.gif,image/jpeg,image/png,image/gif"
            @change="uploadCreateImage"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">创建商品</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="imageDialogVisible" title="选择商品主图" width="760px">
      <div class="panel-meta">
        <span>{{ imageProduct?.name }}</span>
        <el-button :loading="mediaUploading" @click="chooseMediaFile">上传新图片</el-button>
      </div>
      <input
        ref="mediaFileInput"
        class="hidden-file-input"
        type="file"
        accept=".jpg,.jpeg,.png,.gif,image/jpeg,image/png,image/gif"
        @change="uploadForProduct"
      />
      <div v-loading="mediaLoading" class="media-picker-grid">
        <button
          v-for="asset in mediaAssets"
          :key="asset.id"
          type="button"
          class="media-pick"
          @click="setProductImage(asset.url)"
        >
          <img :src="asset.url" :alt="asset.originalName" loading="lazy" />
          <span>{{ asset.originalName }}</span>
        </button>
      </div>
      <el-empty v-if="!mediaLoading && mediaAssets.length === 0" description="素材库暂无图片" />
      <template #footer>
        <el-button v-if="imageProduct?.imageUrl" type="danger" plain @click="setProductImage(null)">
          移除主图
        </el-button>
        <el-button @click="imageDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </section>
</template>
