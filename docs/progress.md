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

## M1 · Skeleton ✅ (device checks pending)

Built and verified offline: no phone and no emulator were available, so nothing ran on a device.

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

**Open (needs a phone with USB debugging):** live status detection and refresh on resume; the enable flow incl. Android 13+ restricted settings; Compose rendering, 200% font scale and TalkBack; dark-mode cold start (a white window flash is expected, see below).

- Ruling: toolchain = Google's `android` CLI, SDK packages only (no emulator, no Android Studio) — user decision — on-device checks need a phone with USB debugging, or an emulator added later.
- Ruling: contract delivered per milestone; M1 `SetupScreen(status, onEnableService, onOpenAppInfo, modifier)` — user decision (2026-10-17), recorded in [03](03-architecture.md#ui--platform-contract) — M2/M3 add `onTestPanel` and `prefs`/`onAddTile`/`onPrefsChange`, so the M2/M3 UI changes touch `SetupScreen` and `MainActivity` again.
- Ruling: AGP 9.4.1 with built-in Kotlin, Gradle 9.8.1, Kotlin 2.4.21 — user decision — no fallback to 2.3.21 was needed; AGP 9 and the Compose plugin work together.
- Ruling: `compileSdk` 37, `targetSdk` stays 36 — Compose BOM 2026.09.00 AARs refuse compileSdk 36 and the alternative is pinning an older BOM; `compileSdk` does not change runtime behaviour — Gradle auto-downloaded `platforms/android-37.0` (within the accepted SDK license); lint's `OldTargetApi` warning is the cost.
- Ruling: `applicationId` `app.edgevol` is final; `allowBackup="false"`; root `gradle.properties` owned by platform-dev; launcher icon in `mipmap-anydpi/` — user decisions.
- Ruling: no `ui-tooling`/`@Preview` dependencies in M1 (ui-dev's change request) — new dependencies need user approval — no previews until approved; M5 UI tests don't need them.
- Ruling: dark-mode cold-start white flash (`Theme.EdgeVol` has a Light parent; `values-night` is outside ui-dev's paths) deferred to M4 polish — cosmetic — a brief white flash on dark-mode launches until then.
- Ruling: accept analyst suggestions as non-blocking: icon pills slightly outside the 66dp circular safe zone (M4 replaces the art); `innerPadding` clips the scroll viewport instead of scrolling under the bars; the disclosure text is duplicated in `strings.xml` and `a11y_service_description` — cosmetic/low risk — copies may drift until M4.
- Ruling: the service description and setup text mention the Quick Settings tile before it exists (M3) — spec text per docs/05 — users see a tile mention for two milestones.
- Note: the Gradle wrapper download from the GitHub release-assets redirect timed out in the JVM on first run; the verified zip was pre-seeded into `~/.gradle/wrapper/dists`. Retry on a fresh machine.
