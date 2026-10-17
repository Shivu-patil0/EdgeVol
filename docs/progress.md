# Progress log

Maintained by the `manager` agent. One section per milestone: tasks, results, and `Ruling:` lines (`Ruling: <decision> — <why> — <cost if wrong>`).

## M0 · Setup ✅

| Task | Owner | Result |
|---|---|---|
| Competitor research, feasibility, architecture, UI, onboarding, build/test docs + images | manager (with user) | Done: [docs/](README.md) |
| Temporary logo | manager | Done: [logo.svg](images/logo.svg), [logo-mono.svg](images/logo-mono.svg) |
| Repo-scoped agent skills | manager | 6 skills in `.agents/skills/`, pinned in `skills-lock.json` |
| Agent team + shared instructions | manager | 5 agents in `.github/agents/`, rules in `.github/copilot-instructions.md` |
| Git repository | manager | Initialized (`main`) |

- Ruling: distribution = sideloaded APK — user decision — Play review would be needed later.
- Ruling: handle hosted by an AccessibilityService overlay — most robust over every app and the shade — Play distribution would need option B.
- Ruling: volume UI = **Android's built-in panel** via `FLAG_SHOW_UI`; the handle doubles as a drag-to-step volume rocker; no custom panel — user decision — expanded multi-stream view needs one extra tap on OEM panels.
- Ruling: no DND / notification-policy permission — refused steps fall back to showing the system panel — a few ringer edge cases need one manual slider move.

## M1 · Skeleton ✅ (phone-verified)

Built and verified offline, then checked on a real phone (see "Device verification" below).

| Task | Owner | Result | Commit |
|---|---|---|---|
| P1 Gradle scaffold, wrapper, toolchain proof | platform-dev | Done. Gradle 9.8.1 / AGP 9.4.1 / Kotlin 2.4.21; `assembleDebug` + `assembleRelease` green | `de5dfe4` |
| P2 Contract types, parser, stub service | platform-dev | Done: `SetupStatus`, `SystemSettings`, `isServiceEnabled`, stub `VolumeAccessibilityService` | `6524624` |
| P3 Manifest, accessibility config, description | platform-dev | Done: no `<uses-permission>`, `canRetrieveWindowContent=false` | `5bfbbd7` |
| U1 Theme, resources, adaptive icon | ui-dev | Done | `16a0134` |
| U2 `SetupScreen` + `MainActivity` | ui-dev | Done (reduced M1 signature) | `8c21ae0` |
| Q1 Parser tests + build/lint/manifest/APK checks | qa | Done: 24 tests green; lint 0 errors / 4 warnings | `98c1ef3` |

Verification (all prefixed with `JAVA_HOME=<JDK 17>`): `./gradlew testDebugUnitTest lintDebug assembleDebug :app:processDebugManifest :app:processReleaseManifest` PASS (24 tests, 0 failures); `./gradlew assembleRelease` PASS (R8 + shrink, unsigned). Merged manifests (debug and release) hold only the androidx `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; exported components are `MainActivity`, the service (guarded by `BIND_ACCESSIBILITY_SERVICE`) and androidx `ProfileInstallReceiver` (guarded by `DUMP`). `aapt2 badging`: package `app.edgevol`, versionCode 1 / `0.1.0`, minSdk 26, targetSdk 36, launchable `app.edgevol.MainActivity`, label EdgeVol.

Lint warnings (non-blocking): `OldTargetApi` (targetSdk 36 while compileSdk is 37), `DataExtractionRules` (from `allowBackup="false"`), `MonochromeLauncherIcon` (no monochrome layer; M4), `UseKtx` (`Uri.parse`; would need core-ktx).

**Device verification (2026-10-17, POCO X2 · Android 11 / API 30 · MIUI 12.5, connected over Wi-Fi debugging):**
- ✅ Debug APK installs and launches; no crash.
- ✅ Setup screen renders (Off state: status card, disclosure, Enable, restricted-settings help). The handwriting-style font is the phone's system font.
- ✅ *Enable* deep-links to Accessibility settings; after switching EdgeVol on, returning to the app shows **On ✓** (refresh on resume works). `dumpsys accessibility` lists the service as enabled and the process is running.
- ⬜ Still unchecked: 200% font scale, TalkBack, dark-mode cold start, Android 13+ restricted-settings flow (this phone is Android 11, so it can't show that step).

**Device facts that affect later milestones:**
- Gesture navigation is on (`navigation_mode=2`), so the back-gesture conflict is real: the handle's vertical drag and exclusion rects must be tested here in M2.
- MIUI blocks adb from writing secure settings and injecting touch (`WRITE_SECURE_SETTINGS` / `INJECT_EVENTS` denied) unless *USB debugging (Security settings)* is on. So the docs/06 adb shortcut for enabling the service doesn't work, and gestures can't be scripted; gestures must be tested by hand, or the user enables that toggle (MIUI may require a Mi account and SIM).
- MIUI also needed *Install via USB* turned on before `adb install` worked.
- Xiaomi/MIUI is known for aggressive background and permission handling. Check in M2 that the handle survives screen off, app swipe-away, and low-memory kills.
- The setup screen says "Ready. Tap the handle…" when the service is On, but the handle doesn't exist until M2. This is accepted for M1, and the copy must be revisited if M2 is delayed.

**Open (needs more devices or an emulator):** Android 13+ restricted-settings flow; dark-mode cold start (a white window flash is expected, see below).

**Backlog decisions for M2:** the user approved adding `ui-tooling` and `ui-tooling-preview` as debug-only dependencies (platform-dev owns Gradle; add them in the first M2 wave A task so ui-dev can add `@Preview`s).

- Ruling: toolchain = Google's `android` CLI, SDK packages only (no emulator, no Android Studio) — user decision — on-device checks need a phone with USB debugging, or an emulator added later.
- Ruling: contract delivered per milestone; M1 `SetupScreen(status, onEnableService, onOpenAppInfo, modifier)` — user decision (2026-10-17), recorded in [03](03-architecture.md#ui--platform-contract) — M2/M3 add `onTestPanel` and `prefs`/`onAddTile`/`onPrefsChange`, so the M2/M3 UI changes touch `SetupScreen` and `MainActivity` again.
- Ruling: AGP 9.4.1 with built-in Kotlin, Gradle 9.8.1, Kotlin 2.4.21 — user decision — no fallback to 2.3.21 was needed; AGP 9 and the Compose plugin work together.
- Ruling: `compileSdk` 37, `targetSdk` stays 36 — Compose BOM 2026.09.00 AARs refuse compileSdk 36 and the alternative is pinning an older BOM; `compileSdk` does not change runtime behaviour — Gradle auto-downloaded `platforms/android-37.0` (within the accepted SDK license); lint's `OldTargetApi` warning is the cost.
- Ruling: `applicationId` `app.edgevol` is final; `allowBackup="false"`; root `gradle.properties` owned by platform-dev; launcher icon in `mipmap-anydpi/` — user decisions.
- Ruling: no `ui-tooling`/`@Preview` dependencies in M1 (ui-dev's change request) — new dependencies need user approval — **user later approved them (debug-only) for M2**.
- Ruling: dark-mode cold-start white flash (`Theme.EdgeVol` has a Light parent; `values-night` is outside ui-dev's paths) deferred to M4 polish — cosmetic — a brief white flash on dark-mode launches until then.
- Ruling: accept analyst suggestions as non-blocking: icon pills slightly outside the 66dp circular safe zone (M4 replaces the art); `innerPadding` clips the scroll viewport instead of scrolling under the bars; the disclosure text is duplicated in `strings.xml` and `a11y_service_description` — cosmetic/low risk — copies may drift until M4.
- Ruling: the service description and setup text mention the Quick Settings tile before it exists (M3) — spec text per docs/05 — users see a tile mention for two milestones.
- Note: the Gradle wrapper download from the GitHub release-assets redirect timed out in the JVM on first run; the verified zip was pre-seeded into `~/.gradle/wrapper/dists`. Retry on a fresh machine.

## M2 · Core engine ⏳ (built and verified over adb; touch gestures await the user's manual script)

Plan: user approved all defaults at Gate 1. Review diffs were taken against `4e71f8e` (after the M0 docs commits).

| Task | Owner | Result | Commit |
|---|---|---|---|
| M0 Docs/process prep (03/04/05/06/07, ownership) | manager | Done | `4cac952`, `4e71f8e` |
| A1 Debug-only `ui-tooling` + `ui-tooling-preview` | platform-dev | Done | `70e9c36` |
| A2 `SystemVolume`, `HandleGestures`, `SetupStatus.showRestrictedSettingsHelp` | platform-dev | Done | `8a73ca2` |
| A3 Plain-language service description, config without event types | platform-dev | Done | `94b7c41` |
| P3 `HandleDefaults`, `EdgeHandleView`, `OverlayController` | platform-dev | Done (+1 compile fix, 2 review fixes) | `153e84b` |
| P4 Real `VolumeAccessibilityService` | platform-dev | Done | `496285f` |
| U1 `SetupScreen` M2 form, `MainActivity`, copy | ui-dev | Done | `89645ba` |
| U2 Debug-only `@Preview`s | ui-dev | Done (compile-checked only) | `07cea23` |
| Q1 Unit tests (56 new) | qa | Done | `53532e4` |
| Q2 Full Gradle check | qa | PASS (second run; first run found one wrong import) | n/a |
| Q3 Automatic device checks | qa | PASS except the `kill -9` restart check (not possible on this phone) | n/a |

Verification: `JAVA_HOME=<JDK 17> ./gradlew testDebugUnitTest lintDebug assembleDebug :app:processDebugManifest :app:processReleaseManifest assembleRelease` PASS, **80 tests, 0 failures** (HandleGestures 36, SystemVolume 6, resource text 8, status gating 6, EnabledServices 24). Lint 0 errors / 8 warnings: the 4 from M1 plus `ClickableViewAccessibility` (`EdgeHandleView`), `ViewConstructor` (`EdgeHandleView`), `RtlHardcoded` ×2 (`OverlayController`; physical edge is intended). Merged manifests: no `INTERNET`, only the androidx dynamic-receiver permission; release exports `MainActivity`, the service (`BIND_ACCESSIBILITY_SERVICE`) and `ProfileInstallReceiver` (`DUMP`); debug additionally exports `androidx.compose.ui.tooling.PreviewActivity`; release APK (≈945 KiB, unsigned) holds no tooling classes.

**Device verification (POCO X2, Android 11, MIUI 12.5, gesture nav) by adb:**
- ✅ `install -r` keeps the service enabled (MIUI did not switch it off) and the service rebinds by itself ("handle added" in logcat).
- ✅ **F4:** `dumpsys accessibility` lists EdgeVol under *Bound services* with `eventTypes=` empty and `capabilities=0`, so a service with no `accessibilityEventTypes` connects and draws its handle on this phone. No fallback needed.
- ✅ Exactly one window `EdgeVolHandle`: `ty=2032`, `NOT_FOCUSABLE | LAYOUT_IN_SCREEN | LAYOUT_NO_LIMITS`, `mFrame=[1014,861][1080,1059]` (66×198 px = 24×72 dp at 440 dpi, on the right edge, centre y = 960 = 40% of 2400). A screenshot crop showed the faint bar. It stays the only EdgeVol overlay with the app in the background.
- ✅ Setup screen shows **On**, the new *Try it* card and the new ready text.
- ⬜ `kill -9` restart resilience: `run-as app.edgevol kill` reports "unknown pid" and plain `adb shell kill` is not permitted, so it cannot be scripted on this phone. Do it by swiping the app away / running the MIUI cleaner (script step 16).
- ⬜ Touch gestures, haptics, back-gesture conflict, other apps and the shade, rotation, screen off, reboot, TalkBack: the user's manual script.
- Device fact: `input keyevent` is also denied (no `INJECT_EVENTS`); ring/notification/system streams were at 0 and muted when the checks ran (silent mode), music was 27.

- Ruling: R1 handle position is in memory only until M3 (`HandleDefaults`: RIGHT, 40%, 72 dp, 35%) — user-approved default — position resets when the service restarts until M3.
- Ruling: R2 *Try it* shows in both Off and On states — needs no permission, isolates `SystemVolume` from overlay bugs.
- Ruling: R3 contract change `SetupStatus.showRestrictedSettingsHelp` (default `false`, set from `SDK_INT >= 33` in the platform) — keeps `SetupScreen` stateless and SDK checks in platform code — M1 callers still compile.
- Ruling: R4 previews live in `app/src/debug/java/app/edgevol/ui/**`; ui-dev ownership extended; `ui-tooling` + `ui-tooling-preview` are `debugImplementation` only — the debug manifest gains an exported `PreviewActivity`, release does not.
- Ruling: R5 window flags add `FLAG_LAYOUT_IN_SCREEN` and title `EdgeVolHandle` (docs/03) — y = percent of real screen height and the window is greppable in `dumpsys`.
- Ruling: F4/R6 the accessibility config drops `accessibilityEventTypes`; fallback is `typeWindowStateChanged` + `packageNames=app.edgevol` — verified on the phone.
- Ruling: F3 service description / in-app disclosure rewritten in plain language (no `canRetrieveWindowContent`, no tile sentence until M3).
- Ruling: F4 outcome — no fallback needed; the service connects and draws the handle with no `accessibilityEventTypes` on the POCO X2 (Android 11) — Android 13+/other OEMs are untested; fallback R6 stays documented in docs/03.
- Ruling: accept the 4 new lint warnings (`ClickableViewAccessibility`, `ViewConstructor`, `RtlHardcoded` ×2) — the handle is a gesture-only view built in code, `performClick()` is reached via `handle()` on tap, and RIGHT/LEFT gravity is deliberately physical — warnings are non-blocking since M1; a `@Suppress` with a comment can come in M4 polish.
- Ruling: `HandleDefaults.HandleSide { Left, Right }` in `overlay` is M2-only and internal — M3 must reconcile it with `app.edgevol.data.HandleSide { LEFT, RIGHT }` from the contract — naming clash otherwise.
- Ruling: `OverlayController` writes the effective (screen-clamped) percent back after each layout (review fix: no dead zone on short screens) — after a landscape rotation on a short screen the stored percent can change slightly — acceptable until M3 persists the position.
- Ruling: the handle view disables its click sound (`isSoundEffectsEnabled = false`), so taps give only haptic feedback — platform-dev choice, not in the spec — revisit in M4 haptics polish.
- Ruling: no separate final full-diff review after the build (waves A and B were each reviewed by the analyst; later changes were one import fix and two small review fixes, read by the manager) — the user's manual run is the real gate — a missed bug would show in the manual script.
- Note: landscape cutout handling (`layoutInDisplayCutoutMode` not set), rotation timing of `currentWindowMetrics` in `onConfigurationChanged`, and whether MIUI honours gesture-exclusion rects for an accessibility overlay are unverified until the user's script steps 12 and 14.
