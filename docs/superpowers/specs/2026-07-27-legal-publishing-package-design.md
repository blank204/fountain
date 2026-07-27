# Fountain — Legal & Publishing Package (Design)

**Date:** 2026-07-27
**Status:** Approved
**Scope:** Sub-project 1 of 3 on the path to a Google Play production release.

---

## 0. Context

Fountain is a minimalist Android launcher with per-app time gates. The code is complete
and released as an APK on GitHub (`github.com/blank204/fountain`). The goal is a Google
Play **production** release from a **personal** developer account.

A personal account created after 2023-11-13 must run a closed test with at least **12
testers opted in continuously for 14 days** before it can apply for production access.
Nothing starts that clock until a Play-acceptable AAB with a complete "App content"
section is uploaded. **The legal package is therefore on the critical path, not after it.**

### Decomposition

The work was split into three sequential sub-projects, each with its own spec, plan, and
implementation cycle:

1. **Legal & publishing package** ← *this spec*
2. **targetSdk 34 → 36 migration** (required; Play rejects new apps below API 35 today,
   and requires API 36 from 2026-08-31)
3. **Signed AAB build & first upload**

They are separated because the legal work and the toolchain upgrade share nothing
technically, and a Gradle upgrade fight should not be able to block finishing a privacy
policy.

### What is already correct

These need no work and are load-bearing for every claim below:

- **No `INTERNET` permission and no HTTP library anywhere in the project.** The app is
  physically incapable of transmitting data. This is the single strongest compliance fact
  Fountain has, and it is verifiable by any reviewer who unzips the AAB.
- `accessibility_service_config.xml` declares `canRetrieveWindowContent="false"` and
  `canPerformGestures="false"`, and listens only for `typeWindowStateChanged`.
- Device admin is scoped to `force-lock` only (`res/xml/device_admin.xml`).
- No custom PIN: `BiometricAuth` delegates to `BiometricPrompt` with `DEVICE_CREDENTIAL`,
  so Fountain stores no authentication secrets.
- The release keystore exists and is correctly gitignored, with signing already wired in
  `app/build.gradle.kts`.

---

## 1. Goals & non-goals

### Goals

- Satisfy every Google Play requirement that gates *uploading a release*, so the 14-day
  closed-test clock can start.
- Make every privacy claim Fountain makes literally true, not approximately true.
- License the project explicitly as MIT and make bundled third-party licenses compliant.

### Non-goals

- targetSdk migration (sub-project 2).
- Building or uploading the AAB (sub-project 3).
- Store listing marketing copy, screenshots, feature graphic.
- Monetisation. Fountain is free, with no ads, no IAP, no analytics.
- Any change to Fountain's actual behaviour or features.

---

## 2. Findings this spec responds to

Discovered while reading the codebase. Each maps to a change below.

| # | Finding | Location |
|---|---|---|
| F1 | Onboarding requests overlay permission the app never declares or uses. No `SYSTEM_ALERT_WINDOW` in the manifest; no `TYPE_APPLICATION_OVERLAY` or `WindowManager.addView` anywhere. `GateActivity` is a plain Activity. | `OnboardingScreen.kt:112-118` |
| F2 | Because of F1, `setupIncomplete` can never become false, so the "finish setup" home banner shows **permanently**, even after a user grants everything. | `MainActivity.kt:114-119` |
| F3 | `allowBackup="true"` with no backup rules, while `CapturedNotificationEntity` persists notification `title` and `text` to Room. Notification content therefore syncs to Google Drive — contradicting the in-app claim that no data leaves the device. | `AndroidManifest.xml:18`, `NotificationData.kt:28-36` |
| F4 | Accessibility disclosure does not meet Play policy. One subtitle line, no description of data accessed, no use/sharing statement, no affirmative consent distinct from the grant tap. | `OnboardingScreen.kt:105-111` |
| F5 | `USE_EXACT_ALARM` declared alongside `SCHEDULE_EXACT_ALARM`. Play restricts the former to alarm-clock and calendar apps. | `AndroidManifest.xml:12` |
| F6 | No LICENSE, no privacy policy, no bundled OFL text, no in-app licenses surface. `pixel_pop.ogg` absent from `ATTRIBUTIONS.md`. README deleted in `0e17c06`. | repo root |
| F7 | `proguard-rules.pro` is referenced by `app/build.gradle.kts` but does not exist. Harmless while `isMinifyEnabled = false`; becomes a build failure the moment minification is enabled. | `app/build.gradle.kts:48-51` |

### Asset provenance

`pixel_pop.ogg` carries FFmpeg encoder metadata (`Lavf62.13.101`,
`encoder=Lavc62.29.101 libvorbis`), is mono 11025 Hz / 3.3 KB, and was created 45 minutes
after `ATTRIBUTIONS.md` was written stating the sound would be original and synthesised.
**It is treated as original, self-made work.** If this is ever found to be untrue, the
asset must be replaced rather than re-licensed — the project ships no Toby Fox or
Deltarune assets.

---

## 3. Architecture

The work divides into three isolated units with no shared state:

```
compliance/          NEW — in-app disclosure, consent, licences. The Play-policy surface,
                     in one findable place rather than scattered through onboarding.
docs/                NEW — privacy policy (GitHub Pages) + Console declaration answers.
manifest & data      CHANGED — permission and backup corrections that make the policy true.
```

Each can be built and verified independently.

---

## 4. Component: in-app disclosure & consent

Play requires that a non-accessibility use of the AccessibilityService API carry a Console
declaration **and** an in-app disclosure that describes the data accessed, explains how it
is used and shared, and takes affirmative user consent. The disclosure must appear during
normal app usage — not behind a menu, and not only in the store listing or on a website.

### New file: `compliance/Disclosure.kt`

```kotlin
enum class SensitiveService { ACCESSIBILITY, NOTIFICATION_LISTENER }

data class DisclosureCopy(
    val title: String,
    val whatIsAccessed: String,
    val howItIsUsed: String,
    val howItIsShared: String,
)

@Composable
fun DisclosureGate(
    service: SensitiveService,
    onConsent: () -> Unit,
    onDismiss: () -> Unit,
)
```

Full-screen, rendered in the Fountain palette, with three labelled sections mirroring the
policy's three demands, then **"I understand — continue"** and **"Not now."**

### Flow

The gate is the **only** path to a grant action. In `OnboardingScreen`, the Accessibility
and Notification steps stop calling `AccessibilityUtil.openSettings` and
`NotificationAccessUtil.openSettings` directly. Instead:

```kotlin
var pendingDisclosure by remember { mutableStateOf<SensitiveService?>(null) }
```

When non-null, the gate renders over the onboarding list. Consent navigates to system
settings; dismiss returns. No new `Screen` enum entry — keeping it local holds the
disclosure adjacent to the grant, which is what the policy asks for.

The existing `Step` composable renders its action button only when `!done`, so the grant
action exists *only while the service is off*. "Gate before grant" is therefore
automatically "gate before every enable," with no extra conditional logic.

### Consent record

Two keys added to `SettingsRepository`, with matching `FountainSettings` fields and
setters:

```kotlin
val KEY_CONSENT_ACCESSIBILITY = booleanPreferencesKey("consent_accessibility")
val KEY_CONSENT_NOTIFICATIONS = booleanPreferencesKey("consent_notifications")
```

These do not gate the flow — the gate already does. They are a record, surfaced on the
About screen.

### Copy (compliance-critical)

**Accessibility:**

> **What Fountain reads:** only the package name of the app currently in front. It cannot
> read screen content, text you type, or anything inside your apps — the service is
> declared with `canRetrieveWindowContent="false"`.
>
> **What it's used for:** noticing when a time-gated app opens so Fountain can ask how long
> you want, and sending you home when that time is up.
>
> **Where it goes:** nowhere. It is held in memory and never written anywhere, never
> transmitted, never shared. Fountain has no internet permission and is incapable of
> sending data off your device.

**Notification access** follows the same three-part shape but is stronger, because
notification `title` and `text` *are* persisted: it names that explicitly, states the data
stays in Fountain's local database on this device, and points to Settings → Inbox to clear
it at any time.

---

## 5. Component: About & licences screen

### Navigation

A new `Screen.About` enum entry in `MainActivity`, and an `onAbout` callback plus a hub row
in `SettingsHubScreen`. New file `settings/AboutScreen.kt`.

### Contents

- App name and version, read from `BuildConfig.VERSION_NAME`.
- The full privacy policy text, rendered in-app. Fountain must not depend on a network to
  show its own policy, and an in-app copy is what reviewers prefer.
- A link out to the hosted policy via an `ACTION_VIEW` intent. (Launching a browser needs
  no `INTERNET` permission of our own.)
- Open-source licences, read from bundled assets.
- Consent status for both sensitive services.

### Licence bundling

**OFL 1.1 requires that the licence accompany the software** — meaning inside the
distributed binary, not merely in the GitHub repo. Licence texts are therefore bundled as
assets and read at runtime:

```
app/src/main/assets/licenses/MIT.txt           Fountain itself
app/src/main/assets/licenses/OFL-1.1.txt       Pixelify Sans
app/src/main/assets/licenses/Apache-2.0.txt    AndroidX, Compose, Room, DataStore,
                                               Biometric, Kotlin stdlib
```

Pixelify Sans ships unmodified, so the Reserved Font Name clause is not engaged.

### Build change required

`BuildConfig` is disabled by default in AGP 8. `app/build.gradle.kts` needs:

```kotlin
buildFeatures {
    compose = true
    buildConfig = true   // NEW — required for BuildConfig.VERSION_NAME
}
```

---

## 6. Manifest & data-handling corrections

| Change | File | Rationale |
|---|---|---|
| Remove `USE_EXACT_ALARM` | `AndroidManifest.xml:12` | F5. Play restricts it to alarm-clock/calendar apps; it is auto-granted, so it invites scrutiny for no benefit. `SCHEDULE_EXACT_ALARM` remains and is already handled by the onboarding "Precise timing" step. Accepted cost: users must grant exact alarms manually, and the spec already documents graceful degradation ("kick may be delayed under Doze"). |
| `allowBackup="false"` | `AndroidManifest.xml:18` | F3. Accepted cost: settings do not survive a device migration. In exchange, "nothing leaves your device" becomes literally true. |
| Delete the overlay onboarding step | `OnboardingScreen.kt:112-118` | F1. Requesting unused special access on a high-scrutiny app. |
| Drop `canDrawOverlays` from `setupIncomplete` | `MainActivity.kt:117` | F2. Fixes the permanent setup banner. |
| Add `app/proguard-rules.pro` | new file | F7. Empty file with a header comment; prevents a future build failure when minification is enabled in sub-project 3. |

---

## 7. Documents

### `LICENSE` (repo root)

MIT, 2026. **Copyright holder line:** `Copyright (c) 2026 blank204` — matching the existing
GitHub identity and git author, since no legal name has been given. Swap for a legal name
if one is preferred; this is the only place it appears.

MIT was chosen over GPL-3.0 and Apache-2.0 with the trade-off understood: a permissive
licence lets anyone reskin Fountain, add an ad SDK, and republish it closed-source on Play.
That is accepted.

### `PRIVACY.md` → published at `blank204.github.io/fountain/privacy`

Hosted via GitHub Pages from `docs/` on `main` — free, versioned, and a stable URL. Content:

1. **The one-sentence version:** Fountain collects nothing, transmits nothing, and has no
   internet permission.
2. What is stored on the device: gated-app list, session history, notification rules,
   captured notification title/text, preferences.
3. What each sensitive permission accesses and why, mirroring the in-app disclosures.
4. That there is no analytics, no advertising, no third-party SDK, and no account.
5. How to delete data: clear inbox in-app, or uninstall.
6. Children's policy: not directed at children under 13.
7. Last-updated date, and a contact address.

**Contact address:** `gshriadhithya@gmail.com`. Play Console requires a publicly visible
support email on the store listing regardless, so this address becomes public either way —
using it in the policy adds no further exposure. Substitute a dedicated alias if preferred;
it appears in exactly two places (the policy and the Console listing).

### `ATTRIBUTIONS.md` (update)

Add the `pixel_pop.ogg` row as original self-made work; add the Apache-2.0 dependency
block; record that OFL text is now bundled, closing the file's own outstanding note.

### `README.md` (restore)

Deleted in `0e17c06`. Restore with description, MIT badge, privacy policy link, build
instructions, and a DCO sign-off note for contributions.

### `docs/play-console-declarations.md`

Not a Play artefact — a working document holding the exact answers to paste into the
Console, so they are version-controlled and consistent across resubmissions:

- **Data safety:** no data collected, no data shared. Play defines collection as
  transmission off-device; with no `INTERNET` permission this is unambiguous.
- **Accessibility declaration:** `isAccessibilityTool=false`, with the justification and a
  pointer to the in-app disclosure.
- **`QUERY_ALL_PACKAGES`:** justified — a launcher requires the full launchable-app list.
- **`SCHEDULE_EXACT_ALARM`:** justified — a session must end at its exact deadline even
  under Doze, or the core feature silently fails.
- **`specialUse` foreground service:** justification text. No predefined FGS type fits a
  user-initiated countdown timer, and `shortService` caps at three minutes while sessions
  run 5–20. **This is the highest-risk declaration; Google reviews it manually and may
  push back.**
- **Device admin:** `force-lock` only, for the optional double-tap-to-lock gesture.
- **Content rating:** questionnaire answers. No objectionable content.
- **Target audience:** 13+. Not designed for families.
- **App access:** no login required; step-by-step instructions for a reviewer to enable
  Accessibility and Notification access, since neither can be granted programmatically and
  a reviewer who skips them will see a non-functional app.

---

## 8. Error handling & edge cases

- **User declines a disclosure.** No navigation, no consent recorded, service stays off.
  Fountain already degrades per-permission with specific messaging, so this is an existing,
  supported state.
- **User revokes Accessibility later.** The step returns to `!done`, the action button
  reappears, and the gate shows again on the next enable. Correct by construction.
- **Consent recorded but service off.** Possible if the user consents then abandons the
  system settings screen. Harmless — the gate shows again next time.
- **Missing licence asset at runtime.** The About screen renders a short fallback rather
  than crashing. A missing licence file must never take down the app.
- **`allowBackup="false"` on existing installs.** Users upgrading from the GitHub APK keep
  their local data; only cloud backup stops. No migration needed.

---

## 9. Testing & verification

No unit tests are proposed. This sub-project changes documents, one screen, one dialog, and
five manifest/config lines; the meaningful verification is observational and manual.

1. `./gradlew assembleDebug` succeeds.
2. Fresh install: onboarding shows the accessibility disclosure **before** any system
   settings screen opens, with all three sections and both buttons.
3. "Not now" leaves the service off and returns to onboarding.
4. Consent then grant: the step flips to done.
5. **F2 regression check:** with every permission granted, the home "finish setup" banner
   is gone. This was impossible before.
6. The overlay step no longer appears in onboarding.
7. Settings → About renders version, policy text, and all three licence texts.
8. `unzip -l app-release.aab | grep -i license` shows the bundled licence assets.
9. `aapt dump permissions` (or reading the merged manifest) confirms no `USE_EXACT_ALARM`
   and no `SYSTEM_ALERT_WINDOW`.
10. GitHub Pages serves the policy at the expected URL.

**Device note:** all of the above is verifiable on the Note 9 (Android 10). This
sub-project needs no emulator. Sub-project 2 does, because the API 35/36 behaviour changes
only manifest on Android 15/16 devices — which is exactly why the migration was separated.

---

## 10. Sequencing

```
[this spec] legal package
        ↓
[spec 2]   targetSdk 34 → 36  (AGP + Gradle upgrade, edge-to-edge, Android 16 emulator)
        ↓
[spec 3]   signed AAB + Play App Signing enrolment + first upload
        ↓
           closed test — 12 testers, 14 continuous days
        ↓
           apply for production access
```

**Carried forward to sub-project 3:** at first AAB upload, enrol in Play App Signing using
the **existing `keystore/fountain-release.jks`** as the app signing key. If Google generates
a fresh key instead, the signature will not match the GitHub APK, and every existing user
must uninstall — losing their gated-app configuration and captured notifications — before
they can install from Play. This choice is effectively permanent.

**Also carried forward:** `versionName` is currently `0.1.0`. Sub-project 3 should set a
release version.
