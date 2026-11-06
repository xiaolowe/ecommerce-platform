#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
log_dir="${project_root}/.tools/local-run-logs"
tool_cache_dir="${project_root}/.tools"
mkdir -p "${tool_cache_dir}/tmp"
export TMPDIR="${tool_cache_dir}/tmp"
export npm_config_cache="${tool_cache_dir}/npm-cache"
export XDG_CACHE_HOME="${tool_cache_dir}/cache"
export XDG_DATA_HOME="${tool_cache_dir}/share"
export XDG_STATE_HOME="${tool_cache_dir}/state"
pids=()
reserved_ports=""
allocated_port=""
startup_timeout="${STARTUP_TIMEOUT_SECONDS:-120}"

cleanup() {
  trap - EXIT INT TERM
  printf '\n正在停止本地服务...\n'
  for pid in "${pids[@]:-}"; do
    kill "${pid}" >/dev/null 2>&1 || true
  done
  for pid in "${pids[@]:-}"; do
    wait "${pid}" >/dev/null 2>&1 || true
  done
  printf '本地服务已停止，业务内存数据已清空；图片素材保留在 .data/。\n'
}

trap cleanup EXIT INT TERM

die() {
  printf 'run-local-h2: %s\n' "$*" >&2
  exit 1
}

port_available() {
  local port="$1"
  case " ${reserved_ports} " in
    *" ${port} "*) return 1 ;;
  esac
  ! lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
}

allocate_port() {
  local env_name="$1"
  local preferred="$2"
  local explicit_value="${!env_name:-}"
  local port="${explicit_value:-${preferred}}"
  local limit=$((preferred + 200))

  while ! port_available "${port}"; do
    if [ -n "${explicit_value}" ]; then
      die "${env_name}=${port} 已被占用或与本次启动的其他端口冲突"
    fi
    port=$((port + 1))
    [ "${port}" -le "${limit}" ] || die "找不到 ${preferred} 附近可用的端口"
  done

  reserved_ports="${reserved_ports} ${port}"
  allocated_port="${port}"
}

wait_for_http() {
  local name="$1"
  local url="$2"
  local attempts="${3:-${startup_timeout}}"
  local attempt
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl -fsS --max-time 2 "${url}" >/dev/null 2>&1; then
      printf '  %-18s READY  %s\n' "${name}" "${url}"
      return
    fi
    sleep 1
  done

  printf '\n%s 启动失败，日志如下：\n' "${name}" >&2
  tail -n 100 "${log_dir}/${name}.log" >&2 || true
  die "${name} 未在 ${attempts} 秒内就绪"
}

command -v curl >/dev/null 2>&1 || die "本地运行需要 curl"

printf '[0/3] 准备 Node.js 和 pnpm\n'
"${project_root}/scripts/bootstrap-node.sh"
node_home="$(head -n 1 "${project_root}/.tools/node-home")"
pnpm_home="$(head -n 1 "${project_root}/.tools/pnpm-home")"
export PATH="${node_home}/bin:${pnpm_home}:${PATH}"

printf '[1/3] 准备 Java 工具链并打包后端\n'
"${project_root}/scripts/bootstrap-build.sh" -DskipTests package

if [ ! -f "${project_root}/.tools/java-home" ]; then
  die "没有找到 JDK 路径标记，请重新运行 bootstrap-build.sh"
fi
local_jdk_home="$(head -n 1 "${project_root}/.tools/java-home")"
[ -x "${local_jdk_home}/bin/java" ] || die "JDK 路径无效: ${local_jdk_home}"

runtime_dir="${project_root}/.tools/runtime"
mkdir -p "${runtime_dir}"
cp -f \
  "${project_root}/gateway/target/gateway-0.1.0-SNAPSHOT.jar" \
  "${runtime_dir}/gateway.jar"
cp -f \
  "${project_root}/services/auth-service/target/auth-service-0.1.0-SNAPSHOT.jar" \
  "${runtime_dir}/auth-service.jar"
cp -f \
  "${project_root}/services/catalog-service/target/catalog-service-0.1.0-SNAPSHOT.jar" \
  "${runtime_dir}/catalog-service.jar"
cp -f \
  "${project_root}/services/inventory-service/target/inventory-service-0.1.0-SNAPSHOT.jar" \
  "${runtime_dir}/inventory-service.jar"
cp -f \
  "${project_root}/services/order-service/target/order-service-0.1.0-SNAPSHOT.jar" \
  "${runtime_dir}/order-service.jar"
cp -f \
  "${project_root}/services/media-service/target/media-service-0.1.0-SNAPSHOT.jar" \
  "${runtime_dir}/media-service.jar"

mkdir -p "${log_dir}"
allocate_port GATEWAY_PORT 8080
gateway_port="${allocated_port}"
allocate_port AUTH_PORT 8081
auth_port="${allocated_port}"
allocate_port CATALOG_PORT 8082
catalog_port="${allocated_port}"
allocate_port INVENTORY_PORT 8083
inventory_port="${allocated_port}"
allocate_port ORDER_PORT 8084
order_port="${allocated_port}"
allocate_port MEDIA_PORT 8085
media_port="${allocated_port}"
allocate_port ADMIN_PORT 5173
admin_port="${allocated_port}"
allocate_port STOREFRONT_PORT 5174
storefront_port="${allocated_port}"

printf '[2/3] 启动 Java 服务（H2 内存数据库）\n'
local_jwt_secret="${JWT_SECRET:-local-development-secret-change-before-cloud-1234567890}"

(
  export SERVER_PORT="${auth_port}"
  export BOOTSTRAP_ADMIN_EMAIL="${BOOTSTRAP_ADMIN_EMAIL:-admin@example.com}"
  export BOOTSTRAP_ADMIN_PASSWORD="${BOOTSTRAP_ADMIN_PASSWORD:-Admin123!ChangeMe}"
  export JWT_SECRET="${local_jwt_secret}"
  exec "${local_jdk_home}/bin/java" -jar "${runtime_dir}/auth-service.jar"
) >"${log_dir}/auth-service.log" 2>&1 &
pids+=("$!")

(
  export SERVER_PORT="${catalog_port}"
  export JWT_SECRET="${local_jwt_secret}"
  exec "${local_jdk_home}/bin/java" -jar "${runtime_dir}/catalog-service.jar"
) >"${log_dir}/catalog-service.log" 2>&1 &
pids+=("$!")

(
  export SERVER_PORT="${inventory_port}"
  export JWT_SECRET="${local_jwt_secret}"
  exec "${local_jdk_home}/bin/java" -jar "${runtime_dir}/inventory-service.jar"
) >"${log_dir}/inventory-service.log" 2>&1 &
pids+=("$!")

(
  export SERVER_PORT="${order_port}"
  export JWT_SECRET="${local_jwt_secret}"
  export CATALOG_SERVICE_URL="http://localhost:${catalog_port}"
  export INVENTORY_SERVICE_URL="http://localhost:${inventory_port}"
  exec "${local_jdk_home}/bin/java" -jar "${runtime_dir}/order-service.jar"
) >"${log_dir}/order-service.log" 2>&1 &
pids+=("$!")

(
  export SERVER_PORT="${media_port}"
  export JWT_SECRET="${local_jwt_secret}"
  export DB_URL="jdbc:h2:file:${project_root}/.data/media-db;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
  export MEDIA_LOCAL_ROOT="${project_root}/.data/media"
  exec "${local_jdk_home}/bin/java" -jar "${runtime_dir}/media-service.jar"
) >"${log_dir}/media-service.log" 2>&1 &
pids+=("$!")

wait_for_http "auth-service" "http://localhost:${auth_port}/actuator/health/readiness"
wait_for_http "catalog-service" "http://localhost:${catalog_port}/actuator/health/readiness"
wait_for_http "inventory-service" "http://localhost:${inventory_port}/actuator/health/readiness"
wait_for_http "order-service" "http://localhost:${order_port}/actuator/health/readiness"
wait_for_http "media-service" "http://localhost:${media_port}/actuator/health/readiness"

(
  export SERVER_PORT="${gateway_port}"
  export JWT_SECRET="${local_jwt_secret}"
  export AUTH_SERVICE_URL="http://localhost:${auth_port}"
  export CATALOG_SERVICE_URL="http://localhost:${catalog_port}"
  export INVENTORY_SERVICE_URL="http://localhost:${inventory_port}"
  export ORDER_SERVICE_URL="http://localhost:${order_port}"
  export MEDIA_SERVICE_URL="http://localhost:${media_port}"
  export CORS_ALLOWED_ORIGINS="http://localhost:${admin_port},http://127.0.0.1:${admin_port},http://localhost:${storefront_port},http://127.0.0.1:${storefront_port}"
  exec "${local_jdk_home}/bin/java" -jar "${runtime_dir}/gateway.jar"
) >"${log_dir}/gateway.log" 2>&1 &
pids+=("$!")

wait_for_http "gateway" "http://localhost:${gateway_port}/actuator/health/readiness"

printf '[3/3] 启动 Vue 3 开发服务器\n'
cd "${project_root}/frontend"
pnpm install --frozen-lockfile --store-dir "${tool_cache_dir}/pnpm-store" \
  >"${log_dir}/pnpm-install.log" 2>&1

(
  export VITE_API_PROXY_TARGET="http://localhost:${gateway_port}"
  exec pnpm --filter @ecommerce/admin-web exec vite --host 127.0.0.1 --port "${admin_port}"
) >"${log_dir}/admin-web.log" 2>&1 &
pids+=("$!")

(
  export VITE_API_PROXY_TARGET="http://localhost:${gateway_port}"
  exec pnpm --filter @ecommerce/storefront-web exec vite --host 127.0.0.1 --port "${storefront_port}"
) >"${log_dir}/storefront-web.log" 2>&1 &
pids+=("$!")

wait_for_http "admin-web" "http://127.0.0.1:${admin_port}"
wait_for_http "storefront-web" "http://127.0.0.1:${storefront_port}"

cat <<EOF

本地环境已启动：
  运营后台    http://localhost:${admin_port}
  用户商城    http://localhost:${storefront_port}
  API 网关    http://localhost:${gateway_port}

管理员账号：
  ${BOOTSTRAP_ADMIN_EMAIL:-admin@example.com}
  ${BOOTSTRAP_ADMIN_PASSWORD:-Admin123!ChangeMe}

数据库：H2 内存模式，无需 MySQL
图片目录：${project_root}/.data/media
日志目录：${log_dir}

按 Ctrl+C 停止全部服务。
EOF

while true; do
  for pid in "${pids[@]}"; do
    if ! kill -0 "${pid}" >/dev/null 2>&1; then
      die "本地服务进程意外退出（PID ${pid}），请查看 ${log_dir}"
    fi
  done
  sleep 5
done
