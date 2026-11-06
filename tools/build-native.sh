#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to NDK 27.2.12479018}"
mkdir -p "$root/work/bin" "$root/work/gotmp"
export GOBIN="$root/work/bin" TMPDIR="$root/work/gotmp" GOTMPDIR="$root/work/gotmp"
export PATH="$GOBIN:$JAVA_HOME/bin:$PATH"
cd "$root/native"
go install golang.org/x/mobile/cmd/gomobile golang.org/x/mobile/cmd/gobind
# Xray/anet requires this flag on Android with Go >= 1.23 (upstream build instructions).
gomobile bind -target="${BLINK_TARGETS:-android/arm,android/arm64}" -androidapi 26 \
  -ldflags='-s -w -checklinkname=0' -o blinkcore.aar ./mobile
