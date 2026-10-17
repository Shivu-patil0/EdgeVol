<p align="center">
  <img src="images/logo.svg" width="128" alt="EdgeVol logo (temporary)"/>
</p>

<h1 align="center">EdgeVol: hidden, system-wide volume keys for Android</h1>

<p align="center"><i>Working name. Your volume buttons are broken, so get them back on the screen edge, in every app.</i></p>

---

## What it is

EdgeVol is a minimal Android app that replaces the hardware volume keys with an invisible **edge handle** that works **in any app**:

- **Hidden by default.** Only a 4 dp sliver sits on the screen edge (or nothing at all if you use the Quick Settings tile).
- **Tap** the handle to bring up **Android's own volume panel**, the same one the hardware keys show.
- **Drag up / down along the handle** to change volume step by step, exactly like pressing Volume + / −.
- **All streams.** Media, ring, notification, alarm and call through the system panel's own expand button.
- **Private.** No `INTERNET` permission, no trackers, and it never reads screen content.

![Interaction mockup](images/panel-mockup.svg)

## Decisions so far

| Topic | Decision | Why |
|---|---|---|
| Distribution | **Sideloaded APK** (personal use) | Lets us use the most robust engine without Play's AccessibilityService declaration form, prominent-disclosure screen and review |
| Engine | **AccessibilityService + `TYPE_ACCESSIBILITY_OVERLAY`** | Draws over every app *and* the notification shade, isn't hidden by apps that block overlays, needs no foreground notification, and isn't killed by OEM battery savers |
| Volume UI | **Android's built-in volume panel** (`FLAG_SHOW_UI`), no custom panel | Identical to the hardware keys, native look on every OEM, least code, no DND permission |
| Triggers | **Invisible edge handle (tap = show, vertical drag = step) + Quick Settings tile** | Always reachable, never in the way; a vertical drag avoids the horizontal back gesture |
| Stack | **Kotlin, Jetpack Compose (Material 3, setup screen only), single module, DataStore** | All the work is native system integration, so cross-platform frameworks add nothing |
| Min / target SDK | **26 / 36** | Android 8+ covers almost every device in use |
| Dev workflow | **Repo-scoped agent team**: manager + analyst, UI dev, platform dev, QA | Small per-agent context, independent review, strict file ownership ([07](07-agent-team.md)) |

## Document map

| # | Document | What's inside |
|---|---|---|
| 01 | [Competitor research](01-competitor-research.md) | Existing apps, how they're triggered and built, gaps we fill |
| 02 | [Feasibility & constraints](02-feasibility-and-constraints.md) | Does it work in *every* app? Platform limits, handle host and volume-UI decisions, risks |
| 03 | [Architecture](03-architecture.md) | Components, gesture flows, state machine, manifest, UI ↔ platform contract |
| 04 | [UI design](04-ui-design.md) | Handle gestures, system panel behavior, haptics, theming, setup screen |
| 05 | [Onboarding & permissions](05-onboarding-and-permissions.md) | First-run flow, every permission and why, privacy |
| 06 | [Build, test & release](06-build-test-release.md) | Toolchain, agent skills, test strategy and device matrix, signing |
| 07 | [Agent team](07-agent-team.md) | Multi-agent workflow: manager + specialists, file ownership, gates |
| — | [Progress log](progress.md) | Task results and rulings per milestone |

## Roadmap

| Milestone | Scope | Status |
|---|---|---|
| **M0 Setup** | Repo-scoped skills and agent team, documentation, temporary logo, git | ✅ done |
| **M1 Skeleton** | Gradle project, theme, setup screen with live service status | ✅ done (build, unit tests, lint and manifest verified; status detection and enable flow verified on a POCO X2, see [progress](progress.md)) |
| **M2 Core engine** | `VolumeAccessibilityService`, edge handle overlay, gestures (tap → panel, drag → steps), `SystemVolume`, `ui-tooling` previews | ⏳ next |
| **M3 Triggers & settings** | Quick Settings tile, handle customization (side, position, size, opacity) | ⬜ |
| **M4 Polish** | Haptics, accessibility actions on the handle, adaptive launcher icon from the logo, dark/light, dynamic color | ⬜ |
| **M5 Test & ship** | Unit + Compose UI tests, emulator/device matrix, signed release APK | ⬜ |

## Logo (temporary)

| Full-color | Monochrome glyph (tile / small icons) |
|---|---|
| <img src="images/logo.svg" width="96"/> | <img src="images/logo-mono.svg" width="48"/> |

Three volume pills at different levels, plus the "hidden edge handle" on the right: a quick visual summary of the product. It's meant to be replaced before any public release.
