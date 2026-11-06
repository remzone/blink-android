#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"
"$root/tools/build-native.sh"
./gradlew --no-daemon :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease \
  :launcher:testReleaseUnitTest :launcher:lintRelease :launcher:assembleRelease --console=plain
