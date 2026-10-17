#!/usr/bin/env bash
# One-command device check for EdgeVol over adb (USB or Wi-Fi debugging).
#
#   scripts/dev-check.sh            status: device, service, handle window, recent logs
#   scripts/dev-check.sh install    build the debug APK, install it, then status
#   scripts/dev-check.sh logs       only the recent filtered logs
#
# Only EdgeVol and volume-panel log lines are read; it takes no screenshots and sends no input.
set -u

PKG=app.edgevol
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

find_adb() {
  if [ -n "${ADB:-}" ] && [ -x "$ADB" ]; then return 0; fi
  for c in "${ANDROID_HOME:-/nonexistent}/platform-tools/adb" "$HOME/Library/Android/sdk/platform-tools/adb" "$(command -v adb 2>/dev/null || true)"; do
    if [ -n "$c" ] && [ -x "$c" ]; then ADB="$c"; return 0; fi
  done
  echo "adb not found. Install platform-tools (see docs/06) or set ADB=/path/to/adb." >&2
  return 1
}

device_ready() { "$ADB" devices | awk 'NR>1 && $2=="device"{f=1} END{exit !f}'; }

ensure_device() {
  device_ready && return 0
  # Wi-Fi debugging: the connect port changes, so ask mDNS for it.
  addr="$("$ADB" mdns services 2>/dev/null | awk '/_adb-tls-connect/{print $NF; exit}')"
  if [ -n "$addr" ]; then "$ADB" connect "$addr" >/dev/null 2>&1; sleep 2; fi
  if device_ready; then return 0; fi
  echo "No device. Wi-Fi: on the phone open Wireless debugging; if pairing was lost run 'adb pair <ip:port>' (docs/06)." >&2
  return 1
}

sh() { "$ADB" shell "$@" | tr -d '\r'; }

status() {
  echo "== device"
  echo "$(sh getprop ro.product.manufacturer) $(sh getprop ro.product.model), Android $(sh getprop ro.build.version.release) (API $(sh getprop ro.build.version.sdk))"
  echo "== foreground"
  sh dumpsys activity activities | awk '/mResumedActivity/{for(i=1;i<=NF;i++) if($i ~ /\//){print $i; exit}}'
  echo "== accessibility service"
  if sh settings get secure enabled_accessibility_services | grep -q "$PKG/"; then echo "listed as enabled in Settings: yes"; else echo "listed as enabled in Settings: NO"; fi
  acc="$(sh dumpsys accessibility)"
  if echo "$acc" | grep -q "Service\[label=EdgeVol"; then echo "bound by Android: yes"; else echo "bound by Android: NO"; fi
  if echo "$acc" | grep "Crashed services" | grep -q "$PKG"; then echo "CRASHED: yes (switch EdgeVol off and on in Accessibility settings)"; else echo "crashed: no"; fi
  echo "== handle window"
  win="$(sh dumpsys window windows)"
  count="$(echo "$win" | grep -c -E "Window #[0-9]+ Window\{[^}]*EdgeVolHandle")"
  echo "EdgeVolHandle windows: $count"
  if [ "$count" -gt 0 ]; then
    echo "$win" | grep -A60 -E "Window #[0-9]+ Window\{[^}]*EdgeVolHandle" | grep -E "^ +(mFrame=|isVisible=|mDrawState=)|mFrame=\[" | head -3 | sed 's/^ */  /'
  fi
  logs
}

logs() {
  echo "== recent logs (EdgeVol + volume panel only)"
  "$ADB" logcat -d -v time 2>/dev/null | grep -E "D/EdgeVol|MiuiVolumeDialogImp-plugin.*(onShowRequested|Final dismiss|dismissH mShowing:true)" | tail -n 15 | cut -c1-170
}

install_debug() {
  if [ -z "${JAVA_HOME:-}" ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    [ -n "$JAVA_HOME" ] || { echo "Set JAVA_HOME to a JDK 17 first." >&2; return 1; }
    export JAVA_HOME
  fi
  ( cd "$ROOT" && ./gradlew --console=plain -q assembleDebug ) || return 1
  "$ADB" install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk" || return 1
  sleep 3
}

find_adb || exit 1
cmd="${1:-status}"
case "$cmd" in
  status)  ensure_device || exit 1; status ;;
  install) ensure_device || exit 1; install_debug || exit 1; status ;;
  logs)    ensure_device || exit 1; logs ;;
  *) echo "usage: $0 [status|install|logs]" >&2; exit 2 ;;
esac
