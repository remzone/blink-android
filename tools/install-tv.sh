#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
: "${TV_SERIAL:?Set TV_SERIAL to the authorized adb device serial}"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
cd "$root"
client=app/build/outputs/apk/release/app-release.apk
launcher=launcher/build/outputs/apk/release/launcher-release.apk
signer="$ANDROID_HOME/build-tools/34.0.0/apksigner"
[[ -f "$client" && -f "$launcher" ]] || { echo 'Build both APKs first.' >&2; exit 1; }
[[ "$(adb -s "$TV_SERIAL" get-state)" == device ]]
backup="$root/work/tv-backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup"
chmod 700 "$backup"
old="$(adb -s "$TV_SERIAL" shell pm path com.gathin.tvhome | tr -d '\r' | sed -n 's/^package://p' | head -n1)"
if [[ -n "$old" ]]; then
  adb -s "$TV_SERIAL" pull "$old" "$backup/launcher-before.apk"
  previous="$("$signer" verify --print-certs "$backup/launcher-before.apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
  updated="$("$signer" verify --print-certs "$launcher" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
  [[ -n "$previous" && "$previous" == "$updated" ]] || { echo 'Launcher signing key differs. Build with the existing key.' >&2; exit 1; }
fi
client_cert="$("$signer" verify --print-certs "$client" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
launcher_cert="$("$signer" verify --print-certs "$launcher" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
[[ -n "$client_cert" && "$client_cert" == "$launcher_cert" ]] || { echo 'Client and launcher must have the same signing key.' >&2; exit 1; }
adb -s "$TV_SERIAL" install -r "$client"
adb -s "$TV_SERIAL" install -r "$launcher"
echo 'Installed. Home selection and app data preserved. Open Network settings on the TV.'
