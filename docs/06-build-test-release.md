# 06 · Build, test & release

## Toolchain

| Tool | Status on this machine | Plan |
|---|---|---|
| JDK 17 | ✅ present (JDK 17.0.15; not the default `java`) | Export `JAVA_HOME` to it for every Gradle command, e.g. `JAVA_HOME=<path to JDK 17> ./gradlew …`. Never commit the path or `org.gradle.java.home` |
| Android SDK + `adb` | ✅ installed with the `android` CLI (`~/.local/bin/android`): `platform-tools` 37.0.1, `platforms/android-36`, `platforms/android-37.0` (auto-downloaded by Gradle, required by `compileSdk 37`), `build-tools/36.0.0` in `~/Library/Android/sdk` | `adb` is at `~/Library/Android/sdk/platform-tools/adb` (not on PATH). `sdk.dir` is in the git-ignored `local.properties`. No emulator (user decision in M5: Robolectric simulates Android on the JVM instead). Test phone: POCO X2 (Android 11, MIUI 12.5) connected over Wi-Fi debugging, see below |
| Gradle | ✅ wrapper **9.8.1** (`-bin`, sha256 pinned); AGP **9.4.1** with built-in Kotlin; Kotlin **2.4.21**; Compose BOM **2026.09.00** | No global install needed. If the wrapper download times out (GitHub release-assets redirect), retry or pre-seed `~/.gradle/wrapper/dists` |
| Node / `npx` | ✅ present | Only used for the `skills` CLI |

**Install options for the Android SDK** (pick **one**; options 2a and 2b are alternatives, not steps):

1. **Android Studio**: a GUI app that bundles the SDK, emulator and device manager. Download it from developer.android.com/studio, or run `brew install --cask android-studio` (which downloads the same app). It's optional for our agent workflow, which runs from the terminal.
2. **Command line only** (enough to build, install, test and run an emulator):
   - **2a. Google's `android` CLI** (recommended; the repo skill `android-cli` is written for it): one tool that installs SDK packages, creates projects, runs apps and manages emulators.
     ```bash
     curl -fsSL https://dl.google.com/android/cli/latest/darwin_arm64/install.sh | bash
     android sdk install platform-tools platforms/android-36 build-tools/36.0.0
     ```
   - **2b. Homebrew + `sdkmanager`** (fallback if 2a fails; no `curl | bash`):
     ```bash
     brew install --cask android-commandlinetools
     sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
     ```
   - **Emulator (optional with a physical phone):** add `"emulator" "system-images;android-36;google_apis;arm64-v8a"` to the packages (about 10 GB). With USB debugging on your phone, you can skip it.

   Then set `sdk.dir` in `local.properties` (git-ignored).

## Agent skills (repo-scoped)

Installed with `npx skills add … --agent github-copilot --copy` (**no `-g`**). The files live in [`.agents/skills/`](../.agents/skills/), and versions and hashes are pinned in [`skills-lock.json`](../skills-lock.json). Each skill was reviewed before install: all are markdown-only, with no scripts or hooks.

| Skill | Source | Used for |
|---|---|---|
| `android-cli` | [android/skills](https://github.com/android/skills) (official Google) | Project creation, SDK management, emulator, screenshots/UI inspection |
| `android-permissions-security` | android/skills | Permission declarations, least privilege, exported components |
| `edge-to-edge` | android/skills | Setup screen insets on Android 15+ (enforced edge-to-edge) |
| `testing-setup` | android/skills | Unit / Compose UI / screenshot test setup |
| `compose-animations` | [chrisbanes/skills](https://github.com/chrisbanes/skills) | Setup-screen motion API choices (handle preview, state changes) |
| `mobile-android-design` | [wshobson/agents](https://github.com/wshobson/agents) | Material 3 theming and component patterns |

Maintenance:

```bash
npx skills list                 # what's installed in this repo
npx skills update -p            # update project skills only
npx skills experimental_install # restore from skills-lock.json on a fresh clone
```

## Build & run

Prefix each command with `JAVA_HOME=<path to JDK 17>`. **Inner loop** while coding: `./gradlew testDebugUnitTest --tests '<affected classes>'` (warm daemon, no `clean`). **Full check** once before each commit:
`./gradlew testDebugUnitTest lintDebug assembleDebug :app:processDebugManifest :app:processReleaseManifest assembleRelease`.
Lint must report 0 errors; warnings are reported, not blocking.

**Device loop in one command:** `scripts/dev-check.sh` prints the device, foreground app, service state (listed / bound / crashed), handle window frame and recent EdgeVol and volume-panel logs (about 2 s). `scripts/dev-check.sh install` builds, installs with `adb install -r` and prints the same (about 18 s). `scripts/dev-check.sh logs` prints only the logs. It reconnects Wi-Fi adb through mDNS, sends no input, takes no screenshots and reads only EdgeVol and volume-panel log lines.

```bash
./gradlew assembleDebug                  # build
./gradlew installDebug                   # install on the connected phone
./gradlew testDebugUnitTest              # JVM + Robolectric tests (incl. Compose UI), ≈ 25 s warm
scripts/phone-check.sh --restart         # drives the phone, ~35 s; ask the user first
```

**Never run `./gradlew connectedDebugAndroidTest` against your phone:** it uninstalls EdgeVol when it finishes, which switches the accessibility service off and wipes Autostart and the Recents lock.

`ui-tooling` and `ui-tooling-preview` are `debugImplementation` only, so `@Preview`s live in `app/src/debug/`. The **debug** merged manifest therefore also contains the exported, debug-only `androidx.compose.ui.tooling.PreviewActivity`, and (from `ui-test-manifest`, M5) the empty `androidx.activity.ComponentActivity` that Compose UI tests launch; the release manifest must contain neither (checked in M5). Previews are compile-checked only (no preview renderer in the CLI-only toolchain).

## Connecting a phone over Wi-Fi (no USB cable)

Works on Android 11+ when the phone and the Mac are on the same network (verified with a POCO X2 on 2026-10-17). Use the full adb path: `ADB=~/Library/Android/sdk/platform-tools/adb`.

1. Phone: unlock Developer options (tap *Build number* 7 times) → turn on **Wireless debugging** → **Pair device with pairing code**. Note the pairing `IP:port` and 6-digit code. Both are short-lived and local-only.
2. Mac: `$ADB pair <IP:port>` and enter the code.
3. Mac: `$ADB mdns services` shows the connect address (`_adb-tls-connect`), and `$ADB devices -l` should list the phone. If it doesn't, use `$ADB connect <IP:port>` with the port from the main *Wireless debugging* screen (it differs from the pairing port).
4. Install and run: `$ADB install -r app/build/outputs/apk/debug/app-debug.apk`, then `$ADB shell am start -n app.edgevol/.MainActivity`. `./gradlew installDebug` also works once the phone is listed.

**Xiaomi / MIUI phones:**
- Turn on **Install via USB** in Developer options, or `adb install` fails with `INSTALL_FAILED_USER_RESTRICTED`. Watch the phone for an install prompt.
- Without **USB debugging (Security settings)**, adb can't write secure settings or inject touch (`INJECT_EVENTS` / `WRITE_SECURE_SETTINGS` denied). The shortcut below and `adb shell input` then don't work, so enable the accessibility service from the app's *Enable* button and test gestures by hand. With it on, `input tap/swipe/motionevent` work (see "Driving the phone with adb" below); `settings put secure` is still denied. This toggle may require a Mi account and a SIM.

Developer shortcuts (emulator, or a phone that allows secure-settings writes):

```bash
# Enable the accessibility service without tapping through Settings
adb shell settings put secure enabled_accessibility_services app.edgevol/app.edgevol.service.VolumeAccessibilityService
adb shell settings put secure accessibility_enabled 1

# Read stream levels (3 = music, 2 = ring, 5 = notification, 4 = alarm, 0 = call)
adb shell cmd media_session volume --stream 3 --get

# Confirm the handle window exists
adb shell dumpsys window windows | grep -i edgevol
```

## Test strategy

| Layer | Tooling | What's covered |
|---|---|---|
| **Unit** (JVM) | JUnit | `HandleGestures` (tap vs hold vs ignored movement, slop/tap-travel thresholds, reposition deltas, `moved`, cancel) and `HandleSnap` (side by half-screen, 10–90% clamp); `PanelToggle` with a fake clock (swallow window, boundary, reset, `markShown`); `PrefsRepository` against a temp-file DataStore (defaults, round trip, clamping, wrong-typed/unknown/corrupt data); `SetupState` mapping; `SetupStatus` defaults; `SystemVolume.showPanel()` via the injected lambda; resource-text and manifest guards (disclosure == a11y description, no `INTERNET`, no drag-volume leftovers) |
| **Robolectric** (JVM, simulated Android) | Robolectric 4.17, Compose `createComposeRule`, `androidx.test.ext:junit` | `SetupScreenTest` (API 35): Off / enabled-but-not-running / Running, Xiaomi card, restricted-settings card, *Show handle* switch, side and size, both sliders, tile card with and without the Android 13+ prompt, *Try it*. `OverlayControllerTest` (API 26 / 30 / 35): exactly one `TYPE_ACCESSIBILITY_OVERLAY` + `FLAG_NOT_FOCUSABLE` window at the saved place, same window on repeated prefs, hide/show, live settings, rotation keeps the relative height, TalkBack click → `showPanel()`, Move actions (labels, moves, saves, none at the ends). `VolumeTileServiceTest` (API 26 / 30 / 35): renders the stored state (subtitle on 29+), a tap flips and saves it, turning on without the service opens setup (Intent path, 26 / 30) |
| **Phone** | `scripts/phone-check.sh [--restart]` | Service bound + one handle window, tap → panel, second tap swallowed, hold-drag-snap to the other edge and back at the same height + saved, rotation keeps the relative height and the exact portrait spot, tile (skipped on MIUI: adb can't click its tiles), `--restart`: process killed from the home screen → handle back in a new process |
| **Manual** | Your phone | Checklist and matrix below |

Robolectric runs Android up to API 35 on JDK 17 (36+ needs JDK 21). Its first run downloads one Android image per simulated version into `~/.m2` (≈ 0.4 GB for 26, 30 and 35). The new tests were mutation-checked: removing `FLAG_NOT_FOCUSABLE`, the tile's "open setup", or the setup screen's hint condition each makes them fail. They don't cover real system behaviour (the panel itself, OEM skins), which is what the phone checks are for.

### Driving the phone with adb (verified 2026-10-17, POCO X2 / MIUI 12.5)

With MIUI *USB debugging (Security settings)* on, `adb shell input tap|swipe|motionevent` and `uiautomator dump` work, so the gestures can be scripted **inside EdgeVol, Settings and the home screen only**. `scripts/phone-check.sh` packages the checks below; it opens EdgeVol first, checks the foreground before every touch and restores side, height and rotation:
- Tap: `input tap X Y` inside the frame from `dumpsys window windows | grep -A12 EdgeVolHandle`. Hold: `input swipe X Y X Y 1200`. Hold + drag in one shell so timing holds: `adb shell 'input motionevent DOWN X Y; sleep 0.8; input motionevent MOVE X1 Y1; …; input motionevent UP X1 Y1'`.
- Check `dumpsys activity activities | grep mResumedActivity` **before every injection**: a tap lands in whatever app is in front.
- Evidence: `adb logcat -d -v time -s EdgeVol:V vol.MiuiVolumeDialogImp-plugin:V` (`showVolumeDialogH`, `dismissH … reason:1` touch outside / `3` timeout), `dumpsys accessibility` (*Bound*/*Crashed services*), DataStore: `run-as app.edgevol xxd files/datastore/handle.preferences_pb`.
- `am force-stop app.edgevol` clears the service from `enabled_accessibility_services` (screen shows *Off*); `am crash app.edgevol` keeps it enabled and lists it under *Crashed services* (reproduces "cleared from Recents": screen shows *Enabled but not running*).
- `uiautomator dump` prints a harmless MIUI `theme_compatibility.xml` FileNotFoundException trace.
- MIUI: `cmd statusbar click-tile` is accepted but never reaches the tile, so the tile is checked by hand.
- After `am crash`, Android restarts the service after a back-off (1 s for a first crash, 11 s and longer when repeated), only if Autostart is allowed.

### Manual test checklist (M2.1: what only you can verify)

Run on your phone with the service on. Tick or write a few words per row; anything that fails goes to the fast lane. `scripts/dev-check.sh` afterwards shows the service state, the handle frame and the logs.

| # | Do | Expect |
|---|---|---|
| 1 | Look at the right edge, a little above the middle | A thin faint bar |
| 2 | Tap it | Android's volume panel shows, with a click. A quick second tap does nothing extra; the panel hides itself after 2 s (MIUI) |
| 3 | Tap **Show volume panel** in the app | The same panel |
| 4 | Hold about 0.5 s without moving, then release | A buzz, the bar widens to a solid pill, and on release nothing changes |
| 5 | Hold, drag into the **left half**, release | The handle snaps to the left edge at the height you released |
| 6 | Hold, drag to the very top and the very bottom | It stops near the ends (about 10% / 90%) |
| 7 | Hold, drag back into the right half | It snaps back to the right edge |
| 8 | Switch EdgeVol off and on in Accessibility settings | The handle returns at the saved side and height |
| 9 | Reboot, unlock, wait about 30 s | The handle returns by itself at the same spot |
| 10 | Gesture navigation: swipe in from the screen edge right beside the handle, on both sides | No accidental Back |
| 11 | Open other apps, a full-screen video, the notification shade | The handle is visible and works, and touches next to it reach the app |
| 12 | Rotate to landscape and back, with the handle near the top and then near the bottom | It keeps the same relative height (top stays top) and returns to the exact spot in portrait |
| 13 | Turn the screen off and on | The handle still works |
| 14 | Xiaomi only: tap **Open Autostart settings** in the app, switch EdgeVol on, come back | The Autostart list opens; Back returns to EdgeVol |
| 15 | Xiaomi only: open Recents, long-press the EdgeVol card, tap the lock | The card shows a lock |
| 16 | Tap "clear all" in Recents | The handle stays (locked). Unlocked but with Autostart: it comes back within about a minute, or at once when you open EdgeVol |
| 17 | Setup screen → Handle: tap Left/Right, Small/Medium/Large, move Height and Opacity, flip **Show handle** | The real handle moves, resizes and fades live; Show handle off removes it and the text says how to bring it back |
| 18 | Add the "Volume handle" tile (Quick Settings → edit → drag it in); tap it twice; long-press it | Off ↔ On hides and shows the handle; long-press opens EdgeVol, which turns the handle on |
| 19 | Optional, TalkBack: focus the handle, double-tap; then open the actions menu (three-finger tap or swipe up then right) | Double-tap shows the panel; the menu offers *Move to left edge* (or right), *Move up*, *Move down*, and each moves the handle and is kept after a restart |
| 20 | Optional: setup screen at 200% font size | Nothing clipped |
| 21 | Look at the launcher icon; switch the phone to dark mode and open EdgeVol | Indigo gradient icon with three pills and a sliver, not cropped; no white flash on opening in dark mode |

Never choose EdgeVol in **Developer options → Select debug app**: Android then force-stops it whenever it leaves Recents. Avoid `uiautomator dump` while testing the handle: it suspends accessibility services while it runs, so EdgeVol's service is unbound and re-bound (the handle disappears for a moment).

### Device / scenario matrix (M5)

No emulators (user decision): Robolectric covers the app logic on simulated Android 8.0 / 11 / 15, the POCO covers real behaviour on MIUI. ☑ verified · ☐ open (by hand) · — not applicable.

| Scenario | Robolectric (API 26 / 30 / 35) | POCO X2 (MIUI 12.5, API 30) |
|---|---|---|
| Exactly one non-focusable accessibility overlay; hiding removes it | ☑ `OverlayControllerTest` | ☑ `phone-check` |
| Tap → Android's own panel (`ADJUST_SAME` + `FLAG_SHOW_UI`) | ☑ the call | ☑ `phone-check` (over EdgeVol) · ☑ by hand in other apps |
| Second tap within 1.8 s is swallowed | ☑ `PanelToggleTest` (JVM) | ☑ `phone-check` |
| Does a tap on the handle close the panel? (MIUI 12.5: **no**) | — | ☑ adb |
| Hold 500 ms → handle widens (visual + haptic) | — | ☐ by hand |
| Hold + drag → snaps to either edge at the same height, saved | ☑ `HandleSnap` (JVM) | ☑ `phone-check` |
| Setup-screen settings change the handle live | ☑ `SetupScreenTest` + `OverlayControllerTest` | ☑ by hand (M3) |
| Rotation keeps the relative height; back to the same portrait spot | ☑ `OverlayControllerTest` | ☑ `phone-check` |
| Process killed → service and handle come back by themselves (Autostart) | — | ☑ `phone-check --restart` (2 s) |
| "Clear all" in Recents with the card locked | — | ☑ by hand (M2.2) |
| Reboot: handle returns without opening the app, at the saved place | — | ☐ by hand (manual row 9) |
| QS tile switches the handle off and on; long-press opens EdgeVol | ☑ `VolumeTileServiceTest` | ☑ by hand (M3.1) |
| Tile turned on with the service off opens setup | ☑ Intent path (26 / 30) | ☐ |
| … via `PendingIntent` (Android 14+) / Android 13+ *Add tile* prompt | — (not simulated) / ☑ button wiring | — (API 30): unverified |
| TalkBack: double-tap shows the panel; Move actions move and save it | ☑ `OverlayControllerTest` | ☐ by hand (manual row 19) |
| Gesture nav: no accidental Back next to the handle, on both edges | — | ☐ by hand (manual row 10) |
| 3-button navigation | — | ☐ |
| Handle visible over the notification shade | — | ☐ |
| Expand the panel → ring / notification / alarm reachable | — | ☐ |
| Bluetooth headphones: panel shows the media stream | — | ☐ |
| Screen off/on: handle persists | — | ☐ by hand (manual row 13) |
| Over an app using `HIDE_OVERLAY_WINDOWS` (Android 12+) | — | — (API 30) |
| Stock Android and other brands (Pixel, Samsung, …) | API-level code paths only | unverified: needs another phone |

## Release (personal sideload)

**Size:** the release APK is **≈ 1.2 MB** (≈ 1.7 MB of code after R8). The debug APK is ≈ 12.5 MB because it ships every library un-shrunk (≈ 30 MB of uncompressed dex) plus the debug-only Compose preview tooling; it's a development build, not the one to keep on the phone.

- **Signing key (created 2026-10-17):** `~/.android/edgevol/edgevol-release.jks` (PKCS12, RSA 4096, alias `edgevol`, valid 100 years) with its passwords in `~/.android/edgevol/release.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Both are `chmod 600` in a `700` folder **outside the repo** and never committed. `app/build.gradle.kts` reads `keystore.properties` at the repo root if present (git-ignored), otherwise that file. Without either, release builds are unsigned and debug builds use the default debug key.
- **Back the key up** (both files, e.g. in a password manager). Android only installs an update signed with the same key; losing it means uninstalling EdgeVol, which switches the accessibility service off and resets its settings, Autostart and the Recents lock.
- **Debug and release use the same key**, so `scripts/dev-check.sh install` (debug) and `scripts/dev-check.sh release` replace each other with `adb install -r` and the service stays enabled.
- **R8** enabled for release (`isMinifyEnabled = true`, `isShrinkResources = true`). The accessibility and tile services are referenced from the manifest, so R8 keeps them automatically. Verified on the POCO X2: service, handle, tap, hold-drag-snap, saving, live settings, tile, and a restart after a kill all work in the release build; no extra keep rules were needed.
- Build and install: `scripts/dev-check.sh release` (or `./gradlew assembleRelease` + `adb install -r app/build/outputs/apk/release/app-release.apk`).
- Version: `versionCode` increments per build; `versionName` follows semver.

## Definition of done (v1.0)

- [x] Tap → system panel works from any app on the physical phone; hold-and-drag moves the handle to either side and the position survives restarts (emulators replaced by Robolectric for API 26 / 30 / 35, user decision)
- [ ] All streams reachable through the system panel (by hand: expand the panel)
- [x] QS tile shows and hides the handle (and opens setup when the service is off)
- [x] No `INTERNET` permission in the merged manifest (`./gradlew :app:processReleaseManifest` + inspect)
- [x] Unit + Compose UI tests green (223, M5)
- [ ] Signed release APK installed and verified after a reboot (installed and verified; reboot: manual row 9)
