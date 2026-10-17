# 02 · Feasibility & constraints

## Verdict: yes, it works across all Android apps

Three facts make a "works everywhere" volume control possible without root:

1. **Volume is global.** `AudioManager.adjustSuggestedStreamVolume()` changes the system-wide level of the active stream from any app, with **no permission**, the same way a hardware key press does.
2. **Android's own volume panel can be summoned by any app.** Passing `FLAG_SHOW_UI` makes SystemUI show its built-in panel. It's the same dialog the hardware keys open, so it works over every app and on every OEM skin.
3. **An AccessibilityService can draw above every app.** A `TYPE_ACCESSIBILITY_OVERLAY` window (API 22+) needs no `SYSTEM_ALERT_WINDOW` and stays above normal apps, full-screen video, games and the notification shade. That's where our edge handle lives.

## Decision 1: where the handle lives

| Criterion | **A. AccessibilityService overlay** ✅ chosen | B. Floating window + foreground service |
|---|---|---|
| Permission the user grants | Accessibility service toggle | "Display over other apps" |
| Draws over normal apps | ✅ | ✅ |
| Draws over the notification shade | ✅ | ❌ |
| Visible in apps that block overlays (`HIDE_OVERLAY_WINDOWS`, Android 12+, e.g. banking) | ✅ (not a normal app overlay) | ❌ hidden |
| Needs a permanent notification | ❌ | ✅ (foreground service) |
| Survives OEM battery savers (Xiaomi, Samsung, Oppo…) | ⚠️ the system keeps it bound normally, **but MIUI does not re-bind it after the app is cleared from Recents** (verified on MIUI 12.5; see [05](05-onboarding-and-permissions.md)) | ⚠️ often killed |
| Restarts after reboot | ✅ automatic | ⚠️ needs a boot receiver + Android 15 foreground-service-from-boot limits |
| Can collapse the shade when the tile is used | ✅ `GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE` (API 31) | ❌ |
| Google Play | ⚠️ needs the Permission Declaration Form, prominent disclosure and review; can't claim `isAccessibilityTool` | ✅ |
| Sideload friction | ⚠️ Android 13+ "restricted settings" step | none |

**Why A:** the app is sideloaded for personal use, so Play's review process doesn't apply, and A is better on every runtime criterion. **Escape hatch:** only the overlay window *type* differs between A and B. If Play distribution is ever needed, B can be added behind a flag.

## Decision 2: which volume UI

| Criterion | **System panel + edge handle** ✅ chosen | Custom EdgeVol panel |
|---|---|---|
| Behaves like the hardware keys | ✅ identical (same API path, same stream choice) | Similar |
| Native look on every OEM | ✅ | ❌ |
| All streams with one tap | ❌ user taps the panel's expand / ⋯ | ✅ |
| Extra permissions | none | DND access for some ring changes |
| Code and bug surface | Small: one overlay window, no Compose in the overlay | Large: panel window, lifecycle owner, state sync, DND handling |

**Why:** the goal is *"my volume keys are broken; give me the keys back, everywhere."* Reusing the real system panel is the most faithful and the least code. A custom panel can return later as an optional style if the OEM panel proves lacking.

## Platform constraints by Android version

| Android (API) | Constraint | Impact | Mitigation |
|---|---|---|---|
| 8.0 (26) | `TYPE_APPLICATION_OVERLAY` replaces old overlay types | None for A | We use `TYPE_ACCESSIBILITY_OVERLAY` |
| 9 (28) | AOSP volume keys default to the **media** stream when nothing is playing | The panel highlights the same stream as the keys would | Intentional: `USE_DEFAULT_STREAM_TYPE` |
| 10 (29) | `Settings.Panel.ACTION_VOLUME` added | Reported as deprecated or unreliable on 14+ | **Not used**; we rely on `FLAG_SHOW_UI` |
| 10 (29) | Gesture navigation: left/right edges are the system *back* gesture (horizontal swipe) | Inward swipes may trigger back | The handle is tap and hold only (no swipe gesture); exclusion rects (view-local, follow the side); configurable side/position |
| 12 (31) | `HIDE_OVERLAY_WINDOWS`; untrusted-touch blocking | Floating windows hidden or blocked | A is unaffected (verify on device in M5) |
| 12 (31) | `GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE` | — | Collapse the shade before showing the panel from the QS tile |
| 13 (33) | **Restricted settings** for sideloaded APKs | Accessibility toggle may be greyed out | Onboarding explains *App info → ⋮ → Allow restricted settings*; `adb install` usually avoids it |
| 13 (33) | `StatusBarManager.requestAddTileService()` | — | One-tap "Add tile" prompt |
| 14 (34) | `TileService.startActivityAndCollapse(Intent)` deprecated | — | Use the `PendingIntent` overload when the tile must open setup |
| 15–16 (35–36) | Tighter foreground-service and background-activity-start rules | Would hurt B | Not used by A; `FLAG_SHOW_UI` starts no activity |

## System panel behavior (inherited from the OEM)

| Aspect | Behavior | Consequence for EdgeVol |
|---|---|---|
| Stream chosen | Call during a call, media while playing, otherwise the device default | Same as the hardware keys. No stream picker needed |
| Expanded view (ring, notification, alarm) | No public API to open it; the user taps expand / ⋯ | Documented in onboarding; one extra tap |
| Look, position, timeout | Owned by SystemUI | Can't restyle or move it, by design |
| First press with nothing playing | Some devices only reveal the panel on the first key press | Not applicable: EdgeVol only shows the panel (`ADJUST_SAME`) |
| Changes that would toggle Do Not Disturb | Android may refuse an app-initiated step with `SecurityException` | Not applicable since M2.1: EdgeVol never changes the volume; the user moves the system slider (SystemUI is allowed) |
| Closing the panel | No API; MIUI hides it after 2.0 s. On MIUI 12.5 it sits **below** our overlay, so touching the handle is not an outside touch and does not close it | The tap toggle only avoids re-requesting it; the panel times out by itself |
| Service not re-bound after the app was cleared from Recents (MIUI) | Android lists it under *Crashed services* while the Accessibility setting still reads "on" | `SetupStatus.serviceRunning` + the *Enabled but not running* setup state; MIUI hints (lock in Recents, Autostart, battery "No restrictions") |

## Risks & mitigations

| # | Risk | Likelihood | Mitigation | Verify in |
|---|---|---|---|---|
| R1 | Handle gesture conflicts with the back gesture | Medium | Tap/hold only, short handle, exclusion rects, configurable side/position | M2 / M5 |
| R2 | OEM ignores or changes accessibility overlay z-order (shade, lock screen) | Low–medium | QS tile as a second trigger; device matrix | M5 |
| R3 | Restricted-settings step confuses the user | Medium | Illustrated onboarding step; adb install | M1 |
| R4 | An OEM SystemUI ignores `FLAG_SHOW_UI` from apps | Low (used by shipping apps on Android 10–17) | Device matrix; fallback: a minimal custom panel behind a flag (only if needed) | M2 / M5 |
| R5 | The 1.8 s tap-toggle window is wrong for a skin (swallowed tap / flicker) | Low | Named constant `PANEL_ASSUMED_VISIBLE_MS`; one extra tap at worst; retune per device | M4 |
| R6 | Accessibility service disabled by the OS after an app update (some OEMs) | Low | Setup screen shows status; QS tile opens setup when the service is off | M3 |

## Out of scope (YAGNI)

Custom volume panel, EQ/booster, per-app volume profiles, themes/skins, Wear OS / TV, Play Store compliance (unless distribution changes), root features.
