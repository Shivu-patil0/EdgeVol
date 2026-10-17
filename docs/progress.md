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

## M2 · Core engine ⏳ (in progress)

Plan: user approved all defaults at Gate 1. Base commit for review diffs: `4cac952` (after committing the pending roadmap change).

| Task | Owner | Result | Commit |
|---|---|---|---|
| M0 Docs/process prep (03/04/05/06/07, ownership) | manager | Done | see git log |

- Ruling: R1 handle position is in memory only until M3 (`HandleDefaults`: RIGHT, 40%, 72 dp, 35%) — user-approved default — position resets when the service restarts until M3.
- Ruling: R2 *Try it* shows in both Off and On states — needs no permission, isolates `SystemVolume` from overlay bugs.
- Ruling: R3 contract change `SetupStatus.showRestrictedSettingsHelp` (default `false`, set from `SDK_INT >= 33` in the platform) — keeps `SetupScreen` stateless and SDK checks in platform code — M1 callers still compile.
- Ruling: R4 previews live in `app/src/debug/java/app/edgevol/ui/**`; ui-dev ownership extended; `ui-tooling` + `ui-tooling-preview` are `debugImplementation` only — the debug manifest gains an exported `PreviewActivity`, release does not.
- Ruling: R5 window flags add `FLAG_LAYOUT_IN_SCREEN` and title `EdgeVolHandle` (docs/03) — y = percent of real screen height and the window is greppable in `dumpsys`.
- Ruling: F4/R6 the accessibility config drops `accessibilityEventTypes`; fallback is `typeWindowStateChanged` + `packageNames=app.edgevol` — verified on the phone.
- Ruling: F3 service description / in-app disclosure rewritten in plain language (no `canRetrieveWindowContent`, no tile sentence until M3).
