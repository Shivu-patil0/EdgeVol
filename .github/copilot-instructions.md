# EdgeVol: project guidelines

EdgeVol is a sideloaded Android app that puts **virtual volume keys** on the screen edge. An invisible handle, drawn by an AccessibilityService overlay, works in every app: a tap shows **Android's built-in volume panel**, and dragging along the handle steps volume like Volume +/−. A Quick Settings tile does the same. The specs live in [docs/](../docs/README.md); **docs are the source of truth**. Read the relevant doc before changing behavior.

## Stack & build
- Kotlin, Jetpack Compose (Material 3; setup screen only), single `app` module, DataStore, Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`). minSdk 26, target/compile 36, JDK 17.
- `./gradlew assembleDebug` · `./gradlew testDebugUnitTest` · `./gradlew connectedDebugAndroidTest` (more in [docs/06](../docs/06-build-test-release.md)).
- Source root: `app/src/main/java/app/edgevol/`.

## Non-negotiable invariants
- **No custom volume UI.** All volume changes go through `SystemVolume` (`adjustSuggestedStreamVolume` + `FLAG_SHOW_UI`), so Android shows its own panel.
- **No `INTERNET` permission**, no analytics, ads or tracking SDKs. The only user-granted capability is the accessibility service (no DND, `SYSTEM_ALERT_WINDOW` or foreground service).
- Accessibility config keeps `canRetrieveWindowContent="false"`. The service never reads screen content.
- The handle is the only overlay window: `TYPE_ACCESSIBILITY_OVERLAY` + `FLAG_NOT_FOCUSABLE`. Remove it when the service unbinds.
- Every API ≥ 27 call is gated with `Build.VERSION.SDK_INT` (see the version table in [docs/02](../docs/02-feasibility-and-constraints.md)).
- Dependencies are limited to the stack above. Anything new needs user approval.

## Agent team
A **manager** agent coordinates four specialists ([docs/07](../docs/07-agent-team.md)). Every agent edits **only the paths it owns**. Anything else goes back to the manager as a *change request*.

| Owner | Paths |
|---|---|
| `manager` | `docs/**`, `.github/**`, `.gitignore`, `README.md`, git commits |
| `platform-dev` | `settings.gradle.kts`, `build.gradle.kts`, `gradle/**`, `gradlew*`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/app/edgevol/{audio,overlay,service,data,system}/**`, `app/src/main/res/xml/**`, `app/src/main/res/values/strings_system.xml` |
| `ui-dev` | `app/src/main/java/app/edgevol/{ui/**,MainActivity.kt}`, `app/src/main/res/{drawable*,mipmap*}/**`, `app/src/main/res/values/{strings,colors,themes}.xml` |
| `qa` | `app/src/test/**`, `app/src/androidTest/**` |
| `analyst` | nothing (read-only) |

## Collaboration rules
- **One Gradle process at a time.** Only the agent whose brief says `Build: allowed` may run `./gradlew`.
- The UI ↔ platform API is the contract in [docs/03 § Contract](../docs/03-architecture.md#ui--platform-contract). Changing it requires the manager's approval, and the report must list it under `Contract changes`.
- Specialists can't ask the user questions. They return `Questions:` to the manager instead of guessing on product decisions.
- Scratch files go in `.agent-work/` (git-ignored). Progress and rulings go in [docs/progress.md](../docs/progress.md).
- Only comment code that needs clarification. Skip formatting-only changes.
