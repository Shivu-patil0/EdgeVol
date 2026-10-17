# 05 · Onboarding & permissions

![Onboarding flow](images/onboarding-flow.svg)

## First-run flow

1. **Install the APK.** `adb install app-release.apk` is preferred (usually avoids step 3), or open the `.apk` on the phone.
2. **Open EdgeVol.** The setup screen shows live status that updates in `onResume`.
3. **Allow restricted settings** *(only if the Accessibility toggle is greyed out; Android 13+ sideloads).* Settings → Apps → EdgeVol → **⋮** → *Allow restricted settings*. The setup screen offers an *Open app info* button (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`).
4. **Enable the service.** The *Enable* button deep-links to `Settings.ACTION_ACCESSIBILITY_SETTINGS`. Once it's on, the edge handle appears immediately.
5. **Add the QS tile** *(optional).* On Android 13+ a one-tap system prompt via `StatusBarManager.requestAddTileService()`; on older versions, short instructions to edit the Quick Settings tiles.

✅ **Ready:** tap the handle to see the phone's own volume panel, or drag along it to change volume, in any app.

## Permissions

| Permission / capability | Why | When requested | If denied |
|---|---|---|---|
| **Accessibility service** (`BIND_ACCESSIBILITY_SERVICE`, granted by the user enabling it) | Draw the edge handle over every app; collapse the shade when the tile is used | Setup step 4 | No handle. The QS tile opens setup instead |
| `INTERNET` | — | **Never requested** | — |
| `SYSTEM_ALERT_WINDOW` | — | **Not needed** (accessibility overlay) | — |
| `ACCESS_NOTIFICATION_POLICY` (DND) | — | **Not needed**: a step Android refuses just shows the system panel, whose sliders SystemUI is allowed to change | — |
| Foreground service / `POST_NOTIFICATIONS` | — | **Not needed** | — |
| Boot receiver | — | **Not needed**: the system re-binds enabled accessibility services after boot | — |

Showing the system volume panel and stepping volume (`AudioManager.adjustSuggestedStreamVolume`) need **no permission at all**.

## Accessibility service disclosure (shown before step 4)

> **EdgeVol uses Android's Accessibility service only to keep its small volume handle on the screen edge above other apps.**
> It does **not** read, collect or transmit anything on your screen (`canRetrieveWindowContent = false`), has no internet access, and performs only one system action: closing the notification shade when you use the Quick Settings tile.

This text also goes in `@string/a11y_service_description`, which Android shows on the accessibility settings page.

## Privacy summary

- No network permission, so data cannot leave the device.
- No screen-content access.
- Settings are stored locally in DataStore and removed on uninstall.
- No analytics, ads or crash-reporting SDKs.
