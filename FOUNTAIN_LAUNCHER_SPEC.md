# Fountain — Minimalist Android Launcher (Build Spec)

> Working codename: **Fountain**. Change freely — nothing in the code should hardcode the display name in more than one place (`strings.xml`).

A pixel-styled, distraction-killing Android launcher. Grayscale home screen, per-app time gates that force-kick you out when time expires, a launcher-level lock overlay, and a custom notification inbox — all wrapped in a Deltarune-inspired purple pixel-fountain aesthetic.

**Platform:** Android only. **Test device:** Samsung Galaxy Note 9, Android 10 (API 29). **Intended for eventual commercial release** — so code cleanly, keep assets license-safe, and target modern SDKs.

---

## 0.0 Revision notes (v2 — feasibility & UX pass)

This spec was reviewed against the realities of the target device (Note 9 / One UI / Android 10) and the newer target SDK. Changes from v1, all folded into the sections below:

- **Service survival is the #1 risk, now designed for.** One UI aggressively sleeps background components. Session timing is owned by a **persistent foreground service**, the kill is scheduled via **`AlarmManager` exact alarm against a persisted absolute end-time** (survives process death), and onboarding includes a **Samsung battery-optimization / "Never sleeping apps"** step. See 2.3, 4, 6.
- **"Intercept before foreground" corrected** to "detect on foreground, immediately cover with the gate overlay" — Android can't show UI before the app appears; accept a ~1-frame leak. See 2.3.
- **Fountain scoped to the lock overlay only.** Notification inbox uses a **static** pixel-fountain motif, not per-row animation (lighter, calmer, on-thesis). See 2.5, 2.6.
- **Optional reopen breathing screen** added: a short delay before re-gating *only* when the user reopens an app within N seconds of being kicked. Off by default. First-open remains instant-on-confirm. See 2.3.
- **Notification "suppress" documented as best-effort** — a listener cancels *after* post, so it can't stop the origin app's initial heads-up/sound. See 2.5.
- **Lock trigger model made explicit:** fires on every return to home, plus optional idle-while-on-home timeout — it cannot lock over other apps. See 2.4.
- **Compose-in-WindowManager complexity flagged** (overlays aren't Activities). See 2.4/2.3 note.
- **Newer-targetSDK work called out:** FGS type, `POST_NOTIFICATIONS`, exact-alarm permission, predictive back. See 4, 5.

---

## 0. Hard Android constraints (read first)

These shape the design. Do not spec around them.

1. **No real lockscreen replacement.** Android does not permit replacing the secure keyguard. "Lock screen" here = a **launcher-level lock overlay** (a full-screen Compose surface shown on the home surface after an idle timeout / on home entry), unlocked by swipe or PIN. It is *not* OS-level security.
2. **No force-killing other apps' processes** without device-owner or root. The "force kick" is implemented via an **AccessibilityService** that detects the foreground app and issues `GLOBAL_ACTION_HOME` when the session timer hits zero. Functionally a kick; technically a redirect.
3. **Cannot restyle other apps' native notifications.** Instead, a **`NotificationListenerService`** captures posted notifications, optionally suppresses the noisy ones, and re-renders the kept ones inside our own pixel-styled inbox + emits our own pixel sound.
4. **System-wide grayscale** needs a special secured permission (`SECURE_SETTINGS`, granted via ADB) — **out of scope**. We render the **launcher UI itself** monochrome via Compose color filters. No system permission needed.
5. **Copyright:** ship **no** Toby Fox / Deltarune assets. Use an open-licensed pixel font and an **original** synthesized sound effect styled to feel similar.

---

## 1. Tech stack & targets

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material3 as base, heavily restyled)
- **IDE/build:** Android Studio, Gradle (Kotlin DSL)
- **minSdk:** 29 (Android 10 — for the Note 9)
- **targetSdk / compileSdk:** latest stable (35+) — for Play Store / commercial readiness
- **Architecture:** MVVM. `ViewModel` + `StateFlow`, unidirectional data flow.
- **Persistence:** **DataStore (Proto or Preferences)** for settings; **Room** for gated-app list, session logs, and captured-notification history. All local. No accounts, no network, no cloud, no analytics SDKs.
- **DI:** Hilt (keeps modules clean for a commercial codebase). Optional but recommended.
- **Min dependencies.** Avoid heavy libs. No ad SDKs.

---

## 2. Core features

### 2.1 Launcher home
- Registered as a **launcher** (`<category android:name="android.intent.category.HOME"/>` + `DEFAULT`).
- **A–Z scrollable app list** of all installed launchable apps (query `PackageManager` / `LauncherApps`).
- **Search bar** at top — filters the list live as you type.
- Fast-scroll letter index on the right edge (nice-to-have, second pass).
- **Launcher UI is monochrome:** apply saturation-0 color filter to icons and use a grayscale palette for all home-surface chrome. (Lock overlay and notification views are exempt — they render in color.)
- Tapping an app: if it's **gated**, route through the time-gate flow (2.3); otherwise launch normally.
- Long-press an app → context menu: App info, Add/remove from gated list, Hide.
- Handle app install/uninstall broadcasts to keep the list fresh.

### 2.2 Grayscale / theme rendering
- Global monochrome applied to the **launcher home surface only**.
- Do it with a `ColorMatrix` (saturation 0) `ColorFilter` on icon rendering + a grayscale color scheme — not a screenshot overlay.

### 2.3 App time-gate (the core mechanic)
- User designates any installed app as **gated** (Settings → Gated apps → multi-select installed apps). No hardcoded list.
- **Flow when a gated app is opened** (via our launcher OR from anywhere, caught by the AccessibilityService):
  1. **Detect on foreground, then immediately cover** the app with our **time-request overlay** (a `SYSTEM_ALERT_WINDOW` Compose surface). *Note: Android cannot show UI **before** the app appears — there is a ~1-frame window where the app is visible behind the overlay. This is expected. From our own launcher we control the tap and can gate cleanly; from anywhere else it is always the detect-then-cover path.*
  2. Present presets: **5 / 10 / 15 / 20 minutes**.
  3. User taps a preset → **confirm** → overlay dismisses, app is usable immediately. *(No breathing delay on first open — open on confirm.)*
  4. A session timer starts for the chosen duration, scoped to that package. **Timing is owned by the foreground service and persisted as an absolute end-time (see below), not an in-memory countdown.**
  5. **On expiry: force kick** — AccessibilityService fires `GLOBAL_ACTION_HOME`. No warning screen, no fade. Straight kick.
- **Timer must survive process death (Note 9 will kill us).** Persist `{package, sessionStart, durationMs, endTimeEpochMs}` to Room the moment a session begins, and schedule the kick with `AlarmManager.setExactAndAllowWhileIdle` on the absolute `endTime`. On boot / service restart, reconcile: any session whose `endTime` has passed → kicked/closed; any still-active → re-arm its alarm. Never rely on a coroutine `delay()` alone for the kill.
- **Optional reopen breathing screen (default OFF).** When enabled, reopening the *same* package within **N seconds** (default 10s) of a kick shows a short breathing/hold screen before re-presenting the presets. First opens and normal reopens stay instant. This is the one intentional friction point; it targets the "kicked → instantly reopen → pick 5 min again forever" loop.
- **No daily/overall cap.** Every fresh open re-triggers the gate; limits are per-open only.
- **No override / unlock-early bypass** — there's nothing to bypass since there's no cumulative limit; each session is just its own countdown.
- Session rows in Room double as the future stats source (stats screen not required in v1 UI).
- Edge cases to handle: switching between two gated apps, backgrounding then reopening within an active session (reuse remaining time, don't re-prompt), screen-off during a session (pause or continue — **default: continue counting**, which the absolute-end-time model gives you for free), our own launcher and system UI must never be gated.

### 2.4 Lock overlay ("lock screen")
- Full-screen Compose surface on the home surface.
- **Trigger model (be explicit — a launcher can't watch system-wide idle):**
  - Fires **on every return to the home surface** (primary trigger).
  - Plus an **optional idle-while-on-home timeout** (default 60s) — locks if home has been sitting idle. Configurable.
  - It **cannot** lock over other apps. "Idle timeout" means idle *on our home surface*, not device-wide.
- **Shows:** big pixel **clock (time)**, **date**, **top notifications** (from our captured inbox), and the animated **purple pixel fountain** (the fountain's one animated home — see 2.6).
- **Renders in color** (fountain is purple) — exempt from launcher monochrome.
- **Unlock:** swipe up to dismiss; **optional PIN** (user can enable). Swipe is default.
- This is cosmetic/focus, not security — document that clearly in-app.
- **Impl note:** the overlay is a `WindowManager` view, not an Activity. Compose there requires manually attaching `ViewTreeLifecycleOwner`, `ViewTreeSavedStateRegistryOwner`, and `ViewTreeViewModelStoreOwner` or it will crash. Same applies to the 2.3 time-gate overlay. Budget for this.

### 2.5 Custom notifications
- **`NotificationListenerService`** captures posted notifications (requires the user to grant Notification Access in system settings — guide them there on first run).
- User chooses, per app, whether notifications are: **shown in our inbox**, **suppressed/hidden**, or **passed through** untouched.
- **"Suppress" is best-effort and slightly late.** A listener can only `cancelNotification()` *after* the notification posts, so it cannot prevent the origin app's initial heads-up / sound / vibration that fired in the same instant. "Suppressed" = "removed a beat later," not "never appeared." Document this so expectations are correct.
- Kept notifications render in a **pixel-styled inbox** (list + detail), using the **pixel font** (font is scoped to notification views + optionally the lock clock).
- **Inbox visuals:** a **static** pixel-fountain motif only (header / empty-state) — **no per-row animation.** The animated fountain lives on the lock overlay (2.6); animating it inside every notification row is heavy and fights the calm/focus ethos.
- **Notification sound:** play an **original synthesized pixel "pop"** (styled like the Deltarune "USE" blip — *original asset, not the real one*). Short, punchy, ~8-bit. Bundle as a small `.wav`/`.ogg` in `res/raw`. Respect system Do-Not-Disturb / silent.
- Top N unread also surface on the lock overlay.

### 2.6 The pixel fountain (aesthetic centerpiece)
- **Pixel-based, PURPLE, animated.** The animated fountain appears on the **lock overlay only.** The notification inbox uses a **static** pixel-fountain motif (2.5) — not this live animation.
- Style reference: a flowing/glitchy pixel column — particles/pixels rising and cascading like a fountain (Deltarune Dark Fountain / "ultracode"-style flowing pixel energy). Purple gradient (deep violet → magenta highlights) against near-black.
- Implement as a Compose `Canvas` particle system OR a low-res pixel-grid animation upscaled with **nearest-neighbor** (no smoothing — keep hard pixel edges).
- **Battery-conscious:** cap frame rate (~15–24fps), pause animation when screen is off / surface not visible, and don't animate on the monochrome home grid.
- **Extras (toggleable in Settings, default on):** CRT scanline overlay + subtle flicker/vignette.

---

## 3. Aesthetic / design tokens

- **Palette (Dark Fountain):**
  - Background: near-black `#0B0B10`
  - Primary purple: `#7A3CFF` → fountain deep `#3A1E6E`, highlight magenta `#C74BFF`
  - Fountain glow/cyan accent (sparingly): `#4DE0E0`
  - Monochrome home: pure grayscale ramp (`#000` → `#FFF`)
- **Font:** open-licensed pixel/bitmap font — e.g. **Pixelify Sans** (SIL OFL) or **m5x7** (free). Bundle in `res/font`. **Scope: notification views + lock clock/date only.** Launcher list stays in a clean legible system/mono font for readability.
- **Sound:** one original pixel-pop `.ogg` in `res/raw`.
- Hard pixel edges everywhere — nearest-neighbor scaling, no anti-aliasing on pixel art elements.

---

## 4. Permissions & special access (must guide user through each)

| Purpose | Mechanism | Grant path |
|---|---|---|
| Be the launcher | HOME intent filter | User picks Fountain as default launcher |
| Detect foreground app + force-kick | `AccessibilityService` | Settings → Accessibility → enable Fountain |
| Capture/suppress notifications | `NotificationListenerService` | Settings → Notification access |
| List/launch apps | `QUERY_ALL_PACKAGES` (+ `LauncherApps`) | Manifest (note: Play Console requires justification for this) |
| Time-gate + lock overlay above other apps | `SYSTEM_ALERT_WINDOW` | Draw-over-apps toggle |
| Fire the kill on time even in Doze | `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Manifest + (31+) user-grantable exact-alarm access |
| Show our own inbox + foreground-service notifications | `POST_NOTIFICATIONS` | Runtime prompt (Android 13+) |
| Keep session-timer service alive | Foreground service (+ `FOREGROUND_SERVICE` and, on 14+, a declared **FGS type**) | Manifest |
| **Survive One UI battery sleep (critical on the Note 9)** | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` **+ Samsung "Never sleeping apps"** | Onboarding deep-link to battery settings |

Build a **first-run onboarding flow** that walks the user through granting each, one at a time, explaining why, with a "you can revoke anytime" note. Requirements:
- **Order steps by importance**, and put the **Samsung battery-optimization / never-sleeping** step high — it's the highest real-world drop-off and, if skipped, silently breaks force-kick and notification capture when the OS sleeps the app.
- **Show live per-permission status** (granted / missing) so onboarding is resumable and re-checkable, not a one-shot wizard.
- Leave a **persistent home banner** whenever something required is missing (e.g. "Accessibility is off — Fountain can prompt for time but can't kick you out").
- Degrade gracefully per missing permission with specific messaging (no accessibility → prompt-but-no-kick; no notification access → no inbox; no exact-alarm → kick may be delayed under Doze).

---

## 5. Module / package structure (suggested)

```
com.fountain.launcher
├── app/                 // Application, Hilt setup, MainActivity (Compose host)
├── home/                // launcher grid, A–Z list, search, monochrome rendering
├── gate/                // time-request UI, session manager, presets
├── accessibility/       // FountainAccessibilityService (foreground detect + kick)
├── lock/                // lock overlay composable + idle-timeout controller
├── notifications/       // NotificationListenerService, inbox, pixel sound
├── fountain/            // pixel fountain particle engine (Canvas), CRT overlay
├── settings/            // gated-app picker, toggles, PIN, onboarding
├── data/                // Room (gated apps, sessions, notif history), DataStore
├── ui/theme/            // palette, pixel font, tokens, monochrome color filters
└── common/              // utils, package helpers, extensions
```

---

## 6. Build order (iterate slowly, verify each on-device)

Do these as sequential, individually-runnable milestones. **Confirm each builds & installs on the Note 9 before moving on.**

1. **Scaffold** — new project, Compose, Hilt, DataStore, Room deps, theme tokens, pixel font wired in. App runs, shows a placeholder home.
2. **Launcher core** — register as HOME, list all apps A–Z, search, launch on tap, monochrome rendering. Long-press menu.
3. **Gated-app settings** — pick which apps are gated; persist to Room.
4. **Time-gate UI + session logic** — presets 5/10/15/20, confirm-to-open, **persist absolute end-time to Room**, **foreground service** owns timing, **`AlarmManager` exact-alarm** scheduled for the kill, reconcile-on-restart. (Kick action not wired yet — just log/close on the alarm.) Optional reopen-breathing-screen toggle (default off).
5. **AccessibilityService** — foreground detection; **detect-then-cover** gated-app opens with the gate overlay; wire **force-kick via `GLOBAL_ACTION_HOME`** driven by the exact alarm + foreground check. Onboarding for the permission **and the Samsung battery-optimization step** (without it, this milestone silently fails after the OS sleeps the app — test by leaving the phone idle).
6. **NotificationListenerService** — capture, per-app show/suppress/passthrough (document suppress = post-then-cancel), pixel inbox UI with **static** fountain motif, original pixel-pop sound. Onboarding for the permission.
7. **Pixel fountain engine** — purple particle/pixel fountain Canvas, nearest-neighbor, fps cap, pause-when-hidden. CRT scanline overlay toggle. (Lock overlay only.)
8. **Lock overlay** — clock + date + top notifications + fountain; **on-return-to-home + optional idle-on-home trigger**; swipe unlock; optional PIN. (Handle the WindowManager/Compose owner wiring here and in milestone 5's overlay.)
9. **Onboarding flow** — chain all permission grants with explanations, ordered by importance, live status, resumable, persistent missing-permission banner; graceful degradation.
10. **Polish pass** — battery profiling on the animation, edge cases (app switching, screen-off, uninstall, **process death mid-session → alarm still fires**), settings screen cleanup, empty/error states.

---

## 7. Commercial-release notes (keep clean from day one)

- **Assets:** only ship license-safe fonts/sounds. Keep a short `ATTRIBUTIONS.md` with each asset's license (OFL / CC0 / self-made).
- **`QUERY_ALL_PACKAGES`** requires a Play Console declaration — justified here because a launcher legitimately needs the full app list. Note it.
- **Accessibility + Notification access** apps get extra Play review scrutiny. Write clear in-app explanations and a privacy policy stating **no data leaves the device**.
- Keep everything local; no telemetry. That's both the product ethos and the easiest compliance path.
- Don't market it using Deltarune/Toby Fox names or trademarks.

---

## 8. Open questions to resolve during the build (Claude Code can ask as it goes)
- Idle-on-home timeout default (start at 60s, make it a setting).
- Reopen-breathing-screen: window length `N` (default 10s) and hold duration, when the toggle is enabled.
- Whether the pixel font also styles the lock clock (spec default: yes, it's thematic).
- Exact fountain particle behavior — Claude Code should build a first version, show it, then tune with you.

**Resolved in v2 (do not re-litigate):**
- Fountain animates on the **lock overlay only**; inbox uses a static motif.
- Screen-off during a session **keeps counting** (absolute end-time model).
- Reopen friction = **optional breathing screen, default off**; first opens stay instant-on-confirm.
- Home stays the **A–Z list + search** as originally spec'd.
- "Force kick" timing is driven by a **persisted end-time + exact alarm**, not an in-memory timer.

---

## 9. PROMPT FOR CLAUDE CODE

> Paste the block below into Claude Code (Opus 4.8, high effort). It has the full spec file alongside it, so this is the driving instruction.

---

**You are building "Fountain," a native Android minimalist launcher, from scratch. Read `FOUNTAIN_LAUNCHER_SPEC.md` in full before writing any code — it is the source of truth for every feature, constraint, and design token.**

Build target: Kotlin + Jetpack Compose, minSdk 29 (Android 10, my test device is a Samsung Galaxy Note 9), compileSdk/targetSdk latest stable. MVVM, Hilt, Room, DataStore. Everything local — no accounts, no network, no analytics, no ad SDKs. This may go to the Play Store commercially, so write clean, modular, production-quality code and keep all bundled assets license-safe (open-licensed pixel font, an original synthesized pixel sound — never any Deltarune/Toby Fox assets).

Work in the **milestone order defined in section 6 of the spec**. Go slowly and iterate: implement one milestone, make sure it compiles and would install/run cleanly, briefly tell me what to verify on-device, then continue to the next. Do not try to write the entire app in a single undifferentiated dump — build it up in verifiable layers, but keep momentum and complete the whole thing across the session.

Non-negotiable constraints from the spec (do not design around them):
- The "lock screen" is a **launcher-level Compose overlay**, not a real keyguard replacement.
- "Force kick" = an **AccessibilityService** issuing `GLOBAL_ACTION_HOME`, **triggered by a persisted absolute end-time + `AlarmManager` exact alarm** owned by a **foreground service** (survives process death — no in-memory-only countdown, no process killing).
- Time-gate opens are **detected on foreground and immediately covered** by the gate overlay (`SYSTEM_ALERT_WINDOW`) — you cannot intercept *before* the app appears; a ~1-frame leak is expected.
- Custom notifications = a **NotificationListenerService** capturing/suppressing/re-rendering into our own pixel inbox; we cannot restyle other apps' native notifications, and **suppress is post-then-cancel (best-effort, slightly late)**.
- **Monochrome applies only to the launcher home surface.** The lock overlay and notification views render in color — the pixel fountain must be **PURPLE**.
- Time-gate: presets **5/10/15/20 min**, open the app immediately on confirm (no breathing delay on first open), **force-kick on expiry**, **no daily/overall cap**, no override. Optional reopen breathing screen (default off) only within N s of a kick.
- Pixel fountain is animated, purple, pixel-based (nearest-neighbor, hard edges), lives on the **lock overlay only** (inbox uses a **static** motif), fps-capped and paused when not visible. CRT scanline overlay as a toggle (default on).
- Pixel font scoped to notification views (and the lock clock/date).
- Original pixel-pop notification sound in `res/raw`.
- Assume **One UI will sleep the app**: foreground service + exact alarm + a Samsung battery-optimization onboarding step are part of the design, not optional polish.

For each milestone, produce the actual files (Gradle config, manifest entries, services, composables, ViewModels, data layer), wire the necessary permissions, and build a first-run onboarding flow that walks me through granting launcher-default, accessibility, and notification access with clear explanations and graceful degradation when a permission is missing.

When a design detail is genuinely ambiguous (e.g. exact fountain particle behavior, idle-timeout default), pick a sensible default per the spec, implement it, and flag it so I can tune it with you — don't stall waiting for me.

Start now with Milestone 1 (scaffold): set up the project, dependencies, theme tokens, palette, and pixel font, and get a running app showing a placeholder home surface. Then pause and tell me what to check.

---
