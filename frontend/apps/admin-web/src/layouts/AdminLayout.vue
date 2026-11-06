<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>

<template>
  <el-container class="admin-shell">
    <el-aside width="228px" class="admin-sidebar">
      <div class="brand">
        <span class="brand-mark">C</span>
        <div>
          <strong>Commerce</strong>
          <small>运营控制台</small>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="nav-menu">
        <el-menu-item index="/">
          <span>运营概览</span>
        </el-menu-item>
        <el-menu-item index="/products">
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/inventory">
          <span>库存管理</span>
        </el-menu-item>
        <el-menu-item index="/media">
          <span>图片素材</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div>
          <p class="eyebrow">ECOMMERCE OPERATIONS</p>
          <h1>商品与库存中枢</h1>
        </div>
        <el-dropdown>
          <span class="user-chip">{{ auth.user?.displayName ?? '管理员' }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="admin-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>
