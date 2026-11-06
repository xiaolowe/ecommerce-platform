<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({
  email: 'admin@example.com',
  password: 'Admin123!ChangeMe',
})

async function submit() {
  loading.value = true
  try {
    await auth.login(form.email, form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-story">
      <p class="eyebrow">COMMERCE CONTROL PLANE</p>
      <h1>让商品、库存与交易保持同一节奏。</h1>
      <p>Vue 3 运营后台，通过 Spring Cloud Gateway 与业务服务连接。</p>
      <div class="story-grid">
        <span>商品上架</span>
        <span>库存预占</span>
        <span>订单闭环</span>
      </div>
    </section>

    <section class="login-card">
      <div class="brand compact">
        <span class="brand-mark">C</span>
        <div>
          <strong>Commerce</strong>
          <small>运营控制台</small>
        </div>
      </div>
      <h2>管理员登录</h2>
      <p class="muted">使用初始化管理员账号进入控制台。</p>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="邮箱">
          <el-input v-model="form.email" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="full-button" @click="submit">
          进入控制台
        </el-button>
      </el-form>
    </section>
  </main>
</template>
