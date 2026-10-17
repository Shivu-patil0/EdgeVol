# 01 · Competitor research

> Goal: learn how existing apps solve "control volume without hardware buttons", how they're triggered, what engine they use, and where they fall short for a **minimal, hidden, works-everywhere** experience.
>
> Research date: October 2026. Feature details come from store listings, project READMEs and review articles (see [Sources](#sources)); some vary between versions.

## Landscape at a glance

| App | Trigger(s) | Engine (as described) | UI | Business model | Where it falls short for us |
|---|---|---|---|---|---|
| **Volume Styles** | Replaces the panel shown by the volume keys; some versions add an edge trigger | Accessibility + overlay | Heavily themable panel (many skins) | Freemium, ads | Built for people whose keys still work; theming adds bulk |
| **Precise Volume (+EQ/Booster)** | Notification controls, floating popup, QS tile, widgets | Background service + overlay | 100+ volume steps, EQ, booster, per-device presets | Freemium | A power-user audio tool; far from minimal |
| **Assistive Volume Button** | **Always-visible** draggable floating buttons; optional edge swipe | Accessibility + overlay | Floating +/- buttons | Free with ads | Permanently covers content; only steps volume, no per-stream sliders |
| **Volume Control Panel (Pro)** | Custom popup, QS tile, home-screen widget | Accessibility + overlay | Large sliders, layouts and themes | Paid / freemium | Lots of settings; not invisible by default |
| **Volume Control: Native Volume** | Floating control, QS tiles, widgets | Overlay; opens the **system** volume panel | System panel | Free, no ads | Depends on the OEM's system panel; floating control is visible |
| **DhwaniControl** (open source, Apache-2.0) | **QS tile + edge swipe + floating button** | Overlay + **foreground service** | Opens the system volume panel; Material 3 app UI | Free, F-Droid | Closest to our goal. Its foreground service needs a permanent notification and can be killed by OEM battery savers; floating windows are hidden by apps that block overlays |
| **Samsung SoundAssistant** (Good Lock) | Floating volume button, per-app volume | System-privileged (Samsung only) | Extended Samsung panel | Free | Samsung One UI only |
| **Built-in "Accessibility Menu"** (Android Accessibility Suite) | Accessibility button / gesture | System accessibility app | Large full-screen grid with volume ± | Free, preinstalled on many devices | Bulky grid menu, ± steps only, no per-stream control |

## How they're built (patterns)

| Pattern | Used by | Pros | Cons |
|---|---|---|---|
| **Floating window** (`SYSTEM_ALERT_WINDOW`, `TYPE_APPLICATION_OVERLAY`) + foreground service | DhwaniControl, Native Volume, Precise Volume | Play-Store friendly | Permanent notification; hidden by apps that set `HIDE_OVERLAY_WINDOWS` (banking/payment apps, Android 12+); can't draw over the notification shade; OEM killers |
| **Accessibility overlay** (`TYPE_ACCESSIBILITY_OVERLAY`) | Assistive Volume Button, Volume Styles, Volume Control Panel | Most robust: draws over everything, no notification, kept alive by the system | Play review (declaration form + prominent disclosure); Android 13+ "restricted settings" for sideloaded APKs |
| **Quick Settings tile** (`TileService`) | DhwaniControl, Precise Volume, Volume Panel Pro, Native Volume | Zero on-screen footprint, available from the lock screen | Two gestures (pull shade + tap) |
| **Show the system panel** (`adjustStreamVolume(..., FLAG_SHOW_UI)`) | Native Volume, DhwaniControl | Zero custom UI | Look and behavior vary by OEM; per-stream access varies |
| **Always-visible bubble** | Assistive Volume Button | Discoverable | Clutter, the opposite of "hidden" |

## What we adopt, avoid, and add

| | Decision |
|---|---|
| ✅ **Adopt** | QS tile + edge trigger (DhwaniControl); **show the system volume panel** instead of a custom one (DhwaniControl, Native Volume); no ads, no trackers, open-source mindset |
| ❌ **Avoid** | Always-visible bubbles, heavy theming, ads, permanent foreground notification, EQ/booster scope creep |
| ⭐ **Add** | **Invisible by default** (4 dp sliver, configurable); **the handle is a virtual volume rocker** (drag along it to step volume, like the hardware keys); **works over the notification shade and apps that block overlays** (accessibility overlay); **no `INTERNET` permission**, no DND permission, no permanent notification |

## Positioning

> *"Your volume keys, back on the screen edge: invisible until you need them, working in every app, showing the panel your phone already has."*

## Sources

- DhwaniControl, GitHub: https://github.com/RajnishKMehta/DhwaniControl
- DhwaniControl on F-Droid: https://f-droid.org/packages/io.github.rajnishkmehta.dhwanicontrol/
- Volume Control: Native Volume, Google Play: https://play.google.com/store/apps/details?id=com.iprototypes.volume
- Volume Control – Volume Slider, Google Play: https://play.google.com/store/apps/details?id=com.remi.customvolume.volumecontrol
- "6 Useful Android Volume Control Apps", The Droid Guy: https://thedroidguy.com/6-useful-android-volume-control-apps-to-fine-tune-your-device-1263542
- "Best Volume Control Apps for Android", the.hosting: https://the.hosting/en/help/best-volume-control-apps-for-android
- Google Play accessibility API policy: https://support.google.com/googleplay/android-developer/answer/10964491
