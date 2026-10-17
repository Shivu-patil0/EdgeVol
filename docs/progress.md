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

## M1 · Skeleton ⏳

_Not started. Blocked on the Android SDK being installed (see [06](06-build-test-release.md#toolchain))._
