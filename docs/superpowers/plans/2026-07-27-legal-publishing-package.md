# Legal & Publishing Package Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Fountain acceptable for upload to Google Play by adding the legally required disclosures, licences, and privacy policy, and by correcting the permission and backup settings that currently make Fountain's privacy claims untrue.

**Architecture:** Three isolated units with no shared state. A new `compliance/` Kotlin package holds the in-app disclosure surface (a pure-Kotlin copy model plus one full-screen composable). A new `docs/` tree holds the published privacy policy and the Play Console declaration answers. A handful of manifest, onboarding, and build-config edits bring the app's actual permission surface in line with what it claims.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), DataStore Preferences, Gradle Kotlin DSL, JUnit 4 (new — no test infrastructure exists yet).

**Source spec:** `docs/superpowers/specs/2026-07-27-legal-publishing-package-design.md`

## Global Constraints

- **Do not change Fountain's behaviour or features.** This plan touches paperwork, one dialog, one screen, and five config lines. Nothing else.
- **Do not add dependencies** beyond `junit:junit:4.13.2` in Task 4. No network libraries, ever — Fountain having no `INTERNET` permission is the foundation of every claim in this plan.
- **Do not bump `compileSdk`, `targetSdk`, AGP, Gradle, or Kotlin.** That is sub-project 2 and is deliberately out of scope here.
- **Do not change `versionCode` or `versionName`.** That is sub-project 3.
- Licence texts must be bundled **inside the app** (`app/src/main/assets/licenses/`), not only in the repo. SIL OFL 1.1 requires the licence accompany the distributed binary.
- Copyright holder string, used verbatim wherever a copyright line is needed: `Copyright (c) 2026 blank204`
- Public contact address, used verbatim: `gshriadhithya@gmail.com`
- Published policy URL, used verbatim: `https://blank204.github.io/fountain/privacy`
- Repository URL, used verbatim: `https://github.com/blank204/fountain`
- Every commit message ends with the two trailer lines shown in Task 1, Step 5.
- Run all Gradle commands from the repo root with `./gradlew` (Git Bash).

---

### Task 1: Repository legal documents

Adds the paperwork the repo itself needs: an explicit licence, an honest attributions file, a restored README, and the missing ProGuard file referenced by the build.

**Files:**
- Create: `LICENSE`
- Create: `README.md`
- Create: `app/proguard-rules.pro`
- Modify: `ATTRIBUTIONS.md`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing consumed by code. Task 6 bundles a copy of the MIT text as an asset; the canonical text is established here.

- [ ] **Step 1: Create `LICENSE`**

```
MIT License

Copyright (c) 2026 blank204

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

- [ ] **Step 2: Create `README.md`**

```markdown
# Fountain

A pixel-styled, distraction-killing Android launcher. Grayscale home screen,
per-app time gates that send you home when time expires, a launcher-level lock
overlay, and a custom notification inbox.

**Fountain has no internet permission.** It cannot transmit data off your
device, and it collects nothing. See the [privacy policy](https://blank204.github.io/fountain/privacy).

## Status

Kotlin / Jetpack Compose. minSdk 29, targetSdk 34. Developed against a Samsung
Galaxy Note 9 running Android 10.

## Building

```bash
./gradlew :app:assembleDebug
```

Release builds need a `keystore.properties` at the repo root pointing at a
signing keystore. Neither is in version control.

## Permissions

Fountain asks for several sensitive permissions and explains each one in-app
before requesting it:

| Permission | Why |
|---|---|
| Accessibility service | Detect which app is in front, so a time-gated app can be gated and a finished session can send you home. Fountain does **not** read screen content. |
| Notification listener | Capture notifications into Fountain's own inbox and mute the apps you choose. |
| `QUERY_ALL_PACKAGES` | A launcher needs the list of installed launchable apps. |
| `SCHEDULE_EXACT_ALARM` | End a session at its exact deadline, even in Doze. |
| Device admin (force-lock only) | The optional double-tap-to-lock gesture. |

## Licence

MIT — see [LICENSE](LICENSE). Bundled third-party assets and their licences are
listed in [ATTRIBUTIONS.md](ATTRIBUTIONS.md).

## Contributing

Contributions are welcome. Please sign off your commits (`git commit -s`) to
certify the [Developer Certificate of Origin](https://developercertificate.org/).
Sign-off keeps the project's licensing unambiguous as contributors accumulate.
```

- [ ] **Step 3: Create `app/proguard-rules.pro`**

`app/build.gradle.kts` already references this file. It is unused while
`isMinifyEnabled = false`, but its absence becomes a build failure the moment
minification is enabled in sub-project 3.

```
# ProGuard/R8 rules for Fountain.
#
# Currently unused: the release build sets isMinifyEnabled = false.
# This file exists because app/build.gradle.kts references it in proguardFiles,
# and enabling minification without it would fail the build.
#
# Room, Compose, and DataStore all ship their own consumer rules, so no manual
# keep rules are needed yet. Add them here if minification is enabled and
# reflection-dependent code breaks.
```

- [ ] **Step 4: Rewrite `ATTRIBUTIONS.md`**

Adds the missing `pixel_pop.ogg` row, records the bundled OFL text (closing the
file's own outstanding note), and lists the Apache-2.0 dependencies.

```markdown
# Third-party assets & licenses

Every bundled asset must be license-safe for commercial release. Keep this file current
as assets are added.

## Bundled assets

| Asset | Path | License | Source |
|---|---|---|---|
| Pixelify Sans (variable) | `app/src/main/res/font/pixelify_sans.ttf` | SIL Open Font License 1.1 | Google Fonts — github.com/google/fonts `ofl/pixelifysans` |
| App icon (pixel fountain) | `app/src/main/res/drawable/ic_launcher_foreground.xml` | Original (self-made) | This project |
| Pixel-pop notification sound | `app/src/main/res/raw/pixel_pop.ogg` | Original (self-made) | This project — synthesized, not sampled |

## Dependencies

All runtime dependencies are Apache License 2.0:

- AndroidX Core, Lifecycle, Activity, Biometric
- Jetpack Compose (UI, Foundation, Material3)
- AndroidX Room
- AndroidX DataStore
- Kotlin standard library

## License texts

Full license texts are bundled **inside the app** at
`app/src/main/assets/licenses/` and shown in Settings → About. This is a
requirement, not a courtesy: SIL OFL 1.1 requires that the license accompany
the software wherever it is distributed, which includes the shipped AAB — not
just this repository.

## Notes

- **No Toby Fox / Deltarune assets** are shipped. The pixel-pop sound is an original
  synthesized asset, not the real game blip. Fountain is not marketed using Deltarune
  or Toby Fox names or trademarks.
- Pixelify Sans ships unmodified, so the OFL Reserved Font Name clause is not engaged.
```

- [ ] **Step 5: Commit**

```bash
git add LICENSE README.md ATTRIBUTIONS.md app/proguard-rules.pro
git commit -m "$(cat <<'EOF'
Add MIT LICENSE, restore README, complete ATTRIBUTIONS

Licenses the project explicitly as MIT. Records pixel_pop.ogg as original
self-made work and the Apache-2.0 dependency set. Adds the proguard-rules.pro
file that app/build.gradle.kts already references.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01Y2YsDChGsKhNUJnvmTuWTS
EOF
)"
```

---

### Task 2: Privacy policy and Play Console declarations

Produces the hosted privacy policy (required before Play accepts any release) and a version-controlled record of every Console answer, so resubmissions stay consistent.

**Files:**
- Create: `docs/privacy.md`
- Create: `docs/index.md`
- Create: `docs/_config.yml`
- Create: `docs/play-console-declarations.md`

**Interfaces:**
- Consumes: nothing.
- Produces: `docs/privacy.md` — Task 4 adds a test asserting it is byte-identical to the copy bundled at `app/src/main/assets/legal/privacy-policy.md`, and Task 6 renders that bundled copy. **Do not reformat `docs/privacy.md` after Task 4 without re-running that test.**

- [ ] **Step 1: Create `docs/_config.yml`**

GitHub Pages needs a theme to render Markdown. Minimal config:

```yaml
theme: jekyll-theme-minimal
title: Fountain
description: A distraction-killing Android launcher
```

- [ ] **Step 2: Create `docs/index.md`**

```markdown
---
title: Fountain
---

# Fountain

A pixel-styled, distraction-killing Android launcher with per-app time gates.

Fountain has no internet permission. It collects nothing and transmits nothing.

- [Privacy policy](privacy)
- [Source on GitHub](https://github.com/blank204/fountain)
```

- [ ] **Step 3: Create `docs/privacy.md`**

This is the document Play requires a URL for. Every claim in it is verifiable
from the source.

```markdown
---
title: Privacy Policy
---

# Fountain — Privacy Policy

**Last updated:** 27 July 2026

## The short version

Fountain collects nothing, transmits nothing, and shares nothing. It does not
request the Android `INTERNET` permission, so it is not merely a promise not to
send your data anywhere — the app is technically incapable of doing so. You can
verify this yourself: the permission is absent from the app manifest, and the
source is public.

There are no accounts, no analytics, no advertising, no crash reporting, and no
third-party SDKs of any kind.

## What Fountain stores on your device

All of this stays in Fountain's private app storage. None of it is transmitted.

| Data | Why |
|---|---|
| Your list of time-gated apps | So Fountain knows which apps to gate |
| Session history (which app, when, how long) | Powers the time-gate mechanic |
| Notification rules (per-app show / suppress / passthrough) | So Fountain knows how to treat each app's notifications |
| Captured notifications (app name, title, text, timestamp) | So they can be shown in Fountain's inbox |
| Your preferences and hidden-app list | Settings |

Cloud backup is disabled for Fountain, so none of this is copied to Google Drive
or any other backup service.

## Sensitive permissions

Fountain asks for each of these in-app, explains it before requesting it, and
works — with reduced functionality — if you decline.

### Accessibility service

**What it accesses:** only the package name of the app currently in the
foreground.

**What it cannot access:** screen content, text you type, or anything inside
your apps. The service is declared with `canRetrieveWindowContent="false"` and
`canPerformGestures="false"`, and listens for a single event type
(`typeWindowStateChanged`).

**What it is used for:** noticing when a time-gated app opens, so Fountain can
ask how long you want, and returning you to the home screen when that time
expires.

**Where it goes:** nowhere. It is held in memory and never written to storage,
never transmitted, never shared.

### Notification access

**What it accesses:** the app name, title, text, and timestamp of notifications
posted by apps you have configured.

**What it is used for:** showing those notifications in Fountain's own inbox and
muting the apps you have chosen to mute.

**Where it is stored:** in Fountain's local database on your device only. It is
never transmitted or shared. You can delete all of it at any time from
Settings → Inbox, or by uninstalling Fountain.

### Other permissions

- **`QUERY_ALL_PACKAGES`** — a launcher needs the list of installed launchable
  apps in order to be a launcher. The list is read and displayed; it is not
  recorded or transmitted.
- **`SCHEDULE_EXACT_ALARM`** — ends a session at its exact deadline, even when
  the device is in Doze. Without it, a session may overrun.
- **Device administrator (force-lock only)** — powers the optional
  double-tap-to-lock gesture. Fountain requests no other device-admin policy and
  cannot wipe, restrict, or manage your device.

## Deleting your data

Clear captured notifications from Settings → Inbox. To remove everything,
uninstall Fountain — Android deletes its private storage with it. Because
Fountain stores nothing off your device, there is nothing else to request or
erase.

## Children

Fountain is not directed at children under 13 and does not knowingly collect
information from anyone. Since it collects nothing from anyone, this is a
statement of fact rather than a policy commitment.

## Changes

Any change to this policy will be published at this URL with an updated date.
The full revision history is public at
<https://github.com/blank204/fountain/commits/main/docs/privacy.md>.

## Contact

gshriadhithya@gmail.com
```

- [ ] **Step 4: Create `docs/play-console-declarations.md`**

Not a Play artefact — a working document holding the exact answers to paste into
the Console, so they stay consistent across resubmissions.

```markdown
# Play Console — declaration answers

Paste-ready answers for the "App content" section. Keep in sync with the
manifest and `docs/privacy.md`.

**Privacy policy URL:** `https://blank204.github.io/fountain/privacy`

## Data safety

**Does your app collect or share any of the required user data types?** — **No.**

Play defines collection as transmitting data off the device. Fountain does not
declare the `INTERNET` permission and links no networking library, so no data
can leave the device. Notification content and session history are stored in
Fountain's private app storage and are never transmitted. Cloud backup is
disabled (`android:allowBackup="false"`).

- Data collected: none
- Data shared: none
- Data encrypted in transit: N/A — no data in transit
- Users can request deletion: N/A — uninstalling removes all data

## Accessibility declaration

**Is your app an accessibility tool?** — **No** (`isAccessibilityTool` is not set).

**Justification:**

> Fountain is a launcher with per-app time limits. It uses the AccessibilityService
> API for one purpose: to observe which application is in the foreground, so that
> a user-configured time-limited app can be gated on open, and so the user can be
> returned to the home screen when the session they chose expires. There is no
> other way on Android to detect a foreground app change from outside the app.
>
> The service accesses only the foreground package name. It is declared with
> `canRetrieveWindowContent="false"` and `canPerformGestures="false"`, and
> subscribes to a single event type, `typeWindowStateChanged`. It cannot read
> screen content, keystrokes, or in-app data. The package name is used in memory
> and never persisted, transmitted, or shared. Fountain does not declare the
> INTERNET permission and is incapable of transmitting data.
>
> A full-screen prominent disclosure is shown in-app immediately before the user
> is sent to the system Accessibility settings. It describes the data accessed,
> how it is used, and that it is never shared, and requires an explicit
> affirmative tap before proceeding. Declining leaves the service disabled and
> the app usable.

## QUERY_ALL_PACKAGES declaration

**Justification:**

> Fountain is a home-screen replacement (it declares
> `android.intent.category.HOME`). Its core function is showing an A–Z list of
> every installed launchable app so the user can search and launch them, and
> letting the user pick which of those apps to apply a time limit to. Both
> require the complete list of installed launchable applications. The list is
> read and displayed; it is not recorded, transmitted, or shared.

## SCHEDULE_EXACT_ALARM declaration

**Justification:**

> Fountain's core feature is a user-initiated countdown: the user explicitly
> chooses a duration (5, 10, 15, or 20 minutes) for an app, and Fountain returns
> them to the home screen when it expires. The session end time is persisted and
> an exact alarm is scheduled against it, so the deadline holds across process
> death and Doze. An inexact alarm would let sessions overrun by an arbitrary
> amount, which would defeat the feature. `USE_EXACT_ALARM` is deliberately not
> declared — the user grants exact alarm access themselves during setup.

## Foreground service — specialUse

**Type:** `specialUse`
**Subtype property:** `focus_time_gate`

**Justification:**

> The service owns an active, user-initiated countdown for a time-limited app
> session and displays the remaining time in its notification. No predefined
> foreground service type describes a user-facing focus timer: it plays no
> media, syncs no data, uses no location, camera, or microphone, and manages no
> connected device. `shortService` is not usable because it is limited to
> roughly three minutes while sessions run 5 to 20 minutes. The service is
> started only when the user has explicitly begun a session and stops when that
> session ends.

**Risk note:** this is the declaration most likely to draw a manual rejection.
If Google rejects `specialUse`, the fallback is to drop the foreground service
and rely solely on the persisted end-time plus the exact alarm, losing the
live countdown notification. Do not attempt that pre-emptively.

## Device admin

Only `force-lock` is declared (`res/xml/device_admin.xml`). It powers an optional
double-tap-to-lock gesture that turns the screen off. No wipe, no password
policy, no device management. It is optional and the app is fully functional
without it.

## Content rating questionnaire

- Violence, sexuality, profanity, controlled substances, gambling: **No** to all
- User-generated content or user interaction: **No**
- Shares location: **No**
- Allows purchases: **No**
- Contains ads: **No**

Expected outcome: rated for everyone.

## Target audience and content

- **Target age group:** 13+
- **Appeals to children:** No
- **Designed for Families:** No

## Ads

**Does your app contain ads?** — **No.**

## App access

**Are any parts of your app restricted?** — **No login required.**

Provide these instructions to reviewers, because two features cannot be granted
programmatically and a reviewer who skips them will see a non-functional app:

> Fountain is a launcher. No account or login is needed.
>
> To exercise the time-gate feature:
> 1. Open Fountain. The first-run setup screen lists each permission.
> 2. Tap "Enable force-kick." A full-screen disclosure appears describing the
>    Accessibility usage. Tap "I understand — continue," then enable Fountain in
>    the system Accessibility settings and return to the app.
> 3. Back in Fountain, go to Settings → Gated apps and select any installed app.
> 4. Return to the home list and tap that app. A time picker appears; choose 5
>    minutes and confirm. The app opens and a countdown notification appears.
> 5. When the timer expires, you are returned to the home screen.
>
> To exercise the notification inbox, tap "Capture notifications" during setup,
> accept the disclosure, and enable Fountain in the system Notification access
> settings. Then Settings → Notifications to choose per-app behaviour.
>
> Setting Fountain as the default launcher is optional for review. Android always
> allows switching back via system Settings → Apps → Default apps.
```

- [ ] **Step 5: Commit**

```bash
git add docs/privacy.md docs/index.md docs/_config.yml docs/play-console-declarations.md
git commit -m "$(cat <<'EOF'
Add privacy policy and Play Console declaration answers

Privacy policy is published via GitHub Pages from docs/ and is the URL Play
requires before accepting a release. The declarations file records paste-ready
answers for Data Safety, the accessibility declaration, QUERY_ALL_PACKAGES,
exact alarms, the specialUse foreground service, content rating, and reviewer
access instructions, so resubmissions stay consistent.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01Y2YsDChGsKhNUJnvmTuWTS
EOF
)"
```

- [ ] **Step 6: Enable GitHub Pages (manual, in a browser)**

Go to `https://github.com/blank204/fountain/settings/pages`. Set **Source** to
"Deploy from a branch," **Branch** to `main`, folder `/docs`. Save. After the
build completes, confirm `https://blank204.github.io/fountain/privacy` loads.

This step is done by a human in a browser and cannot be automated here. The
plan's remaining tasks do not depend on it, so it does not block progress —
but the URL must resolve before the Play Console submission.

---

### Task 3: Manifest and permission corrections

Removes a permission Play restricts, disables cloud backup so the privacy policy is true, and deletes a dead onboarding step that also pins the home setup banner permanently on.

**Files:**
- Modify: `app/src/main/AndroidManifest.xml:12` (remove `USE_EXACT_ALARM`)
- Modify: `app/src/main/AndroidManifest.xml:18` (`allowBackup`)
- Modify: `app/src/main/java/com/fountain/launcher/onboarding/OnboardingScreen.kt:112-118` (delete overlay step)
- Modify: `app/src/main/java/com/fountain/launcher/app/MainActivity.kt:117` (drop `canDrawOverlays`)
- Modify: `app/src/main/java/com/fountain/launcher/common/SystemAccess.kt:42-50` (delete now-dead functions)

**Interfaces:**
- Consumes: nothing.
- Produces: `SystemAccess.canDrawOverlays` and `SystemAccess.openOverlaySettings` **no longer exist**. Task 5 modifies the same `OnboardingScreen` file and must not reintroduce them.

- [ ] **Step 1: Remove `USE_EXACT_ALARM` from the manifest**

Delete this single line at `app/src/main/AndroidManifest.xml:12`:

```xml
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
```

Leave `SCHEDULE_EXACT_ALARM` on line 11 in place. Play restricts `USE_EXACT_ALARM`
to apps whose core function is an alarm clock or calendar; it is auto-granted, so
declaring it on a launcher invites scrutiny for no benefit. The onboarding
"Precise timing" step already walks the user through granting
`SCHEDULE_EXACT_ALARM` themselves.

- [ ] **Step 2: Disable cloud backup**

At `app/src/main/AndroidManifest.xml:18`, change:

```xml
        android:allowBackup="true"
```

to:

```xml
        android:allowBackup="false"
```

`CapturedNotificationEntity` persists notification `title` and `text` to Room.
With backup enabled, that content syncs to Google Drive — contradicting the
in-app claim and the privacy policy that no data leaves the device. The accepted
cost is that settings no longer survive a device migration.

- [ ] **Step 3: Delete the overlay onboarding step**

Remove this entire `Step(...)` block from `OnboardingScreen.kt:112-118`:

```kotlin
        Step(
            title = "Show the gate over apps",
            why = "Lets the time picker pop up on top of a gated app — not only inside Fountain.",
            done = SystemAccess.canDrawOverlays(context),
            action = "Allow",
            onAction = { SystemAccess.openOverlaySettings(context) },
        )
```

The app declares no `SYSTEM_ALERT_WINDOW` permission and creates no overlay
window anywhere — `GateActivity` is a plain Activity. This step could never be
satisfied and asked users for special access the app does not use.

- [ ] **Step 4: Fix the permanently-on setup banner**

At `MainActivity.kt:114-119`, remove the `canDrawOverlays` clause so the block reads:

```kotlin
    val setupIncomplete = remember(permCheck) {
        !SystemAccess.isDefaultLauncher(context) ||
            !AccessibilityUtil.isServiceEnabled(context) ||
            !NotificationAccessUtil.isEnabled(context)
    }
```

Because `canDrawOverlays` could never return true, `setupIncomplete` was
permanently true and the home "finish setup" banner showed forever, even after a
user granted everything correctly.

- [ ] **Step 5: Delete the now-dead functions**

In `SystemAccess.kt`, delete `canDrawOverlays` (line 42) and `openOverlaySettings`
(lines 44-50) entirely. Then remove any import left unused by the deletion —
check whether `android.net.Uri` is still referenced elsewhere in the file before
removing its import.

- [ ] **Step 6: Verify no references remain**

Run:

```bash
grep -rn "canDrawOverlays\|openOverlaySettings\|USE_EXACT_ALARM" app/src/
```

Expected: no output at all.

- [ ] **Step 7: Verify the build still compiles**

Run:

```bash
./gradlew :app:assembleDebug --console=plain
```

Expected: `BUILD SUCCESSFUL`. If it fails on an unused import in `SystemAccess.kt`,
remove that import and re-run.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/fountain/launcher/onboarding/OnboardingScreen.kt app/src/main/java/com/fountain/launcher/app/MainActivity.kt app/src/main/java/com/fountain/launcher/common/SystemAccess.kt
git commit -m "$(cat <<'EOF'
Correct permission surface and disable cloud backup

Removes USE_EXACT_ALARM, which Play restricts to alarm clock and calendar apps.
SCHEDULE_EXACT_ALARM remains and onboarding already walks the user through it.

Sets allowBackup=false. Captured notification title and text are persisted to
Room, so cloud backup was copying notification content to Google Drive while
the app claimed nothing leaves the device.

Deletes the overlay onboarding step and its helpers. No SYSTEM_ALERT_WINDOW is
declared and no overlay window is ever created, so the step could not be
satisfied — which also pinned the home "finish setup" banner permanently on via
setupIncomplete.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01Y2YsDChGsKhNUJnvmTuWTS
EOF
)"
```

---

### Task 4: Disclosure copy model and compliance tests

The disclosure text is the compliance-critical part of this whole plan. This task makes it a pure, testable data structure and locks its required content behind tests, so a future refactor cannot silently gut it.

**Files:**
- Modify: `app/build.gradle.kts` (add JUnit; add `buildConfig = true`)
- Create: `app/src/main/java/com/fountain/launcher/compliance/Disclosure.kt`
- Create: `app/src/main/assets/legal/privacy-policy.md`
- Test: `app/src/test/java/com/fountain/launcher/compliance/DisclosureCopyTest.kt`
- Test: `app/src/test/java/com/fountain/launcher/compliance/PrivacyPolicyMirrorTest.kt`

**Interfaces:**
- Consumes: `docs/privacy.md` from Task 2.
- Produces:
  - `enum class SensitiveService { ACCESSIBILITY, NOTIFICATION_LISTENER }`
  - `data class DisclosureCopy(title, whatIsAccessed, howItIsUsed, howItIsShared, consentLabel, dismissLabel)` — all `String`
  - `fun disclosureCopyFor(service: SensitiveService): DisclosureCopy`

  Task 5 consumes all three. Use these exact names.

- [ ] **Step 1: Add JUnit and enable BuildConfig**

In `app/build.gradle.kts`, change the `buildFeatures` block to:

```kotlin
    buildFeatures {
        compose = true
        buildConfig = true
    }
```

`BuildConfig` is off by default in AGP 8; Task 6's About screen needs
`BuildConfig.VERSION_NAME`.

Then add to the `dependencies` block, after the `debugImplementation` line:

```kotlin
    testImplementation("junit:junit:4.13.2")
```

This is the only dependency this plan adds. The project has no test
infrastructure today; these are plain JVM unit tests with no Android
dependencies, so they need no emulator.

- [ ] **Step 2: Write the failing disclosure test**

Create `app/src/test/java/com/fountain/launcher/compliance/DisclosureCopyTest.kt`:

```kotlin
package com.fountain.launcher.compliance

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Google Play requires that a non-accessibility use of the AccessibilityService API
 * carry an in-app disclosure describing the data accessed, how it is used, and how it
 * is shared, with affirmative consent. These tests pin that content so a later
 * refactor cannot quietly remove it.
 */
class DisclosureCopyTest {

    @Test
    fun `every service has all disclosure sections populated`() {
        for (service in SensitiveService.entries) {
            val copy = disclosureCopyFor(service)
            assertTrue("$service title is blank", copy.title.isNotBlank())
            assertTrue("$service whatIsAccessed is blank", copy.whatIsAccessed.isNotBlank())
            assertTrue("$service howItIsUsed is blank", copy.howItIsUsed.isNotBlank())
            assertTrue("$service howItIsShared is blank", copy.howItIsShared.isNotBlank())
            assertTrue("$service consentLabel is blank", copy.consentLabel.isNotBlank())
            assertTrue("$service dismissLabel is blank", copy.dismissLabel.isNotBlank())
        }
    }

    @Test
    fun `every service states that data is never transmitted`() {
        for (service in SensitiveService.entries) {
            val shared = disclosureCopyFor(service).howItIsShared.lowercase()
            assertTrue(
                "$service must state data is never transmitted",
                shared.contains("never") && shared.contains("transmit"),
            )
        }
    }

    @Test
    fun `accessibility copy denies reading screen content`() {
        val copy = disclosureCopyFor(SensitiveService.ACCESSIBILITY)
        val accessed = copy.whatIsAccessed.lowercase()
        assertTrue(
            "must state only the foreground package name is read",
            accessed.contains("package name"),
        )
        assertTrue(
            "must explicitly deny reading screen content",
            accessed.contains("cannot read") && accessed.contains("screen content"),
        )
    }

    @Test
    fun `accessibility copy cites the no-internet-permission fact`() {
        val shared = disclosureCopyFor(SensitiveService.ACCESSIBILITY).howItIsShared.lowercase()
        assertTrue(
            "the strongest verifiable claim must be stated",
            shared.contains("no internet permission"),
        )
    }

    @Test
    fun `notification copy names the stored fields and how to erase them`() {
        val copy = disclosureCopyFor(SensitiveService.NOTIFICATION_LISTENER)
        val accessed = copy.whatIsAccessed.lowercase()
        assertTrue("must name the title field", accessed.contains("title"))
        assertTrue("must name the text field", accessed.contains("text"))

        val shared = copy.howItIsShared.lowercase()
        assertTrue(
            "must say the data is stored on this device",
            shared.contains("this device"),
        )
        assertTrue(
            "must tell the user how to delete it",
            shared.contains("inbox"),
        )
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --console=plain
```

Expected: FAIL — compilation error, `Unresolved reference: SensitiveService`.

- [ ] **Step 4: Write the disclosure model**

Create `app/src/main/java/com/fountain/launcher/compliance/Disclosure.kt`:

```kotlin
package com.fountain.launcher.compliance

/**
 * Fountain's Google Play policy surface, kept in one package so it is findable.
 *
 * Fountain is not an accessibility tool, so it cannot set isAccessibilityTool=true.
 * Play therefore requires an in-app disclosure that names the data accessed, explains
 * how it is used and shared, and takes affirmative consent — shown during normal app
 * usage, not buried in a menu. DisclosureCopyTest pins the required content.
 */
enum class SensitiveService { ACCESSIBILITY, NOTIFICATION_LISTENER }

/** The three sections Play's policy demands, plus the two button labels. */
data class DisclosureCopy(
    val title: String,
    val whatIsAccessed: String,
    val howItIsUsed: String,
    val howItIsShared: String,
    val consentLabel: String,
    val dismissLabel: String,
)

fun disclosureCopyFor(service: SensitiveService): DisclosureCopy = when (service) {
    SensitiveService.ACCESSIBILITY -> DisclosureCopy(
        title = "Before you enable force-kick",
        whatIsAccessed =
            "Fountain reads only the package name of the app currently in front. " +
                "It cannot read screen content, text you type, or anything inside your apps.",
        howItIsUsed =
            "Noticing when a time-gated app opens, so Fountain can ask how long you want — " +
                "and sending you back home when that time is up.",
        howItIsShared =
            "Nowhere. It is held in memory, never saved, never transmitted, never shared " +
                "with anyone. Fountain has no internet permission and is incapable of " +
                "sending anything off your device.",
        consentLabel = "I understand — continue",
        dismissLabel = "Not now",
    )

    SensitiveService.NOTIFICATION_LISTENER -> DisclosureCopy(
        title = "Before you enable the inbox",
        whatIsAccessed =
            "For apps you choose, Fountain reads the app name, title, text and time of " +
                "notifications they post.",
        howItIsUsed =
            "Showing those notifications in Fountain's own inbox, and muting the apps you " +
                "have asked to mute.",
        howItIsShared =
            "Never transmitted, never shared. It is saved only in Fountain's database on " +
                "this device, and you can erase all of it any time from Settings → Inbox.",
        consentLabel = "I understand — continue",
        dismissLabel = "Not now",
    )
}
```

- [ ] **Step 5: Run the disclosure test to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --console=plain
```

Expected: PASS, 5 tests in `DisclosureCopyTest`.

- [ ] **Step 6: Write the failing policy-mirror test**

The About screen renders a bundled copy of the privacy policy so it works
offline. Two copies of a legal document is a drift risk, and an in-app policy
that contradicts the hosted one is a real compliance problem. This test makes
drift a build failure.

Create `app/src/test/java/com/fountain/launcher/compliance/PrivacyPolicyMirrorTest.kt`:

```kotlin
package com.fountain.launcher.compliance

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The policy is published from docs/privacy.md via GitHub Pages and bundled into the
 * app so About works offline. If the two ever diverge, the app shows users a different
 * policy than the one Play was given. Fail loudly instead.
 *
 * Gradle runs unit tests with the module directory (app/) as the working directory.
 */
class PrivacyPolicyMirrorTest {

    private val published = File("../docs/privacy.md")
    private val bundled = File("src/main/assets/legal/privacy-policy.md")

    @Test
    fun `both copies of the privacy policy exist`() {
        assertTrue("missing ${published.path}", published.isFile)
        assertTrue("missing ${bundled.path}", bundled.isFile)
    }

    @Test
    fun `bundled policy is identical to the published policy`() {
        assertEquals(
            "docs/privacy.md and the bundled asset have diverged — re-copy one over the other",
            published.readText().replace("\r\n", "\n"),
            bundled.readText().replace("\r\n", "\n"),
        )
    }
}
```

- [ ] **Step 7: Run it to verify it fails**

Run:

```bash
./gradlew :app:testDebugUnitTest --console=plain
```

Expected: FAIL — `missing src/main/assets/legal/privacy-policy.md`.

- [ ] **Step 8: Bundle the policy**

Copy the file verbatim:

```bash
mkdir -p app/src/main/assets/legal
cp docs/privacy.md app/src/main/assets/legal/privacy-policy.md
```

- [ ] **Step 9: Run the full test suite to verify it passes**

Run:

```bash
./gradlew :app:testDebugUnitTest --console=plain
```

Expected: PASS, 7 tests total across both classes.

- [ ] **Step 10: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/fountain/launcher/compliance/ app/src/test/ app/src/main/assets/legal/
git commit -m "$(cat <<'EOF'
Add disclosure copy model with compliance tests

The in-app disclosure text is what Play actually reviews, so it is a pure
testable data structure rather than inline strings. Tests pin the required
content: all three policy sections populated, an explicit denial of reading
screen content, the no-internet-permission claim, and the notification fields
plus how to erase them.

Bundles the privacy policy as an asset so About works offline, with a test
asserting it stays byte-identical to the published docs/privacy.md — a divergent
in-app policy would contradict the one given to Play.

Adds JUnit and enables buildConfig for the About screen's version string.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01Y2YsDChGsKhNUJnvmTuWTS
EOF
)"
```

---

### Task 5: Disclosure gate UI and onboarding wiring

Puts the disclosure in front of the user at the moment it matters — immediately before the grant — and records their consent.

**Files:**
- Modify: `app/src/main/java/com/fountain/launcher/data/SettingsRepository.kt`
- Create: `app/src/main/java/com/fountain/launcher/compliance/DisclosureGate.kt`
- Modify: `app/src/main/java/com/fountain/launcher/onboarding/OnboardingScreen.kt`

**Interfaces:**
- Consumes: `SensitiveService`, `DisclosureCopy`, `disclosureCopyFor` from Task 4.
- Produces:
  - `@Composable fun DisclosureGate(service: SensitiveService, onConsent: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier)`
  - `FountainSettings.consentAccessibility: Boolean` and `FountainSettings.consentNotifications: Boolean`
  - `SettingsRepository.setConsentAccessibility(granted: Boolean)` and `setConsentNotifications(granted: Boolean)`

  Task 6 reads the two `FountainSettings` fields.

- [ ] **Step 1: Add consent persistence to `SettingsRepository`**

Three edits to `SettingsRepository.kt`.

Add two fields to `FountainSettings`, after `onboardingSeen`:

```kotlin
    val consentAccessibility: Boolean = false,     // Play disclosure acknowledged
    val consentNotifications: Boolean = false,     // Play disclosure acknowledged
```

Add two lines inside the `settings` flow's `map`, after the `onboardingSeen` line:

```kotlin
            consentAccessibility = p[KEY_CONSENT_ACCESSIBILITY] ?: false,
            consentNotifications = p[KEY_CONSENT_NOTIFICATIONS] ?: false,
```

Add two setters after `setOnboardingSeen`:

```kotlin
    suspend fun setConsentAccessibility(granted: Boolean) =
        edit { it[KEY_CONSENT_ACCESSIBILITY] = granted }

    suspend fun setConsentNotifications(granted: Boolean) =
        edit { it[KEY_CONSENT_NOTIFICATIONS] = granted }
```

And two keys in the `companion object`:

```kotlin
        val KEY_CONSENT_ACCESSIBILITY = booleanPreferencesKey("consent_accessibility")
        val KEY_CONSENT_NOTIFICATIONS = booleanPreferencesKey("consent_notifications")
```

These do not gate the flow — the gate itself does that. They are a record, shown
on the About screen in Task 6.

- [ ] **Step 2: Create the gate composable**

Create `app/src/main/java/com/fountain/launcher/compliance/DisclosureGate.kt`:

```kotlin
package com.fountain.launcher.compliance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fountain.launcher.ui.theme.FountainPalette

/**
 * Play's prominent-disclosure requirement, rendered. Shown full-screen immediately
 * before Fountain sends the user to a system settings screen to grant a sensitive
 * service — never behind a menu, and never after the fact.
 */
@Composable
fun DisclosureGate(
    service: SensitiveService,
    onConsent: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = disclosureCopyFor(service)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FountainPalette.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = copy.title,
            style = MaterialTheme.typography.titleLarge,
            color = FountainPalette.Mono6,
        )

        DisclosureSection("What Fountain reads", copy.whatIsAccessed)
        DisclosureSection("What it's used for", copy.howItIsUsed)
        DisclosureSection("Where it goes", copy.howItIsShared)

        Button(
            onClick = onConsent,
            colors = ButtonDefaults.buttonColors(
                containerColor = FountainPalette.PurplePrimary,
                contentColor = FountainPalette.Mono6,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(copy.consentLabel, style = MaterialTheme.typography.bodyLarge)
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(copy.dismissLabel, color = FountainPalette.Mono3)
        }
    }
}

@Composable
private fun DisclosureSection(heading: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FountainPalette.Surface)
            .padding(16.dp),
    ) {
        Text(
            text = heading,
            style = MaterialTheme.typography.bodyLarge,
            color = FountainPalette.MagentaHi,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.labelSmall,
            color = FountainPalette.Mono5,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
```

- [ ] **Step 3: Wire the gate into onboarding**

Four edits to `OnboardingScreen.kt`.

Add these imports:

```kotlin
import com.fountain.launcher.compliance.DisclosureGate
import com.fountain.launcher.compliance.SensitiveService
import com.fountain.launcher.data.SettingsRepository
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
```

Immediately after `val lifecycleOwner = LocalLifecycleOwner.current` near the top
of `OnboardingScreen`, add:

```kotlin
    val scope = rememberCoroutineScope()
    var pendingDisclosure by remember { mutableStateOf<SensitiveService?>(null) }
```

`mutableStateOf` is already imported in this file.

Change the two sensitive steps so their `onAction` opens the gate instead of
jumping straight to system settings. The "Enable force-kick" step becomes:

```kotlin
        Step(
            title = "Enable force-kick",
            why = "Accessibility lets Fountain notice a gated app and send you home when time is up.",
            done = AccessibilityUtil.isServiceEnabled(context),
            action = "Enable",
            onAction = { pendingDisclosure = SensitiveService.ACCESSIBILITY },
        )
```

and the "Capture notifications" step becomes:

```kotlin
        Step(
            title = "Capture notifications",
            why = "Optional. Lets the inbox and mute rules work.",
            done = NotificationAccessUtil.isEnabled(context),
            action = "Grant",
            onAction = { pendingDisclosure = SensitiveService.NOTIFICATION_LISTENER },
        )
```

Finally, wrap the whole existing `Column` so the gate can render over it. Change
the outermost composable of `OnboardingScreen` from a bare `Column` to:

```kotlin
    Box(Modifier.fillMaxSize()) {
        Column(
            // ...existing modifier and arrangement, unchanged...
        ) {
            // ...all existing Step calls and the Done button, unchanged...
        }

        pendingDisclosure?.let { service ->
            DisclosureGate(
                service = service,
                onConsent = {
                    scope.launch {
                        val repo = SettingsRepository(context)
                        when (service) {
                            SensitiveService.ACCESSIBILITY -> repo.setConsentAccessibility(true)
                            SensitiveService.NOTIFICATION_LISTENER -> repo.setConsentNotifications(true)
                        }
                    }
                    when (service) {
                        SensitiveService.ACCESSIBILITY -> AccessibilityUtil.openSettings(context)
                        SensitiveService.NOTIFICATION_LISTENER -> NotificationAccessUtil.openSettings(context)
                    }
                    pendingDisclosure = null
                },
                onDismiss = { pendingDisclosure = null },
            )
        }
    }
```

Add `import androidx.compose.foundation.layout.Box` for this.

Note the ordering inside `onConsent`: consent is persisted **before** the system
settings screen opens. The `Step` composable only renders its action button when
`!done`, so the grant action exists only while the service is off — which makes
"gate before grant" automatic, with no extra conditional logic.

- [ ] **Step 4: Verify the build compiles and tests still pass**

Run:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Expected: `BUILD SUCCESSFUL`, 7 tests passing.

- [ ] **Step 5: Verify on device**

Install and check, on the Note 9 or any Android 10+ device:

```bash
./gradlew :app:installDebug
```

1. Clear app data first so onboarding shows: `adb shell pm clear com.fountain.launcher.debug`
2. Open Fountain. Tap "Enable force-kick." **The disclosure must appear before any system screen** — with all three sections and both buttons.
3. Tap "Not now." You return to onboarding; Accessibility is still off.
4. Tap "Enable force-kick" again, then "I understand — continue." The system Accessibility settings open.
5. Enable Fountain, return. The step shows `✓` and its button is gone.
6. Repeat 2–5 for "Capture notifications."
7. Confirm the "Show the gate over apps" step is gone (deleted in Task 3).
8. With launcher, accessibility, and notification access all granted, confirm the home "finish setup" banner is **gone**. This was impossible before Task 3.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/fountain/launcher/data/SettingsRepository.kt app/src/main/java/com/fountain/launcher/compliance/DisclosureGate.kt app/src/main/java/com/fountain/launcher/onboarding/OnboardingScreen.kt
git commit -m "$(cat <<'EOF'
Show a prominent disclosure before granting sensitive services

Play requires a non-accessibility use of the AccessibilityService API to carry
an in-app disclosure naming the data accessed, how it is used and shared, with
affirmative consent, shown during normal usage rather than behind a menu. The
previous onboarding offered one subtitle line and a button.

The gate is now the only path to the grant action for both Accessibility and
Notification access. Since a step renders its button only while the service is
off, gating the grant automatically gates every enable. Consent is persisted
before the system screen opens.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01Y2YsDChGsKhNUJnvmTuWTS
EOF
)"
```

---

### Task 6: About screen with licences and policy

Surfaces the privacy policy, the bundled licence texts, and consent status. Bundling the licences is required, not decorative: SIL OFL 1.1 obliges the licence to accompany the software in the shipped binary.

**Files:**
- Create: `app/src/main/assets/licenses/MIT.txt`
- Create: `app/src/main/assets/licenses/OFL-1.1.txt`
- Create: `app/src/main/assets/licenses/Apache-2.0.txt`
- Create: `app/src/main/java/com/fountain/launcher/settings/AboutScreen.kt`
- Modify: `app/src/main/java/com/fountain/launcher/settings/SettingsHubScreen.kt`
- Modify: `app/src/main/java/com/fountain/launcher/app/MainActivity.kt`

**Interfaces:**
- Consumes: `FountainSettings.consentAccessibility` and `.consentNotifications` from Task 5; `BuildConfig.VERSION_NAME` enabled in Task 4.
- Produces: `@Composable fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier)`. Nothing later consumes it.

- [ ] **Step 1: Bundle the licence texts**

```bash
mkdir -p app/src/main/assets/licenses
cp LICENSE app/src/main/assets/licenses/MIT.txt
```

Then fetch the two standard texts verbatim:

- `app/src/main/assets/licenses/OFL-1.1.txt` — the SIL Open Font License 1.1 text, from <https://openfontlicense.org/documents/OFL.txt> or the `OFL.txt` in the upstream `google/fonts` `ofl/pixelifysans` directory. Verify it begins with `Copyright (c)` and contains the heading `SIL OPEN FONT LICENSE Version 1.1`.
- `app/src/main/assets/licenses/Apache-2.0.txt` — from <https://www.apache.org/licenses/LICENSE-2.0.txt>. Verify it begins with `                                 Apache License` and contains `Version 2.0, January 2004`.

Do not paraphrase, truncate, or reformat either file. A licence text that is not
the licence text does not satisfy the licence.

- [ ] **Step 2: Create the About screen**

Create `app/src/main/java/com/fountain/launcher/settings/AboutScreen.kt`:

```kotlin
package com.fountain.launcher.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fountain.launcher.BuildConfig
import com.fountain.launcher.data.FountainSettings
import com.fountain.launcher.data.SettingsRepository
import com.fountain.launcher.ui.theme.FountainPalette

private const val POLICY_URL = "https://blank204.github.io/fountain/privacy"

/**
 * Privacy policy, third-party licences, and consent status.
 *
 * The policy is read from a bundled asset rather than fetched, so it works with no
 * network — Fountain has no internet permission and should not need one to show its
 * own policy. PrivacyPolicyMirrorTest keeps that asset identical to the published one.
 */
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by remember { SettingsRepository(context).settings }
        .collectAsStateWithLifecycle(initialValue = FountainSettings())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FountainPalette.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = "←",
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono5,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 4.dp),
            )
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                color = FountainPalette.Mono6,
            )
        }

        Card {
            Text(
                "Fountain ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
                color = FountainPalette.Mono6,
            )
            Body("Fountain has no internet permission. It collects nothing and transmits nothing.")
        }

        Card {
            Heading("Permissions you've acknowledged")
            Body(
                "Accessibility disclosure: " +
                    if (settings.consentAccessibility) "acknowledged" else "not yet shown"
            )
            Body(
                "Notification disclosure: " +
                    if (settings.consentNotifications) "acknowledged" else "not yet shown"
            )
        }

        ExpandableCard("Privacy policy", assetName = "legal/privacy-policy.md")

        Card {
            Heading("Open the policy online")
            Body(POLICY_URL)
            Text(
                "Open in browser",
                style = MaterialTheme.typography.labelSmall,
                color = FountainPalette.MagentaHi,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(POLICY_URL))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
            )
        }

        ExpandableCard("Fountain — MIT License", assetName = "licenses/MIT.txt")
        ExpandableCard("Pixelify Sans — SIL Open Font License 1.1", assetName = "licenses/OFL-1.1.txt")
        ExpandableCard("AndroidX, Compose, Room, Kotlin — Apache License 2.0", assetName = "licenses/Apache-2.0.txt")
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FountainPalette.Surface)
            .padding(20.dp),
    ) { content() }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = FountainPalette.Mono6)
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = FountainPalette.Mono3,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** Collapsed by default — these texts are long and would bury everything else. */
@Composable
private fun ExpandableCard(title: String, assetName: String) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    // A missing licence asset must never crash the app.
    val text = remember(assetName, expanded) {
        if (!expanded) "" else runCatching {
            context.assets.open(assetName).bufferedReader().use { it.readText() }
        }.getOrElse { "Could not load $assetName." }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FountainPalette.Surface)
            .clickable { expanded = !expanded }
            .padding(20.dp),
    ) {
        Text(
            text = (if (expanded) "▾ " else "▸ ") + title,
            style = MaterialTheme.typography.bodyLarge,
            color = FountainPalette.Mono6,
        )
        if (expanded) Body(text)
    }
}
```

- [ ] **Step 3: Add the hub row**

In `SettingsHubScreen.kt`, add an `onAbout: () -> Unit` parameter after `onHidden`,
and a row after the "Hidden apps" `HubItem`:

```kotlin
        HubItem("About", "Privacy policy, licences, version.", onAbout)
```

- [ ] **Step 4: Route it in `MainActivity`**

Add `About` to the `Screen` enum:

```kotlin
private enum class Screen {
    Home, Onboarding, Settings, GatedApps, NotificationRules, Inbox, Behavior, HiddenApps, About
}
```

Add `onAbout = { screen = Screen.About },` to the `SettingsHubScreen(...)` call,
and add a branch alongside the other settings screens:

```kotlin
            Screen.About -> {
                BackHandler { screen = Screen.Settings }
                AboutScreen(onBack = { screen = Screen.Settings })
            }
```

Add `import com.fountain.launcher.settings.AboutScreen`.

- [ ] **Step 5: Verify build and tests**

Run:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Expected: `BUILD SUCCESSFUL`, 7 tests passing.

- [ ] **Step 6: Verify the licences are actually in the built artifact**

This is the step that proves OFL compliance — the licence must be in the
shipped binary, not just the repo:

```bash
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -i -E "licenses|legal"
```

Expected: four entries — `assets/licenses/MIT.txt`, `assets/licenses/OFL-1.1.txt`,
`assets/licenses/Apache-2.0.txt`, `assets/legal/privacy-policy.md`.

- [ ] **Step 7: Verify on device**

```bash
./gradlew :app:installDebug
```

Open Settings → About. Confirm: version string renders; both consent rows reflect
what you granted in Task 5; every collapsed section expands to real text; the
"Open in browser" link launches a browser.

- [ ] **Step 8: Confirm the removed permissions are absent from the built manifest**

```bash
./gradlew :app:processDebugMainManifest --console=plain
grep -E "USE_EXACT_ALARM|SYSTEM_ALERT_WINDOW|allowBackup" app/build/intermediates/merged_manifest/debug/AndroidManifest.xml
```

Expected: only `android:allowBackup="false"`. Neither permission should appear.
If `USE_EXACT_ALARM` shows up, a library is injecting it via manifest merge and
it must be removed with `tools:node="remove"`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/assets/licenses/ app/src/main/java/com/fountain/launcher/settings/AboutScreen.kt app/src/main/java/com/fountain/launcher/settings/SettingsHubScreen.kt app/src/main/java/com/fountain/launcher/app/MainActivity.kt
git commit -m "$(cat <<'EOF'
Add About screen with bundled licences and privacy policy

SIL OFL 1.1 requires the licence accompany the software wherever distributed,
which means inside the AAB rather than only in the repo. Bundles MIT, OFL-1.1
and Apache-2.0 texts as assets and surfaces them, the privacy policy, the app
version, and disclosure consent status under Settings → About.

The policy renders from a bundled asset so it needs no network, which suits an
app that deliberately has no internet permission.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01Y2YsDChGsKhNUJnvmTuWTS
EOF
)"
```

---

## Done criteria

All six tasks committed, and:

- `./gradlew :app:assembleDebug :app:testDebugUnitTest` passes.
- `grep -rn "canDrawOverlays\|openOverlaySettings\|USE_EXACT_ALARM" app/src/` returns nothing.
- The merged manifest shows `allowBackup="false"` and neither removed permission.
- The built artifact contains all three licence texts and the privacy policy.
- On device: the disclosure appears before either sensitive grant, and the home
  "finish setup" banner clears once permissions are granted.
- `https://blank204.github.io/fountain/privacy` resolves (Task 2, Step 6 — manual).

## What this plan deliberately does not do

- **targetSdk 34 → 36.** Sub-project 2. Play rejects new apps below API 35 today
  and requires API 36 from 2026-08-31. Needs an AGP and Gradle upgrade, plus an
  Android 16 emulator — the behaviour changes only manifest on Android 15/16
  devices, so the Note 9 cannot surface them.
- **Building or uploading the AAB.** Sub-project 3. Carries one irreversible
  decision: enrol in Play App Signing using the existing
  `keystore/fountain-release.jks` as the app signing key. If Google generates a
  fresh key, the signature will not match the GitHub APK and every existing user
  must uninstall — losing their gated apps and captured notifications — before
  installing from Play.
- **Setting a release `versionName`.** Currently `0.1.0`. Sub-project 3.
- **Enabling R8/minification.** `proguard-rules.pro` now exists so that it can be
  turned on without a build failure, but turning it on is sub-project 3's call.
- **Store listing copy, screenshots, feature graphic.** Marketing, not legal.
