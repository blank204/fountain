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
