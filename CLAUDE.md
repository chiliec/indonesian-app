# Lancar — Indonesian Vocabulary Trainer (KMP)

Offline iOS + Android vocabulary trainer. Kotlin Multiplatform + Compose Multiplatform.
No backend, no network at runtime. Content and audio are bundled resources.

## Architecture (enforced)
UI (Compose, immutable UiState, one ViewModel per screen, unidirectional data flow)
→ domain (pure Kotlin: QuestionFactory, MasteryCalculator)
→ data (ContentRepository = bundled JSON; ProgressRepository = SQLDelight)
→ platform (AudioPlayer + SQLDelight DriverFactory via expect/actual, androidMain/iosMain only).

- `domain` imports nothing from ui/data/platform/Android/iOS/Compose.
- All randomness goes through an injected `kotlin.random.Random`.
- Mastery: card mastered when `correct > 0`. Module % = mastered/total.
- Audio is `.m4a` only. Regenerate content via `content-prep` (see docs/content-pipeline.md).

## Build & test
- Android: `./gradlew :composeApp:assembleDebug`
- iOS (framework only): `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- iOS (full app): `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=32623728-2A40-4964-912D-252369F2692D' build`
- iOS (run on simulator): `xcrun simctl install <sim-id> <app-path> && xcrun simctl launch <sim-id> cx.viz.lancar`
- Tests: `./gradlew :composeApp:testDebugUnitTest`

## Environment requirements
Java 21 is required. Set JAVA_HOME before every Gradle command:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```
Do NOT use `$(/usr/libexec/java_home -v 21)` — it does not locate this Homebrew install.

Android SDK: `/opt/homebrew/share/android-commandlinetools` (Homebrew install; set in
`local.properties` as `sdk.dir`). ANDROID_HOME is auto-detected from `local.properties`
by the Android Gradle plugin; no explicit export needed.

Always run `./gradlew` from the repo root (`/Users/babin/Develop/Pet/indonesian-app/`).

## iOS-specific gotchas (learned during launch)

- **iOS host uses UIKit lifecycle** (`AppDelegate` + `SceneDelegate`), not SwiftUI `@main`.
  `SceneDelegate` sets `MainViewControllerKt.MainViewController()` as `window.rootViewController`.
  Do not revert to SwiftUI — CMP's PlistSanityCheck requires `UISceneDelegateClassName` in plist.
- **PlistSanityCheck is enabled** (default). The NSArray OOB crash in CMP's own check code on
  arm64 simulator was a **CMP 1.8.0** bug; **fixed by bumping to CMP 1.8.2**. `MainViewController`
  no longer passes `enforceStrictPlistSanityCheck = false`. Verified: app launches on the sim.
- **`Info.plist` must have** `CADisableMinimumFrameDurationOnPhone = true` (CMP requirement) and
  `UIWindowSceneSessionRoleApplication` with `UISceneDelegateClassName` (UIKit scene lifecycle).
- **JSON deserialization on K/N**: use `ListSerializer(Card.serializer())` explicitly — do NOT use
  `decodeFromString<List<Card>>()`. The generic-inferred path crashes in the naming-strategy
  annotation lookup on arm64 (null deref in `kotlin.Any#equals`). Also set `useAlternativeNames =
  false` in the `Json` config.
- **SQLite**: `libsqlite3.tbd` must be in the Xcode Frameworks build phase.
- **Simulator ID**: iPhone 16 Pro (iOS 26.4) = `32623728-2A40-4964-912D-252369F2692D`.
  (Earlier IDs `7E679B15…`/`057ACF07…` were wiped in a disk cleanup — sims aren't stable across resets.)
- **`open -a Simulator` before `simctl launch`/`io screenshot`** — a headless (never-foregrounded)
  sim errors "Timeout waiting for screen surfaces" and `launch` hangs.

## Agent skills
Match this project's module name `composeApp` and package `cx.viz.lancar`.

## Shipped features
- **Onboarding** (2-step: welcome + optional name) — gates first cold start
- **App shell** (2-tab floating pill bar: Beranda + Profil)
- **Profile** — edit name, pick accent color (terracotta/green/blue), reset mastery, replay onboarding, about
- **Accent theming** — `Accent` enum + `LocalAccentColor` CompositionLocal; persisted via `SettingsRepository`

## Out of scope (still deferred) — clean seams exist for later
Scenarios / Claude role-play, STT/TTS, accounts/sync, monetization, SRS scheduling,
sentence audio, card-browse screen (Kartu tab).
