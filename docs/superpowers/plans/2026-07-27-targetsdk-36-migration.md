# targetSdk 36 Migration — Design & Plan

> **For agentic workers:** Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move Fountain from `compileSdk`/`targetSdk` 34 to 36 so Play will accept the AAB, and fix the behaviour changes that bump turns on.

**Architecture:** Two independent bumps, deliberately sequenced apart — toolchain first at the current SDK, then the SDK itself. If the build breaks, that ordering tells you which change did it. Then one real code fix (edge-to-edge), then verification on an Android 16 emulator, because none of the risky behaviour changes are observable on the Note 9.

**Sub-project 2 of 3.** Sub-project 1 (legal package) is merged. Sub-project 3 is the signed AAB.

## Why this is required

Play requires new apps to target API 35 today, and **API 36 from 2026-08-31** — 35 days out. Fountain is at 34, so a submission would be rejected outright.

## Global Constraints

- **Minimal churn.** Bump AGP, Gradle, and the two SDK levels. Do **not** modernise Kotlin, KSP, or the Compose BOM unless a build failure forces it — each is a separate risk surface and none is required for Play acceptance.
- Do not change `versionCode` / `versionName` — that is sub-project 3.
- Do not change app behaviour beyond what edge-to-edge enforcement requires.
- `minSdk` stays 29 (Note 9 / Android 10).

## Verified prerequisites

Checked on this machine before planning — no downloads required:

| Requirement | Status |
|---|---|
| AGP 8.13.0 needs Gradle ≥ 8.13, JDK ≥ 17, Build Tools ≥ 35.0.0, max compileSdk 36.1 | authoritative, from AGP 8.13.0 release notes |
| SDK Platform `android-36` | installed |
| Build Tools `36.1.0` | installed |
| JDK | 17.0.12 (Temurin) |
| Android 36.1 emulator system image (`google_apis_playstore`, x86_64) | installed |
| 16 KB page-size compliance | **already satisfied** — see below |

### 16 KB page size — already resolved, do not "fix"

Play blocks releases targeting Android 15+ that ship native libraries not aligned to
16 KB. Fountain does ship one, via DataStore:
`lib/*/libdatastore_shared_counter.so` (androidx.datastore 1.1.1).

It was checked directly by parsing the arm64 ELF program headers. Every `PT_LOAD`
segment reports `p_align = 16384`. **The requirement is met.** Public reports of this
library being unaligned refer to other versions; do not upgrade DataStore on their
basis. If the dependency ever changes, re-run the check rather than assuming.

## Behaviour changes this bump turns on

| Change | Exposure | Action |
|---|---|---|
| **Edge-to-edge enforced** (API 35+; the `windowOptOutEdgeToEdgeEnforcement` escape hatch is gone at 36) | `MainActivity` already calls `enableEdgeToEdge()` and wraps content in `systemBarsPadding()` — safe. **`GateActivity` does neither**, and `GateScreen` applies only `.padding(28.dp)`. | Task 3 — real fix required |
| **Predictive back on by default** at targetSdk 36 | `MainActivity` uses `BackHandler` throughout, including a no-op `BackHandler(enabled = true) {}` to swallow back while locked | Task 4 — verify on emulator |
| **Elegant text height** defaults true (API 35+) | Pixel font line metrics may shift | Task 4 — visual check |
| 16 KB page size | native lib already aligned | none |

## Task 1: Toolchain bump only

Bump AGP and Gradle while holding `compileSdk` at 34, so any toolchain breakage is isolated from SDK breakage.

**Files:** `build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Bump the Gradle wrapper**

In `gradle/wrapper/gradle-wrapper.properties`, change `distributionUrl` from
`gradle-8.7-bin.zip` to `gradle-8.13-bin.zip`.

- [ ] **Step 2: Bump AGP**

In the root `build.gradle.kts`, change the `com.android.application` plugin version
from `8.5.2` to `8.13.0`. Leave the Kotlin and KSP plugin versions untouched.

- [ ] **Step 3: Build at the old SDK**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Expected: `BUILD SUCCESSFUL`, 7 tests pass. A failure here is purely a toolchain
problem — do not touch `compileSdk` to work around it.

- [ ] **Step 4: Commit**

```bash
git add build.gradle.kts gradle/wrapper/gradle-wrapper.properties
git commit -m "Upgrade AGP to 8.13.0 and Gradle to 8.13"
```

## Task 2: compileSdk and targetSdk to 36

**Files:** `app/build.gradle.kts`

- [ ] **Step 1: Bump both SDK levels**

In `app/build.gradle.kts`, set `compileSdk = 36` and `targetSdk = 36`. Leave
`minSdk = 29`.

- [ ] **Step 2: Build and test**

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Expected: `BUILD SUCCESSFUL`, 7 tests pass. Deprecation warnings are acceptable;
errors are not.

- [ ] **Step 3: Confirm the built manifest reports 36**

```bash
./gradlew :app:processReleaseMainManifest --console=plain
grep -o 'targetSdkVersion="[0-9]*"' app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
```

Expected: `targetSdkVersion="36"`.

- [ ] **Step 4: Re-confirm 16 KB alignment survived the toolchain change**

Re-extract `lib/arm64-v8a/libdatastore_shared_counter.so` from the debug APK and
re-check that every `PT_LOAD` segment has `p_align >= 16384`. A new AGP repackages
native libs, so this must be re-verified, not assumed.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts
git commit -m "Target Android 16 (API 36)"
```

## Task 3: Edge-to-edge correctness for the gate

`GateActivity` is the app's most important surface — it is what a user sees every time a
time-gated app opens. Under enforced edge-to-edge its content draws under the status and
navigation bars.

**Files:** `app/src/main/java/com/fountain/launcher/gate/GateActivity.kt`, `app/src/main/java/com/fountain/launcher/gate/GateScreen.kt`

- [ ] **Step 1: Audit every Activity for edge-to-edge handling**

```bash
grep -rn "class .*Activity" app/src/main/java --include=*.kt
grep -rn "enableEdgeToEdge\|systemBarsPadding\|safeDrawingPadding" app/src/main/java --include=*.kt
```

Record which Activities call `enableEdgeToEdge()` and which of their content roots apply
insets. Every Activity that hosts Compose content needs both.

- [ ] **Step 2: Add edge-to-edge handling to `GateActivity`**

Call `enableEdgeToEdge()` before `super.onCreate(...)`, matching `MainActivity`'s
pattern, and add the `androidx.activity.enableEdgeToEdge` import.

- [ ] **Step 3: Apply insets to the gate content**

Add `.systemBarsPadding()` to `GateScreen`'s root modifier, before its existing
`.padding(28.dp)`, importing `androidx.compose.foundation.layout.systemBarsPadding`.
Order matters: system-bar insets first, then the design padding.

- [ ] **Step 4: Build**

```bash
./gradlew :app:assembleDebug --console=plain
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fountain/launcher/gate/
git commit -m "Handle enforced edge-to-edge on the gate surface"
```

## Task 4: Verify on an Android 16 emulator

The whole reason this sub-project was separated. None of the above is observable on the
Note 9 (Android 10) — the behaviour changes only take effect when running on Android
15/16.

- [ ] **Step 1: Create the AVD**

```bash
"$SDK/cmdline-tools/latest/bin/avdmanager.bat" create avd \
  -n fountain-api36 \
  -k "system-images;android-36.1;google_apis_playstore;x86_64" \
  -d pixel_6 --force
```

- [ ] **Step 2: Boot it and wait for readiness**

Launch headless-friendly, then poll `sys.boot_completed` until it reports `1`.

- [ ] **Step 3: Install**

```bash
./gradlew :app:installDebug
```

- [ ] **Step 4: Verify**

1. App launches without crashing on API 36.
2. **Gate screen:** trigger it and confirm its content is not under the status or
   navigation bar. This is the regression Task 3 fixes — check it deliberately.
3. **Home/settings/about:** content correctly inset; lock overlay still intentionally
   full-bleed.
4. **Predictive back:** back gesture behaves; the lock screen still swallows back.
5. **Text:** pixel font renders without clipped line metrics.

- [ ] **Step 5: Capture evidence**

Screenshot the gate screen and the home screen via `adb exec-out screencap -p`, so the
edge-to-edge result is recorded rather than asserted.

## Done criteria

- `assembleDebug` + `testDebugUnitTest` green at compileSdk 36.
- Merged release manifest reports `targetSdkVersion="36"`.
- Native lib still 16 KB aligned after the AGP change.
- App runs on an Android 16 emulator with no content under system bars.

## Out of scope

- Kotlin, KSP, Compose BOM, and AndroidX version bumps — unnecessary for Play acceptance and each is its own risk surface.
- AGP 9.x — 8.13.0 supports compileSdk 36.1, which is all that is needed.
- Building the AAB, Play App Signing, release versioning — sub-project 3.
