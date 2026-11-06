import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: 10_000,
})

function isPublicRequest(config: { url?: string; method?: string }) {
  const url = config.url ?? ''
  const method = (config.method ?? 'get').toLowerCase()
  return url.startsWith('/auth/') || (method === 'get' && url.startsWith('/products'))
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('store.accessToken')
  if (token && !isPublicRequest(config)) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Trace-Id'] = crypto.randomUUID().replaceAll('-', '')
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && !isPublicRequest(error.config ?? {})) {
      localStorage.removeItem('store.accessToken')
      localStorage.removeItem('store.user')
      if (window.location.pathname !== '/auth') {
        window.location.assign('/auth')
      }
    }
    return Promise.reject(error)
  },
)

export function apiErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    return error.response?.data?.detail ?? error.response?.data?.title ?? '请求失败，请稍后重试'
  }
  return error instanceof Error ? error.message : '请求失败，请稍后重试'
}
