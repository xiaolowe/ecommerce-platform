#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tools_dir="${project_root}/.tools"
mkdir -p "${tools_dir}/tmp"
export TMPDIR="${tools_dir}/tmp"
jdk_link="${tools_dir}/jdk-21"
jdk_release="21.0.13+9"
jdk_storage_name="temurin-21.0.13-9"

die() {
  printf 'bootstrap-build: %s\n' "$*" >&2
  exit 1
}

is_java_21() {
  local candidate="${1:-}"
  [ -n "${candidate}" ] || return 1
  [ -x "${candidate}/bin/java" ] || return 1
  "${candidate}/bin/java" -version 2>&1 | head -n 1 | grep -Eq 'version "21([."]|$)'
}

sha256() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    die "需要 shasum 或 sha256sum 校验 JDK 下载文件"
  fi
}

resolve_jdk() {
  if is_java_21 "${jdk_link}"; then
    printf '%s\n' "${jdk_link}"
    return
  fi

  if is_java_21 "${JAVA_HOME:-}"; then
    printf '%s\n' "${JAVA_HOME}"
    return
  fi

  if command -v java >/dev/null 2>&1; then
    local detected_java_home
    detected_java_home="$(cd "$(dirname "$(command -v java)")/.." && pwd)"
    if is_java_21 "${detected_java_home}"; then
      printf '%s\n' "${detected_java_home}"
      return
    fi
  fi

  if [ "$(uname -s)" = "Darwin" ] && [ -x /usr/libexec/java_home ]; then
    local mac_jdk_home
    mac_jdk_home="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if is_java_21 "${mac_jdk_home}"; then
      printf '%s\n' "${mac_jdk_home}"
      return
    fi
  fi

  download_jdk
}

download_jdk() {
  command -v curl >/dev/null 2>&1 || die "下载 JDK 需要 curl"
  command -v tar >/dev/null 2>&1 || die "解压 JDK 需要 tar"

  local os arch mirror_arch archive checksum primary_url fallback_url
  local work_dir archive_path extracted_home
  os="$(uname -s)"
  arch="$(uname -m)"

  case "${os}:${arch}" in
    Darwin:x86_64)
      mirror_arch="x64"
      archive="OpenJDK21U-jdk_x64_mac_hotspot_21.0.13_9.tar.gz"
      checksum="9aa1d1c17ae5467a022679e7e0af5c00797d94ac462ed1a46f4db07a8a34a6dd"
      ;;
    Darwin:arm64)
      mirror_arch="aarch64"
      archive="OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.13_9.tar.gz"
      checksum="bf036b35b071bfb2f54f2c0af8ee8d88afa95ee6eb3dce02c0f94cfc2524d04a"
      ;;
    Linux:x86_64|Linux:amd64)
      mirror_arch="x64"
      archive="OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz"
      checksum="d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c"
      ;;
    Linux:aarch64|Linux:arm64)
      mirror_arch="aarch64"
      archive="OpenJDK21U-jdk_aarch64_linux_hotspot_21.0.13_9.tar.gz"
      checksum="e3522e707fc7dbe2a72c30746b1e7068301a0213ecd213d4491f35a14eb55578"
      ;;
    *)
      die "暂不支持自动下载 ${os}/${arch} 的 JDK；请安装 JDK 21 后重新运行"
      ;;
  esac

  primary_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/${mirror_arch}/mac/${archive}"
  if [ "${os}" = "Linux" ]; then
    primary_url="https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/${mirror_arch}/linux/${archive}"
  fi
  fallback_url="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/${archive}"
  work_dir="$(mktemp -d "${TMPDIR:-/tmp}/ecommerce-jdk.XXXXXX")"
  archive_path="${work_dir}/${archive}"

  mkdir -p "${tools_dir}"
  printf '未找到 JDK 21，正在下载 Eclipse Temurin %s（首次构建约 190 MB）...\n' "${jdk_release}" >&2
  if ! curl --fail --location --retry 2 --retry-delay 2 --connect-timeout 15 \
    --output "${archive_path}" "${primary_url}"; then
    printf '主镜像下载失败，正在尝试 Adoptium GitHub 备用源...\n' >&2
    curl --fail --location --retry 3 --retry-delay 2 --connect-timeout 15 \
      --output "${archive_path}" "${fallback_url}"
  fi

  local actual_checksum
  actual_checksum="$(sha256 "${archive_path}")"
  if [ "${actual_checksum}" != "${checksum}" ]; then
    rm -rf -- "${work_dir}"
    die "JDK 下载文件校验失败，已停止构建"
  fi

  mkdir -p "${work_dir}/extract"
  tar -xzf "${archive_path}" -C "${work_dir}/extract"
  local java_bin
  java_bin="$(find "${work_dir}/extract" -type f -path '*/bin/java' -print -quit)"
  [ -n "${java_bin}" ] || {
    rm -rf -- "${work_dir}"
    die "JDK 压缩包中没有找到 java 可执行文件"
  }
  extracted_home="$(cd "$(dirname "${java_bin}")/.." && pwd)"

  rm -rf -- "${tools_dir:?}/${jdk_storage_name}"
  mv "${extracted_home}" "${tools_dir}/${jdk_storage_name}"
  ln -sfn "${jdk_storage_name}" "${jdk_link}"
  rm -rf -- "${work_dir}"

  is_java_21 "${jdk_link}" || die "下载后的 JDK 无法运行"
  printf 'JDK 已安装到 %s\n' "${jdk_link}" >&2
  printf '%s\n' "${jdk_link}"
}

jdk_home="$(resolve_jdk)"
mkdir -p "${tools_dir}"
printf '%s\n' "${jdk_home}" > "${tools_dir}/java-home"
export JAVA_HOME="${jdk_home}"
export PATH="${JAVA_HOME}/bin:${PATH}"
export MAVEN_USER_HOME="${MAVEN_USER_HOME:-${tools_dir}/maven-wrapper}"
maven_repo="${MAVEN_REPO_LOCAL:-${tools_dir}/m2}"

if [ "$#" -eq 0 ]; then
  set -- verify
fi

printf '使用 JDK: %s\n' "$("${JAVA_HOME}/bin/java" -version 2>&1 | head -n 1)"
printf '使用 Maven Wrapper: %s/.mvn/wrapper/maven-wrapper.properties\n' "${project_root}"

cd "${project_root}"
exec ./mvnw -B -ntp -Dmaven.repo.local="${maven_repo}" "$@"
