import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  login as loginRequest,
  register as registerRequest,
  type TokenResponse,
  type User,
} from '../services/api'

const TOKEN_KEY = 'store.accessToken'
const USER_KEY = 'store.user'

function readStoredToken() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return ''

  try {
    const payload = token.split('.')[1]
    const normalized = payload.replaceAll('-', '+').replaceAll('_', '/')
    const decoded = atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '='))
    const expiresAt = Number(JSON.parse(decoded).exp) * 1000
    if (Number.isFinite(expiresAt) && expiresAt > Date.now()) {
      return token
    }
  } catch {
    // Invalid tokens are cleared below.
  }

  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  return ''
}

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
  const token = ref(readStoredToken())
  const user = ref<User | null>(storedUser())
  const isAuthenticated = computed(() => Boolean(token.value))

  function applySession(session: TokenResponse) {
    token.value = session.accessToken
    user.value = session.user
    localStorage.setItem(TOKEN_KEY, session.accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(session.user))
  }

  async function login(email: string, password: string) {
    applySession(await loginRequest(email, password))
  }

  async function register(email: string, password: string, displayName: string) {
    applySession(await registerRequest(email, password, displayName))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isAuthenticated, login, register, logout }
})
