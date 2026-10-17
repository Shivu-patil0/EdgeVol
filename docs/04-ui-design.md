# 04 · UI design

> Principles: **invisible until called · behaves exactly like the hardware volume keys · Android's own volume panel · zero clutter.**

![Interaction mockup](images/panel-mockup.svg)

EdgeVol draws only two things: the **edge handle** and the **setup screen**. All volume UI comes from **Android's built-in volume panel** (SystemUI), so it looks and works like the phone's normal panel on Pixel, Samsung, Xiaomi and others.

## Edge handle: a virtual volume rocker

![Handle states](images/handle-states.svg)

| Gesture | Result | Feedback |
|---|---|---|
| **Tap** (or short inward swipe) | Shows the system volume panel without changing anything | haptic `CONTEXT_CLICK`, handle flashes |
| **Drag up / down along the handle** | One volume step per **24 dp** of travel, like pressing Volume + / − | haptic tick per step; system panel shows the level |
| **Long-press 500 ms, then drag** | Moves the handle along the edge; position saved | handle turns solid while moving |

| Property | Default | Configurable |
|---|---|---|
| Side | Right | Left / Right |
| Vertical position | 40% of screen height | Long-press + drag on the handle, or a slider in settings |
| Visual size | 4 dp × 72 dp pill | S 56 dp / **M 72 dp** / L 120 dp |
| Touch area | 24 dp wide (invisible) | — |
| Opacity | 35% | 10–80% (0% = "QS tile only" mode) |
| Color | `primary` from the theme (dynamic color on Android 12+) | — |

**Back-gesture conflict (gesture navigation):** Android's *back* is a **horizontal** inward swipe. Our main gesture, the volume drag, is **vertical**, and a tap never triggers back. We also register the handle's bounds with `setSystemGestureExclusionRects` (capped at 200 dp per edge; the largest handle is 120 dp).

## The system volume panel (what the user sees after a gesture)

- **Look, position and timeout** are controlled by the phone's SystemUI. On Pixel it's a vertical pill on the right; Samsung and Xiaomi have their own styles. EdgeVol can't restyle or move it, by design.
- **Which stream** a drag changes is decided by Android exactly as for the hardware keys: call during a call, media while playing, otherwise the device default.
- **Other streams** (ring, notification, alarm): the user taps the panel's own expand / ⋯ button. Android has no public API to open the expanded view directly.
- **Mute / ringer mode:** use the panel's own mute and ringer icons.
- **First step with nothing playing:** on some devices the first key press only reveals the panel; the next one changes the level. EdgeVol inherits this behavior.

## Motion & haptics

| Event | Motion | Haptic |
|---|---|---|
| Touch down | Handle widens 4 → 8 dp, opacity → 100%, 120 ms | — |
| Tap → panel | (system panel animates itself) | `CONTEXT_CLICK` |
| Each volume step | — | `SEGMENT_TICK` (API 34+), else `CLOCK_TICK` |
| Reposition start | Handle turns solid | `LONG_PRESS` |
| Release | Handle returns to idle size and opacity, 160 ms | — |

## Theming (setup screen + handle)

- **Material 3**, `dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12+, falling back to the brand palette:

| Token | Light | Dark |
|---|---|---|
| Brand primary | `#4F46E5` (indigo) | `#A5B4FC` |
| Brand secondary | `#7C3AED` (violet) | `#C4B5FD` |

- The setup screen is edge-to-edge (Android 15+ enforces it). See the repo skill `edge-to-edge`.

## Accessibility of our own UI

- The handle exposes a content description ("EdgeVol volume handle") and **custom accessibility actions**: *Show volume panel*, *Volume up*, *Volume down*. TalkBack and Switch Access users don't need the drag gesture.
- Setup screen: content descriptions, ≥ 48 dp targets, font scaling, and *Remove animations* respected.

## Setup / settings screen

| Section | Content |
|---|---|
| Status | Accessibility service: On ✓ / *Enable* · Restricted settings help (if needed) · *Add Quick Settings tile* |
| Handle | Side · Position (10–90%) · Size (S/M/L) · Opacity (0–80%) with a live preview on the screen edge |
| Try it | *Show volume panel* button (calls `SystemVolume.showPanel()`) |

## Logo

| | |
|---|---|
| <img src="images/logo.svg" width="96"/> | **Temporary logo.** Indigo→violet rounded square, three white volume pills at different levels, and the edge-handle sliver on the right. M4 converts it into an adaptive icon (`ic_launcher_foreground` vector + gradient background) and uses [`logo-mono.svg`](images/logo-mono.svg) for the QS tile icon. |
