# Play Store listing — draft copy and assets

Everything Play asks for in the "Main store listing" section. Copy is a starting
point, not gospel — rewrite in your own voice.

## Assets in this folder

| Asset | File | Play requirement | Status |
|---|---|---|---|
| App icon | `icon-512.png` | 512×512 32-bit PNG | ready — rendered from `ic_launcher_foreground.xml`, pixel-exact |
| Feature graphic | `feature-graphic-1024x500.png` | 1024×500 PNG/JPG | ready — uses Pixelify Sans and the app palette |
| Phone screenshots | `screenshots/*.png` | 2–8, 320–3840 px | 5 supplied, captured on the Galaxy Note 9 |

**Before uploading `02-app-list.png`, look at it.** It shows the real app list from
your phone, so it names software you have installed. Harmless, but it is your
data on a public store page — retake it on a device with a tidier app list if
you'd rather not publish that.

`05-time-gate-landscape.png` is landscape while the rest are portrait. Play accepts
mixed orientations, but a consistent set looks better; consider recapturing the
time-gate screen in portrait and dropping this one.

## App name

```
Fountain
```

## Short description (max 80 characters)

```
A pixel launcher with per-app time limits. No internet, no tracking.
```

## Full description (max 4000 characters)

```
Fountain is a minimalist Android launcher built around one idea: you should
decide how long you spend in an app before you open it, not after.

TIME GATES
Mark any app as gated. Opening it asks how long you want — 1, 5 or 10 minutes.
When the time is up, Fountain sends you back to the home screen. There is no
running total to game and no streak to protect; every time you open the app, you
make the choice again.

A QUIETER HOME SCREEN
The app list is monochrome A–Z with a search box. No feeds, no suggestions, no
badges competing for attention. Icons are rendered in greyscale so nothing pulls
your eye more than anything else.

A NOTIFICATION INBOX YOU CONTROL
Choose, per app, whether notifications pass through untouched, get collected into
Fountain's own inbox to read when you want, or are suppressed entirely.

A LOCK SCREEN WITH A FOUNTAIN ON IT
The home surface sits behind a pixel-art fountain, with the clock, the date, and
an optional CRT scanline overlay.

PRIVACY — THE SHORT VERSION
Fountain does not request the internet permission. That is not a promise to
behave; the app is technically incapable of sending your data anywhere. There are
no accounts, no analytics, no advertising, and no third-party SDKs of any kind.
Everything Fountain stores — your gated apps, session history, notification rules
— stays in its own storage on your device. Cloud backup is switched off.

The source is public and MIT-licensed, so you can check any of this yourself:
github.com/blank204/fountain

PERMISSIONS, AND WHY
Fountain explains each of these in the app before it asks for it, and works with
reduced functionality if you decline.

• Accessibility — reads only the package name of the app currently in front, so
  it can notice when a gated app opens and return you home when time is up. It
  cannot read screen content or anything you type.
• Notification access — needed to collect notifications into Fountain's inbox.
• Query all packages — a launcher needs the list of installed apps to be a
  launcher.
• Exact alarms — ends a session on time even when the device is dozing.
• Device administrator (screen lock only) — powers the optional double-tap-to-lock
  gesture. You can turn it off from inside Fountain at any time.

Requires Android 10 or later.
```

## Other listing fields

- **App category:** Personalization (Fountain is a home-screen replacement)
- **Tags:** launcher, productivity, digital wellbeing, minimalist
- **Contact email:** gshriadhithya@gmail.com
- **Website:** https://github.com/blank204/fountain
- **Privacy policy:** https://blank204.github.io/fountain/privacy

## Not yet done

- **Video** — optional, skip it.
- **Tablet / other form-factor screenshots** — optional. Skipping means Play may
  warn the listing is not optimised for tablets; harmless for a phone launcher.
