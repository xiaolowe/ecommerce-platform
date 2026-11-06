import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginRequest, type TokenResponse, type User } from '../services/api'

const TOKEN_KEY = 'admin.accessToken'
const USER_KEY = 'admin.user'

function storedUser(): User | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<User | null>(storedUser())
  const isAuthenticated = computed(() => Boolean(token.value))

  function applySession(session: TokenResponse) {
    token.value = session.accessToken
    user.value = session.user
    localStorage.setItem(TOKEN_KEY, session.accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(session.user))
  }

  async function login(email: string, password: string) {
    const session = await loginRequest(email, password)
    if (session.user.role !== 'ADMIN') {
      throw new Error('当前账号不是管理员')
    }
    applySession(session)
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isAuthenticated, login, logout }
})
