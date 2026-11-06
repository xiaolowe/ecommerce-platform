# Vue 3 Frontend

两个独立 Vue 3 应用共享同一套网关 API：

- `admin-web`：Vue 3 + TypeScript + Element Plus + Pinia。
- `storefront-web`：Vue 3 + TypeScript + Pinia + 原生组件样式。

```bash
pnpm install
pnpm dev:admin
pnpm dev:storefront
```

开发端口分别为 `5173` 和 `5174`，通过 Vite 将 `/api` 代理到 `http://localhost:8080`。生产镜像由根目录 `compose.yaml` 构建，Nginx 负责 SPA 回退和网关代理。
