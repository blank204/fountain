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

**Fountain does not resist uninstallation.** The admin can be revoked from inside
the app — first-run setup shows a "Turn off" action on the double-tap-to-lock row
whenever the admin is active, which calls `removeActiveAdmin`. Verified on device:
with the admin active, uninstalling fails with
`DELETE_FAILED_DEVICE_POLICY_MANAGER`; after using the in-app "Turn off", the same
uninstall succeeds. Users are never forced into system settings to remove Fountain.

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
