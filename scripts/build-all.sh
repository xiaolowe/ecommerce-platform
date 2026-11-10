#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tool_cache_dir="${project_root}/.tools"
mkdir -p "${tool_cache_dir}/tmp"
export TMPDIR="${tool_cache_dir}/tmp"
export npm_config_cache="${tool_cache_dir}/npm-cache"
export XDG_CACHE_HOME="${tool_cache_dir}/cache"
export XDG_DATA_HOME="${tool_cache_dir}/share"
export XDG_STATE_HOME="${tool_cache_dir}/state"

printf '\n[0/2] 准备 Node.js 和 pnpm\n'
"${project_root}/scripts/bootstrap-node.sh"
node_home="$(head -n 1 "${project_root}/.tools/node-home")"
pnpm_home="$(head -n 1 "${project_root}/.tools/pnpm-home")"
export PATH="${node_home}/bin:${pnpm_home}:${PATH}"

printf '\n[1/2] 构建并验证 Java 后端\n'
"${project_root}/scripts/bootstrap-build.sh" verify

printf '\n[2/2] 构建并验证 Vue 3 前端\n'
cd "${project_root}/frontend"
pnpm install --frozen-lockfile
pnpm typecheck
pnpm test
pnpm build

printf '\n完整构建通过。\n'
