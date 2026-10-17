#!/usr/bin/env bash
# Scripted on-phone checks for EdgeVol (M5). Unlike dev-check.sh this SENDS INPUT:
#   - it opens EdgeVol first and stops if anything else is in front before a touch;
#   - it puts side, height and rotation back the way they were.
#
#   scripts/phone-check.sh            service, tap, quick second tap, hold-drag-snap both ways, rotation, tile
#   scripts/phone-check.sh --restart  also kills the app process and waits for Android to restart the
#                                     service by itself (needs Autostart on Xiaomi phones; up to ~1 min)
#
# Evidence comes from the window list (dumpsys window) and EdgeVol's own log lines.
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=dev-check.sh
. "$ROOT/scripts/dev-check.sh"
ensure_device || exit 1

RESULTS=()
FAILED=0
result() { RESULTS+=("$(printf '%-5s %s' "$1" "$2")"); [ "$1" = FAIL ] && FAILED=1; echo "  -> $1: $2"; }

foreground() { sh dumpsys activity activities | awk '/mResumedActivity/{for(i=1;i<=NF;i++) if($i ~ /\//){print $i; exit}}'; }

# Every touch goes through here: never inject into another app.
require_edgevol() {
  local fg; fg="$(foreground)"
  case "$fg" in
    "$PKG"/*) return 0 ;;
    *) echo "STOP: '$fg' is in front, not EdgeVol. No touch sent." >&2; summary; exit 1 ;;
  esac
}

# "L T R B" of the handle window, empty if there is none.
frame() {
  sh dumpsys window windows | grep -A60 -E "Window #[0-9]+ Window\{[^}]*EdgeVolHandle" \
    | grep -m1 -oE "mFrame=\[-?[0-9]+,-?[0-9]+\]\[-?[0-9]+,-?[0-9]+\]" | tr -c '0-9\n-' ' ' | xargs
}
handle_windows() { sh dumpsys window windows | grep -c -E "Window #[0-9]+ Window\{[^}]*EdgeVolHandle"; }

now() { sh "date '+%m-%d %H:%M:%S.000'"; }
# Waits until an EdgeVol log line since $1 matches $2; prints it.
wait_log() {
  local since="$1" pattern="$2" timeout="${3:-4}" i line
  for ((i = 0; i < timeout * 4; i++)); do
    line="$("$ADB" logcat -d -v time -T "$since" -s EdgeVol:D 2>/dev/null | grep -E "$pattern" | tail -1)"
    [ -n "$line" ] && { echo "$line" | cut -c1-120; return 0; }
    sleep 0.25
  done
  return 1
}

# Taps the handle $1 times (default 1), 0.3 s apart, in one shell so the gap stays short.
# No handle window means no coordinates: stop rather than touch (0,0), which opens the shade.
require_frame() { [ -n "$1" ] || { result FAIL "no handle window: no touch sent"; summary; exit 1; }; }

tap_handle() {
  local f l t r b x y cmd i; f="$(frame)"; require_frame "$f"; read -r l t r b <<<"$f"
  x=$(((l + r) / 2)); y=$(((t + b) / 2)); cmd="input tap $x $y"
  for ((i = 1; i < ${1:-1}; i++)); do cmd="$cmd; sleep 0.3; input tap $x $y"; done
  require_edgevol
  "$ADB" shell "$cmd"
}

# Hold on the handle 0.8 s, then drag to ($1, $2) and release, in one shell so the timing holds.
hold_drag_to() {
  local f l t r b x0 y0; f="$(frame)"; require_frame "$f"; read -r l t r b <<<"$f"
  x0=$(((l + r) / 2)); y0=$(((t + b) / 2))
  require_edgevol
  "$ADB" shell "input motionevent DOWN $x0 $y0; sleep 0.8; input motionevent MOVE $(((x0 + $1) / 2)) $(((y0 + $2) / 2)); input motionevent MOVE $1 $2; input motionevent UP $1 $2"
}

summary() {
  echo
  echo "== summary"
  printf '%s\n' "${RESULTS[@]}"
}

echo "== phone: $(sh getprop ro.product.manufacturer) $(sh getprop ro.product.model), Android $(sh getprop ro.build.version.release) (API $(sh getprop ro.build.version.sdk))"
read -r W H <<<"$(sh wm size | awk -F'[ x]' '/size:/{w=$3; h=$4} END{print w, h}')"
[ "$W" -gt "$H" ] && { t=$W; W=$H; H=$t; }

echo "== 1. service and handle window"
sh am start -n "$PKG/.MainActivity" >/dev/null
for _ in 1 2 3 4 5 6 7 8; do [ "$(foreground | cut -d/ -f1)" = "$PKG" ] && break; sleep 0.5; done
require_edgevol
sleep 1
if sh dumpsys accessibility | grep -q "Service\[label=EdgeVol" && [ "$(handle_windows)" = 1 ]; then
  result PASS "service bound, exactly one handle window ($(frame))"
else
  result FAIL "service bound / one handle window (windows: $(handle_windows))"; summary; exit 1
fi
read -r L0 T0 R0 B0 <<<"$(frame)"
ORIG_SIDE=$([ "$L0" -le 0 ] && echo LEFT || echo RIGHT)
ORIG_CENTRE=$(((T0 + B0) / 2))
ORIG_PCT=$(((ORIG_CENTRE * 100 + H / 2) / H))
echo "  side $ORIG_SIDE, centre ${ORIG_PCT}% of $H px"

echo "== 2. tap shows the panel, a quick second tap does nothing"
since="$(now)"; tap_handle 2
if wait_log "$since" "tap: panel shown"; then result PASS "tap -> panel shown"; else result FAIL "tap -> no 'panel shown' log"; fi
if wait_log "$since" "tap: close \(not re-shown\)"; then result PASS "second tap 0.3 s later swallowed"; else result FAIL "second tap was not swallowed"; fi
sleep 2.5

echo "== 3. hold and drag to the other half snaps to that edge, same height"
if [ "$ORIG_SIDE" = RIGHT ]; then OTHER=LEFT; X_OTHER=$((W / 4)); X_BACK=$((W * 3 / 4)); else OTHER=RIGHT; X_OTHER=$((W * 3 / 4)); X_BACK=$((W / 4)); fi
since="$(now)"; hold_drag_to "$X_OTHER" "$ORIG_CENTRE"
if line="$(wait_log "$since" "snap: side=$OTHER")"; then
  sleep 0.5; read -r l _ r _ <<<"$(frame)"
  if { [ "$OTHER" = LEFT ] && [ "$l" -le 0 ]; } || { [ "$OTHER" = RIGHT ] && [ "$r" -ge "$W" ]; }; then
    result PASS "snapped $OTHER (${line##*: })"
  else
    result FAIL "snap log says $OTHER but the window is at $(frame)"
  fi
else
  result FAIL "no snap to $OTHER"
fi
since="$(now)"; hold_drag_to "$X_BACK" "$ORIG_CENTRE"
if line="$(wait_log "$since" "snap: side=$ORIG_SIDE")"; then
  pct="${line##*pct=}"
  if [ "$pct" -ge $((ORIG_PCT - 1)) ] && [ "$pct" -le $((ORIG_PCT + 1)) ]; then
    result PASS "snapped back $ORIG_SIDE at ${pct}% (was ${ORIG_PCT}%)"
  else
    result FAIL "snapped back at ${pct}%, was ${ORIG_PCT}%: set Height on the setup screen"
  fi
  if wait_log "$since" "prefs saved: side=$ORIG_SIDE pct=$pct" >/dev/null; then result PASS "placement saved"; else result FAIL "snap back not saved"; fi
else
  result FAIL "no snap back to $ORIG_SIDE: move the handle back by hand"
fi

echo "== 4. rotation keeps the relative height"
# Unset settings read as "null"; Android's defaults are auto-rotate on, rotation 0.
ACC=$(sh settings get system accelerometer_rotation); [ "$ACC" = null ] && ACC=1
ROT=$(sh settings get system user_rotation); [ "$ROT" = null ] && ROT=0
restore_rotation() { sh settings put system user_rotation "$ROT"; sh settings put system accelerometer_rotation "$ACC"; }
trap restore_rotation EXIT INT TERM
sh settings put system accelerometer_rotation 0; sh settings put system user_rotation 1; sleep 2.5
read -r _ t _ b <<<"$(frame)"
land_pct=$(((((t + b) / 2) * 100 + W / 2) / W))
restore_rotation; trap - EXIT INT TERM
sleep 2.5
read -r l t r b <<<"$(frame)"
if [ "$land_pct" -ge $((ORIG_PCT - 2)) ] && [ "$land_pct" -le $((ORIG_PCT + 2)) ]; then
  result PASS "landscape centre ${land_pct}% (portrait ${ORIG_PCT}%)"
else
  result FAIL "landscape centre ${land_pct}%, portrait ${ORIG_PCT}%"
fi
if [ "$(((t + b) / 2))" -ge $((ORIG_CENTRE - 3)) ] && [ "$(((t + b) / 2))" -le $((ORIG_CENTRE + 3)) ]; then
  result PASS "back in portrait at the same spot ($l $t $r $b)"
else
  result FAIL "portrait spot changed: $l $t $r $b, was $L0 $T0 $R0 $B0"
fi

echo "== 5. Quick Settings tile hides and shows the handle"
TILE="$PKG/.service.VolumeTileService"
since="$(now)"
out="$(sh cmd statusbar click-tile "$TILE" 2>&1)"
if [ -n "$out" ] || ! wait_log "$since" "tile: handle off" >/dev/null; then
  result SKIP "adb tile clicks don't reach EdgeVol on this phone${out:+ ($out)}: check by hand (manual row 18)"
else
  sleep 0.8; off_windows="$(handle_windows)"
  since="$(now)"; sh cmd statusbar click-tile "$TILE" >/dev/null 2>&1
  wait_log "$since" "tile: handle on" >/dev/null; sleep 0.8
  if [ "$off_windows" = 0 ] && [ "$(handle_windows)" = 1 ]; then
    result PASS "tile off removed the handle, tile on brought it back"
  else
    result FAIL "tile: windows off=$off_windows, on=$(handle_windows)"
  fi
fi

# A late tile click (slow bind) can land after the step gave up: switch the handle back on.
if [ "$(handle_windows)" = 0 ]; then
  sh cmd statusbar click-tile "$TILE" >/dev/null 2>&1; sleep 1.5
  [ "$(handle_windows)" = 0 ] && result FAIL "handle left hidden by the tile step: turn it on in EdgeVol (Show handle)"
fi

if [ "${1:-}" = "--restart" ]; then
  echo "== 6. Android restarts the service after the process is killed"
  # From the home screen, so only the service restart (not EdgeVol's screen) can bring it back.
  sh input keyevent KEYCODE_HOME; sleep 1
  pid="$(sh pidof "$PKG")"
  since="$(now)"; sh am crash "$PKG" >/dev/null 2>&1
  start=$(date +%s)
  if wait_log "$since" "handle added" 90 >/dev/null && [ "$(sh pidof "$PKG")" != "$pid" ]; then
    result PASS "process $pid killed; handle back by itself in a new process after $(($(date +%s) - start)) s"
  else
    result FAIL "handle not back within 90 s (Autostart off? open EdgeVol to restart it)"
  fi
fi

summary
exit $FAILED
