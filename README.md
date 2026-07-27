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
