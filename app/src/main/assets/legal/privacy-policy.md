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
