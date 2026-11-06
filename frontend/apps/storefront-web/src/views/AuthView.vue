<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiErrorMessage } from '../services/http'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const error = ref('')
const title = computed(() => (mode.value === 'login' ? '欢迎回来' : '创建账户'))
const form = reactive({
  email: '',
  password: '',
  displayName: '',
})

async function submit() {
  loading.value = true
  error.value = ''
  try {
    if (mode.value === 'login') {
      await auth.login(form.email, form.password)
    } else {
      await auth.register(form.email, form.password, form.displayName)
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (reason) {
    error.value = apiErrorMessage(reason)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page">
    <div class="auth-story">
      <p class="kicker">NORTHSTAR MEMBERSHIP</p>
      <h1>好商品，<br />值得被认真对待。</h1>
      <p>登录后可以创建订单并查看交易进度。</p>
    </div>
    <form class="auth-card" @submit.prevent="submit">
      <p class="kicker">{{ mode === 'login' ? 'SIGN IN' : 'CREATE ACCOUNT' }}</p>
      <h2>{{ title }}</h2>
      <label v-if="mode === 'register'">
        称呼
        <input v-model="form.displayName" required maxlength="80" autocomplete="name" />
      </label>
      <label>
        邮箱
        <input v-model="form.email" type="email" required autocomplete="email" />
      </label>
      <label>
        密码
        <input
          v-model="form.password"
          type="password"
          required
          minlength="8"
          :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
        />
      </label>
      <p v-if="error" class="notice">{{ error }}</p>
      <button class="primary-button" :disabled="loading" type="submit">
        {{ loading ? '请稍候…' : title }}
      </button>
      <button
        class="text-button switch-mode"
        type="button"
        @click="mode = mode === 'login' ? 'register' : 'login'"
      >
        {{ mode === 'login' ? '还没有账户？立即注册' : '已有账户？返回登录' }}
      </button>
    </form>
  </section>
</template>
