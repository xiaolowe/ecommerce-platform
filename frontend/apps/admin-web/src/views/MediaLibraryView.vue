<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteMediaAsset,
  listMediaAssets,
  uploadImage,
  type MediaAsset,
} from '../services/api'

const assets = ref<MediaAsset[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 24
const loading = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

async function load() {
  loading.value = true
  try {
    const result = await listMediaAssets(page.value - 1, pageSize)
    assets.value = result.content
    total.value = result.page.totalElements
  } finally {
    loading.value = false
  }
}

function chooseFile() {
  fileInput.value?.click()
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 10 MB')
    return
  }

  uploading.value = true
  try {
    await uploadImage(file)
    ElMessage.success('图片上传成功')
    page.value = 1
    await load()
  } catch {
    ElMessage.error('图片上传失败，仅支持 JPG、PNG 和 GIF')
  } finally {
    uploading.value = false
  }
}

async function copyUrl(asset: MediaAsset) {
  await navigator.clipboard.writeText(asset.url)
  ElMessage.success('图片地址已复制')
}

async function remove(asset: MediaAsset) {
  await ElMessageBox.confirm('删除后无法恢复，确认删除这张图片？', '删除图片', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  try {
    await deleteMediaAsset(asset.id)
    ElMessage.success('图片已删除')
    await load()
  } catch {
    ElMessage.error('图片删除失败')
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-title">
      <div>
        <p class="eyebrow">MEDIA LIBRARY</p>
        <h2>图片素材</h2>
      </div>
      <el-button type="primary" :loading="uploading" @click="chooseFile">上传图片</el-button>
      <input
        ref="fileInput"
        class="hidden-file-input"
        type="file"
        accept=".jpg,.jpeg,.png,.gif,image/jpeg,image/png,image/gif"
        @change="onFileSelected"
      />
    </div>

    <div class="panel" v-loading="loading">
      <div class="panel-meta">
        <span>共 {{ total }} 张图片</span>
        <el-button text @click="load">刷新</el-button>
      </div>
      <el-empty v-if="!loading && assets.length === 0" description="还没有上传图片" />
      <div v-else class="media-grid">
        <article v-for="asset in assets" :key="asset.id" class="media-card">
          <div class="media-preview">
            <img :src="asset.url" :alt="asset.originalName" loading="lazy" decoding="async" />
          </div>
          <div class="media-meta">
            <strong>{{ asset.originalName }}</strong>
            <small>{{ asset.width }} × {{ asset.height }} · {{ Math.ceil(asset.sizeBytes / 1024) }} KB</small>
          </div>
          <div class="media-actions">
            <el-button size="small" @click="copyUrl(asset)">复制地址</el-button>
            <el-button size="small" type="danger" plain @click="remove(asset)">删除</el-button>
          </div>
        </article>
      </div>
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
