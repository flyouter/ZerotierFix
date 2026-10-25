#!/usr/bin/env bash
#
# 校验 APK 的 16 KB 页对齐：
#   1. APK 内的 .so 必须按 16 KB 对齐（zipalign -c -P 16）
#   2. 每个 .so 的所有 LOAD 段 p_align >= 16384（llvm-readelf）
#
# 用法：tools/check-16kb.sh <apk> [apk...]
#
set -euo pipefail

if [ "$#" -eq 0 ]; then
    echo "usage: $0 <apk> [apk...]" >&2
    exit 2
fi

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"

BUILD_TOOLS=$(find "$ANDROID_HOME/build-tools" -maxdepth 1 -mindepth 1 -type d 2>/dev/null \
    | sort -V | tail -n 1 || true)
ZIPALIGN="${BUILD_TOOLS:-}/zipalign"
if [ ! -x "$ZIPALIGN" ]; then
    echo "zipalign not found under $ANDROID_HOME/build-tools" >&2
    exit 2
fi

READELF=""
if [ -d "$ANDROID_HOME/ndk" ]; then
    NDK=$(find "$ANDROID_HOME/ndk" -maxdepth 1 -mindepth 1 -type d 2>/dev/null \
        | sort -V | tail -n 1 || true)
    CANDIDATE="${NDK:-}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
    if [ -x "$CANDIDATE" ]; then
        READELF="$CANDIDATE"
    fi
fi

status=0

for apk in "$@"; do
    echo "==> $apk"

    if ! "$ZIPALIGN" -c -P 16 -v 4 "$apk" >/tmp/zipalign-16k.log 2>&1; then
        cat /tmp/zipalign-16k.log
        echo "::error::zipalign 16 KB check failed: $apk"
        status=1
        continue
    fi

    if [ -z "$READELF" ]; then
        echo "::warning::llvm-readelf not found; skipping ELF alignment check"
        continue
    fi

    while IFS= read -r so; do
        tmp=$(mktemp)
        unzip -p "$apk" "$so" > "$tmp"
        while IFS= read -r align; do
            if [ $((align)) -lt 16384 ]; then
                echo "::error::Non-16KB-aligned LOAD segment in $so ($apk): $align"
                status=1
            fi
        done < <("$READELF" -lW "$tmp" | awk '/^ *LOAD/{print $NF}')
        rm -f "$tmp"
    done < <(unzip -Z1 "$apk" | grep -E '^lib/.*\.so$' || true)
done

exit "$status"
