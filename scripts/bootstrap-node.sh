#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tools_dir="${project_root}/.tools"
mkdir -p "${tools_dir}/tmp"
export TMPDIR="${tools_dir}/tmp"
node_release="22.23.4"
node_storage_name="node-v${node_release}"
node_link="${tools_dir}/node"
pnpm_prefix="${tools_dir}/pnpm"
pnpm_version="11.25.0"

die() {
  printf 'bootstrap-node: %s\n' "$*" >&2
  exit 1
}

is_supported_node() {
  local binary="${1:-}"
  [ -n "${binary}" ] || return 1
  [ -x "${binary}" ] || return 1
  local major
  major="$("${binary}" -p 'process.versions.node.split(".")[0]' 2>/dev/null || true)"
  [ -n "${major}" ] && [ "${major}" -ge 22 ]
}

sha256() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    die "需要 shasum 或 sha256sum 校验 Node.js 下载文件"
  fi
}

download_node() {
  command -v curl >/dev/null 2>&1 || die "下载 Node.js 需要 curl"
  command -v tar >/dev/null 2>&1 || die "解压 Node.js 需要 tar"

  local os arch archive checksum primary_url fallback_url work_dir
  local archive_path extracted_home node_bin
  os="$(uname -s)"
  arch="$(uname -m)"

  case "${os}:${arch}" in
    Darwin:x86_64)
      archive="node-v${node_release}-darwin-x64.tar.gz"
      checksum="efcc2b0db6f94bf53eddb038fe2b370277a042bd29e4f19b8f8e23598795b819"
      ;;
    Darwin:arm64)
      archive="node-v${node_release}-darwin-arm64.tar.gz"
      checksum="c9e1dbc748b33ed2d14f42445191ecaf14c56259c7a9a26d0042c49ec2b66428"
      ;;
    Linux:x86_64|Linux:amd64)
      archive="node-v${node_release}-linux-x64.tar.gz"
      checksum="b8492284c76a54baf58d61668dd0e41ee0fc98adabf9503d30f05d08f55b4406"
      ;;
    Linux:aarch64|Linux:arm64)
      archive="node-v${node_release}-linux-arm64.tar.gz"
      checksum="f7a1e486dbc8b286847815b477d4fb5e9fb963ed4489c3e1588853d685f589f9"
      ;;
    *)
      die "暂不支持自动下载 ${os}/${arch} 的 Node.js"
      ;;
  esac

  primary_url="https://mirrors.tuna.tsinghua.edu.cn/nodejs-release/v${node_release}/${archive}"
  fallback_url="https://nodejs.org/dist/v${node_release}/${archive}"
  work_dir="$(mktemp -d "${TMPDIR:-/tmp}/ecommerce-node.XXXXXX")"
  archive_path="${work_dir}/${archive}"

  mkdir -p "${tools_dir}"
  printf '未找到 Node.js 22+，正在下载 Node.js %s...\n' "${node_release}" >&2
  if ! curl --fail --location --retry 2 --retry-delay 2 --connect-timeout 15 \
    --output "${archive_path}" "${primary_url}"; then
    printf '主镜像下载失败，正在尝试 Node.js 官方备用源...\n' >&2
    curl --fail --location --retry 3 --retry-delay 2 --connect-timeout 15 \
      --output "${archive_path}" "${fallback_url}"
  fi

  if [ "$(sha256 "${archive_path}")" != "${checksum}" ]; then
    rm -rf -- "${work_dir}"
    die "Node.js 下载文件校验失败，已停止"
  fi

  mkdir -p "${work_dir}/extract"
  tar -xzf "${archive_path}" -C "${work_dir}/extract"
  node_bin="$(find "${work_dir}/extract" -type f -path '*/bin/node' -print -quit)"
  [ -n "${node_bin}" ] || {
    rm -rf -- "${work_dir}"
    die "Node.js 压缩包中没有找到 node 可执行文件"
  }
  extracted_home="$(cd "$(dirname "${node_bin}")/.." && pwd)"

  rm -rf -- "${tools_dir:?}/${node_storage_name}"
  mv "${extracted_home}" "${tools_dir}/${node_storage_name}"
  ln -sfn "${node_storage_name}" "${node_link}"
  rm -rf -- "${work_dir}"

  is_supported_node "${node_link}/bin/node" || die "下载后的 Node.js 无法运行"
  printf 'Node.js 已安装到 %s\n' "${node_link}" >&2
}

resolve_node_home() {
  if is_supported_node "${node_link}/bin/node"; then
    printf '%s\n' "${node_link}"
    return
  fi
  if command -v node >/dev/null 2>&1; then
    local system_node_home
    system_node_home="$(cd "$(dirname "$(command -v node)")/.." && pwd)"
    if is_supported_node "${system_node_home}/bin/node"; then
      printf '%s\n' "${system_node_home}"
      return
    fi
  fi
  download_node
  printf '%s\n' "${node_link}"
}

node_home="$(resolve_node_home)"
mkdir -p "${tools_dir}"
printf '%s\n' "${node_home}" >"${tools_dir}/node-home"

export PATH="${node_home}/bin:${PATH}"

if [ -x "${pnpm_prefix}/bin/pnpm" ]; then
  pnpm_bin="${pnpm_prefix}/bin/pnpm"
elif command -v pnpm >/dev/null 2>&1; then
  pnpm_bin="$(command -v pnpm)"
else
  [ -x "${node_home}/bin/npm" ] || die "当前 Node.js 没有 npm，无法安装 pnpm"
  printf '未找到 pnpm，正在安装 pnpm %s...\n' "${pnpm_version}" >&2
  npm_config_cache="${tools_dir}/npm-cache" \
    npm install --global --prefix "${pnpm_prefix}" "pnpm@${pnpm_version}"
  pnpm_bin="${pnpm_prefix}/bin/pnpm"
fi

"${pnpm_bin}" --version >/dev/null 2>&1 || die "pnpm 无法运行"
printf '%s\n' "$(dirname "${pnpm_bin}")" >"${tools_dir}/pnpm-home"

printf '使用 Node.js: %s\n' "$("${node_home}/bin/node" --version)" >&2
printf '使用 pnpm: %s\n' "$("${pnpm_bin}" --version)" >&2
