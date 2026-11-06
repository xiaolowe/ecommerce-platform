# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS build
WORKDIR /workspace

RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/admin-web/package.json apps/admin-web/package.json
COPY apps/storefront-web/package.json apps/storefront-web/package.json
RUN pnpm install --frozen-lockfile

COPY apps ./apps
ARG APP
RUN pnpm --filter "./apps/${APP}" build

FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
ARG APP
COPY --from=build "/workspace/apps/${APP}/dist" /usr/share/nginx/html
EXPOSE 8080
