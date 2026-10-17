# 06 · Build, test & release

## Toolchain

| Tool | Status on this machine | Plan |
|---|---|---|
| JDK 17 | ✅ present (JDK 17.0.15; not the default `java`) | Export `JAVA_HOME` to it for every Gradle command, e.g. `JAVA_HOME=<path to JDK 17> ./gradlew …`. Never commit the path or `org.gradle.java.home` |
| Android SDK + `adb` | ✅ installed with the `android` CLI (`~/.local/bin/android`): `platform-tools` 37.0.1, `platforms/android-36`, `platforms/android-37.0` (auto-downloaded by Gradle, required by `compileSdk 37`), `build-tools/36.0.0` in `~/Library/Android/sdk` | `adb` is at `~/Library/Android/sdk/platform-tools/adb` (not on PATH). `sdk.dir` is in the git-ignored `local.properties`. No emulator installed. Test phone: POCO X2 (Android 11, MIUI 12.5) connected over Wi-Fi debugging, see below |
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
./gradlew installDebug                   # install on the connected device/emulator
./gradlew testDebugUnitTest              # JVM + Robolectric tests
./gradlew connectedDebugAndroidTest      # on-device Compose UI tests
```

`ui-tooling` and `ui-tooling-preview` are `debugImplementation` only, so `@Preview`s live in `app/src/debug/`. The **debug** merged manifest therefore also contains the exported, debug-only `androidx.compose.ui.tooling.PreviewActivity`; the release manifest must not. Previews are compile-checked only (no preview renderer in the CLI-only toolchain).

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
| **Compose UI** | `createComposeRule` | Setup screen: Off / enabled-but-not-running / Running states, buttons fire the right callbacks; prefs controls update `Prefs` (M3) |
| **On-device** | Instrumented tests + adb | Service connects and the handle window exists; tap shows the panel (`vol.MiuiVolumeDialogImp-plugin` log); a second tap within 1.8 s does not re-show it; hold + drag snaps to a side at the dragged height (`dumpsys window windows` frame); side/position survive a service restart (DataStore file via `run-as`) |
| **Manual matrix** | Emulators + your phone | See below |

### Driving the phone with adb (verified 2026-10-17, POCO X2 / MIUI 12.5)

With MIUI *USB debugging (Security settings)* on, `adb shell input tap|swipe|motionevent` and `uiautomator dump` work, so the gestures can be scripted **inside EdgeVol, Settings and the home screen only**:
- Tap: `input tap X Y` inside the frame from `dumpsys window windows | grep -A12 EdgeVolHandle`. Hold: `input swipe X Y X Y 1200`. Hold + drag in one shell so timing holds: `adb shell 'input motionevent DOWN X Y; sleep 0.8; input motionevent MOVE X1 Y1; …; input motionevent UP X1 Y1'`.
- Check `dumpsys activity activities | grep mResumedActivity` **before every injection**: a tap lands in whatever app is in front.
- Evidence: `adb logcat -d -v time -s EdgeVol:V vol.MiuiVolumeDialogImp-plugin:V` (`showVolumeDialogH`, `dismissH … reason:1` touch outside / `3` timeout), `dumpsys accessibility` (*Bound*/*Crashed services*), DataStore: `run-as app.edgevol xxd files/datastore/handle.preferences_pb`.
- `am force-stop app.edgevol` clears the service from `enabled_accessibility_services` (screen shows *Off*); `am crash app.edgevol` keeps it enabled and lists it under *Crashed services* (reproduces "cleared from Recents": screen shows *Enabled but not running*).
- `uiautomator dump` prints a harmless MIUI `theme_compatibility.xml` FileNotFoundException trace.

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
| 17 | Optional, TalkBack: focus the handle and double-tap | The panel shows |
| 18 | Optional: setup screen at 200% font size | Nothing clipped |

Never choose EdgeVol in **Developer options → Select debug app**: Android then force-stops it whenever it leaves Recents.

### Device / scenario matrix (M5)

| Scenario | API 29 emu | API 34 emu | API 36 emu | Physical phone |
|---|---|---|---|---|
| Tap handle → system panel over YouTube / Chrome / a game | ☐ | ☐ | ☐ | ☐ (shown over home/EdgeVol/Settings by adb) |
| Second tap within 1.8 s is swallowed (no re-show); tap after the panel timed out shows it again | ☐ | ☐ | ☐ | ☑ adb (MIUI) |
| Does a tap on the handle close the panel on this OEM? (MIUI 12.5: **no**) | ☐ | ☐ | ☐ | ☑ adb (MIUI) |
| Hold 500 ms → handle widens (visual + haptic) | ☐ | ☐ | ☐ | ☐ by hand |
| Hold + drag → snaps to the nearest side at the released height (10–90% clamp); hold + release without moving does nothing | ☐ | ☐ | ☐ | ☑ adb |
| Side + position persist across a service restart / reboot | ☐ | ☐ | ☐ | ☑ restart (adb) · ☐ reboot |
| Gestures with **gesture nav** (no accidental back on either edge, incl. after snapping left) | ☐ | ☐ | ☐ | ☐ by hand |
| Gestures with **3-button nav** | ☐ | ☐ | ☐ | ☐ |
| QS tile → shade collapses (31+) and panel shows (M3) | ☐ | ☐ | ☐ | ☐ |
| Handle visible over the notification shade | ☐ | ☐ | ☐ | ☐ |
| Over an app using `HIDE_OVERLAY_WINDOWS` (e.g. banking) | n/a | ☐ | ☐ | ☐ |
| Expand system panel → ring / notification / alarm reachable | ☐ | ☐ | ☐ | ☐ |
| Bluetooth headphones: panel shows the media stream | — | — | — | ☐ |
| Rotation / split-screen: handle stays on its edge (either side) | ☐ | ☐ | ☐ | ☐ |
| Reboot: handle returns without opening the app, at the saved side/height | ☐ | ☐ | ☐ | ☐ |
| App cleared from Recents (MIUI): setup screen shows *Enabled but not running*; off/on recovers | ☐ | ☐ | ☐ | ☑ crash/force-stop (adb) · ☐ real Recents swipe |
| Screen off/on: handle persists | ☐ | ☐ | ☐ | ☐ |
| TalkBack: double-tap on the handle shows the panel | ☐ | ☐ | ☐ | ☐ |

## Release (personal sideload)

- **Signing:** a keystore kept **outside the repo**. `keystore.properties` (git-ignored) is read by `app/build.gradle.kts`.
- **R8** enabled for release (`isMinifyEnabled = true`, `isShrinkResources = true`). The accessibility and tile services are referenced from the manifest, so R8 keeps them automatically.
- `./gradlew assembleRelease`, then `adb install -r app/build/outputs/apk/release/app-release.apk`.
- Version: `versionCode` increments per build; `versionName` follows semver.

## Definition of done (v1.0)

- [ ] Tap → system panel works from any app on API 29/34/36 emulators and the physical phone; hold-and-drag moves the handle to either side and the position survives restarts
- [ ] All streams reachable through the system panel
- [ ] QS tile shows the panel (and opens setup when the service is off)
- [ ] No `INTERNET` permission in the merged manifest (`./gradlew :app:processReleaseManifest` + inspect)
- [ ] Unit + Compose UI tests green
- [ ] Signed release APK installed and verified after a reboot
