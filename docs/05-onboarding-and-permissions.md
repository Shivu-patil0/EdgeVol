# 05 · Onboarding & permissions

![Onboarding flow](images/onboarding-flow.svg)

## First-run flow

1. **Install the APK.** `adb install app-release.apk` is preferred (usually avoids step 3), or open the `.apk` on the phone.
2. **Open EdgeVol.** The setup screen shows live status that updates in `onResume`.
3. **Allow restricted settings** *(only if the Accessibility toggle is greyed out; Android 13+ sideloads).* Settings → Apps → EdgeVol → **⋮** → *Allow restricted settings*. The setup screen offers an *Open app info* button (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`).
4. **Enable the service.** The *Enable* button deep-links to `Settings.ACTION_ACCESSIBILITY_SETTINGS`. Once it's on, the edge handle appears immediately.
5. **Add the QS tile** *(optional).* On Android 13+ a one-tap system prompt via `StatusBarManager.requestAddTileService()`; on older versions, short instructions to edit the Quick Settings tiles.

✅ **Ready:** tap the handle to see the phone's own volume panel, in any app. Touch and hold it, then drag, to park it on the left or right edge (remembered across restarts).

### Service enabled but not running (MIUI and similar)

When the app process is killed (e.g. the user clears EdgeVol from Recents on MIUI), Android lists the service under *Crashed services* and **does not re-bind it** until the user switches it off and on again, although the Accessibility setting still reads "on". The setup screen detects this (`SetupStatus.serviceRunning == false` while `serviceEnabled`) and shows the *Enabled but not running* state: a plain explanation, a button to Accessibility settings, and a hint. Recovery: Settings → Accessibility → Downloaded apps → EdgeVol → switch **off**, then **on**. On the verified phone the first off/on cycle after a crash sometimes still fails to draw the handle (second cycle works), so the screen says "Still not running after that? Switch it off and on once more."

To prevent it on Xiaomi/MIUI-type phones: **lock EdgeVol in Recents** (long-press its card → lock), allow **Autostart**, and set Battery saver to **No restrictions** for EdgeVol. A force-stop from App info also switches the service off in Settings (the screen then shows *Off*).

## Permissions

| Permission / capability | Why | When requested | If denied |
|---|---|---|---|
| **Accessibility service** (`BIND_ACCESSIBILITY_SERVICE`, granted by the user enabling it) | Draw the edge handle over every app; collapse the shade when the tile is used (M3) | Setup step 4 | No handle. The QS tile (M3) opens setup instead |
| `INTERNET` | — | **Never requested** | — |
| `SYSTEM_ALERT_WINDOW` | — | **Not needed** (accessibility overlay) | — |
| `ACCESS_NOTIFICATION_POLICY` (DND) | — | **Not needed**: EdgeVol never changes the volume itself, it only shows the system panel, whose sliders SystemUI is allowed to change | — |
| Foreground service / `POST_NOTIFICATIONS` | — | **Not needed** | — |
| Boot receiver | — | **Not needed**: the system re-binds enabled accessibility services after boot | — |

Showing the system volume panel (`AudioManager.adjustSuggestedStreamVolume` with `ADJUST_SAME` + `FLAG_SHOW_UI`) needs **no permission at all**.

## Accessibility service disclosure (shown before step 4)

> **EdgeVol uses Android's Accessibility service only to keep its small volume handle on the screen edge, above other apps.**
> It does not read, collect or send anything on your screen, and it has no internet access. The handle only asks your phone to show its volume panel.

The code-level facts (`canRetrieveWindowContent = false`, no `INTERNET` permission) are enforced by tests, not stated in user-facing text. The Quick Settings tile sentence ("closing the notification shade when you use the Quick Settings tile") is added in M3 together with the tile.

The shipped `@string/a11y_service_description` (shown by Android on the accessibility settings page) and the in-app disclosure (`setup_disclosure_body`) use this exact text (M2.1; it said "show or change the volume" until the drag-volume gesture was removed):

> EdgeVol uses Android's Accessibility service only to keep its small volume handle on the screen edge, above other apps. It does not read, collect or send anything on your screen, and it has no internet access. The handle only asks your phone to show its volume panel.

## Privacy summary

- No network permission, so data cannot leave the device.
- No screen-content access.
- Settings (handle side and position; later size and opacity) are stored locally in DataStore and removed on uninstall.
- No analytics, ads or crash-reporting SDKs.
