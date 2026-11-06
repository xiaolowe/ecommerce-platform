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
  const token = localStorage.getItem('admin.accessToken')
  if (token && !isPublicRequest(config)) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Trace-Id'] = crypto.randomUUID().replaceAll('-', '')
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin.accessToken')
      localStorage.removeItem('admin.user')
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)
