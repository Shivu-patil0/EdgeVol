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

Prefix each command with `JAVA_HOME=<path to JDK 17>`. The full M1 check is:
`./gradlew testDebugUnitTest lintDebug assembleDebug :app:processDebugManifest :app:processReleaseManifest`
(plus `./gradlew assembleRelease` for R8). Lint must report 0 errors; warnings are reported, not blocking.

```bash
./gradlew assembleDebug                  # build
./gradlew installDebug                   # install on the connected device/emulator
./gradlew testDebugUnitTest              # JVM + Robolectric tests
./gradlew connectedDebugAndroidTest      # on-device Compose UI tests
```

## Connecting a phone over Wi-Fi (no USB cable)

Works on Android 11+ when the phone and the Mac are on the same network (verified with a POCO X2 on 2026-10-17). Use the full adb path: `ADB=~/Library/Android/sdk/platform-tools/adb`.

1. Phone: unlock Developer options (tap *Build number* 7 times) → turn on **Wireless debugging** → **Pair device with pairing code**. Note the pairing `IP:port` and 6-digit code. Both are short-lived and local-only.
2. Mac: `$ADB pair <IP:port>` and enter the code.
3. Mac: `$ADB mdns services` shows the connect address (`_adb-tls-connect`), and `$ADB devices -l` should list the phone. If it doesn't, use `$ADB connect <IP:port>` with the port from the main *Wireless debugging* screen (it differs from the pairing port).
4. Install and run: `$ADB install -r app/build/outputs/apk/debug/app-debug.apk`, then `$ADB shell am start -n app.edgevol/.MainActivity`. `./gradlew installDebug` also works once the phone is listed.

**Xiaomi / MIUI phones:**
- Turn on **Install via USB** in Developer options, or `adb install` fails with `INSTALL_FAILED_USER_RESTRICTED`. Watch the phone for an install prompt.
- Without **USB debugging (Security settings)**, adb can't write secure settings or inject touch. The shortcut below and `adb shell input` won't work, so enable the accessibility service from the app's *Enable* button and test gestures by hand. This toggle may require a Mi account and a SIM.

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
| **Unit** (JVM) | JUnit | `HandleGestures`: drag distance → steps (24 dp per step, direction, no step on jitter < 24 dp, long-press vs drag) |
| **Unit** (Robolectric) | `ShadowAudioManager` | `SystemVolume`: `showPanel()` uses `ADJUST_SAME` + `FLAG_SHOW_UI`; `step()` raises/lowers the level; `SecurityException` → `false`. `PrefsRepository` round-trip |
| **Compose UI** | `createComposeRule` | Setup screen: service off/on states, buttons fire the right callbacks, prefs controls update `Prefs` |
| **On-device** | Instrumented tests + adb | Service connects and the handle window exists; tap shows the system panel; drag changes the media level |
| **Manual matrix** | Emulators + your phone | See below |

### Device / scenario matrix (M5)

| Scenario | API 29 emu | API 34 emu | API 36 emu | Physical phone |
|---|---|---|---|---|
| Tap handle → system panel over YouTube / Chrome / a game | ☐ | ☐ | ☐ | ☐ |
| Drag along handle → media volume steps while music plays | ☐ | ☐ | ☐ | ☐ |
| Drag with nothing playing → device-default stream (same as keys) | ☐ | ☐ | ☐ | ☐ |
| Drag during a call → call volume | — | ☐ | ☐ | ☐ |
| Gestures with **gesture nav** (no accidental back) | ☐ | ☐ | ☐ | ☐ |
| Gestures with **3-button nav** | ☐ | ☐ | ☐ | ☐ |
| QS tile → shade collapses (31+) and panel shows | ☐ | ☐ | ☐ | ☐ |
| Handle visible over the notification shade | ☐ | ☐ | ☐ | ☐ |
| Over an app using `HIDE_OVERLAY_WINDOWS` (e.g. banking) | n/a | ☐ | ☐ | ☐ |
| Lowering ring into silent with DND on → falls back to the panel, no crash | ☐ | ☐ | ☐ | ☐ |
| Expand system panel → ring / notification / alarm reachable | ☐ | ☐ | ☐ | ☐ |
| Bluetooth headphones: media level follows | — | — | — | ☐ |
| Rotation / split-screen: handle stays on the edge | ☐ | ☐ | ☐ | ☐ |
| Reboot: handle returns without opening the app | ☐ | ☐ | ☐ | ☐ |
| App swiped from recents: handle persists | ☐ | ☐ | ☐ | ☐ |
| TalkBack: handle actions (show panel / up / down) | ☐ | ☐ | ☐ | ☐ |

## Release (personal sideload)

- **Signing:** a keystore kept **outside the repo**. `keystore.properties` (git-ignored) is read by `app/build.gradle.kts`.
- **R8** enabled for release (`isMinifyEnabled = true`, `isShrinkResources = true`). The accessibility and tile services are referenced from the manifest, so R8 keeps them automatically.
- `./gradlew assembleRelease`, then `adb install -r app/build/outputs/apk/release/app-release.apk`.
- Version: `versionCode` increments per build; `versionName` follows semver.

## Definition of done (v1.0)

- [ ] Tap → system panel and drag → volume steps work from any app on API 29/34/36 emulators and the physical phone
- [ ] All streams reachable through the system panel; refused steps fall back to the panel without crashing
- [ ] QS tile shows the panel (and opens setup when the service is off)
- [ ] No `INTERNET` permission in the merged manifest (`./gradlew :app:processReleaseManifest` + inspect)
- [ ] Unit + Compose UI tests green
- [ ] Signed release APK installed and verified after a reboot
