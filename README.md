<p align="center">
  <img src="docs/images/logo.svg" width="112" alt="EdgeVol logo"/>
</p>

<h1 align="center">EdgeVol</h1>

<p align="center"><i>Volume keys broken? EdgeVol puts them back on the screen edge, in every app.</i></p>

EdgeVol is a small, private Android app (Android 8.0+) for phones with broken volume buttons:

- **A faint handle on the screen edge**, drawn above every app. **Tap** it to bring up Android's own volume panel, the same one the hardware keys show (media, ring, notification, alarm and call through its expand button).
- **Hold, then drag** to park it on the left or right edge at any height. It keeps the same relative height in landscape.
- **Quick Settings tile "Volume handle"** hides the handle for a game or video and shows it again.
- **Private:** no internet permission, no trackers, and the accessibility service never reads screen content. It only asks Android to show its volume panel.

![How it works](docs/images/panel-mockup.svg)

## Download

**[⬇ Download EdgeVol.apk](https://github.com/Shivu-patil0/EdgeVol/releases/latest/download/EdgeVol.apk)** (≈ 1.2 MB, Android 8.0+, [all releases](https://github.com/Shivu-patil0/EdgeVol/releases))

Open the downloaded file on the phone. What to expect, because EdgeVol isn't from the Play Store:

- Android asks to allow installs from your browser or file manager (*Install unknown apps*): allow it once.
- **Google Play Protect may warn** about an app from an unknown developer, especially because it uses an accessibility service: tap *More details* → *Install anyway*. EdgeVol has no internet permission and never reads the screen.
- Android 13+: the Accessibility toggle may be greyed out at first; see step 2 below.
- Updates install over the old version only if they come from this page (same signing key).

## Install (sideload)

1. Install the APK from **Download** above (or `adb install -r EdgeVol.apk`, or build it yourself, below).
2. Open **EdgeVol** → **Enable** → *Accessibility* → **EdgeVol** → On. The handle appears at once.
   On Android 13+, if the toggle is greyed out: *App info* → ⋮ → *Allow restricted settings*.
3. **Xiaomi / Redmi / POCO:** follow the *Keep EdgeVol running* card (allow Autostart, lock EdgeVol in Recents), or clearing recent apps stops it.
4. Optional: add the **Volume handle** tile in Quick Settings.

Details: [onboarding & permissions](docs/05-onboarding-and-permissions.md).

## Build

Needs JDK 17 and the Android SDK (command-line setup in [docs/06](docs/06-build-test-release.md)).

```bash
./gradlew assembleRelease        # R8-shrunk APK, ~1.2 MB (signed if a key is configured, see docs/06)
./gradlew testDebugUnitTest      # 223 JVM tests, incl. Robolectric UI tests on simulated Android 8.0 / 11 / 15
scripts/dev-check.sh release     # build, install on a connected phone, show service and handle status
```

Kotlin · Jetpack Compose (setup screen only) · AccessibilityService overlay · DataStore. Single module, no internet permission, no third-party SDKs.

## Docs

The specs are the source of truth: [docs/](docs/README.md) has the competitor research, feasibility, architecture, UI design, onboarding, build/test/release and the progress log per milestone.

Tested on a POCO X2 (MIUI 12.5, Android 11). Other brands and stock Android haven't been tried on a real device yet.
