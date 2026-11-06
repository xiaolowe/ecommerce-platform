<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useCartStore } from './stores/cart'

const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()

function logout() {
  auth.logout()
  cart.clear()
  router.push('/')
}
</script>

<template>
  <div class="site-shell">
    <header class="site-header">
      <RouterLink to="/" class="store-brand">
        <span>N</span>
        Northstar
      </RouterLink>
      <nav>
        <RouterLink to="/">精选商品</RouterLink>
        <RouterLink to="/cart">购物袋 <b v-if="cart.count">{{ cart.count }}</b></RouterLink>
      </nav>
      <div class="account-actions">
        <span v-if="auth.user">你好，{{ auth.user.displayName }}</span>
        <button v-if="auth.user" class="text-button" @click="logout">退出</button>
        <RouterLink v-else to="/auth" class="login-link">登录</RouterLink>
      </div>
    </header>

    <main class="page-content">
      <RouterView />
    </main>

    <footer class="site-footer">
      <span>Northstar Market</span>
      <span>Vue 3 · Spring Cloud</span>
    </footer>
  </div>
</template>
