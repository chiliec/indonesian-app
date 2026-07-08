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
- iOS (full app): `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,id=057ACF07-A2C3-446D-A734-99AA3CB773AE' build`
- iOS (run on simulator): `xcrun simctl install <sim-id> <app-path> && xcrun simctl launch <sim-id> cx.viz.lancar`
- Tests: `./gradlew :composeApp:testDebugUnitTest`

## Environment requirements
Java 21 is required. Set JAVA_HOME before every Gradle command:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```
Do NOT use `$(/usr/libexec/java_home -v 21)` — it does not locate this Homebrew install.

Android SDK: `~/Library/Android/sdk` (standard Mac install). ANDROID_HOME is auto-detected
by the Android Gradle plugin; no explicit export needed unless the SDK is in a non-standard path.

Always run `./gradlew` from the repo root (`/Users/babin/Develop/Pet/indonesian-app/`).

## iOS-specific gotchas (learned during launch)

- **iOS host uses UIKit lifecycle** (`AppDelegate` + `SceneDelegate`), not SwiftUI `@main`.
  `SceneDelegate` sets `MainViewControllerKt.MainViewController()` as `window.rootViewController`.
  Do not revert to SwiftUI — CMP's PlistSanityCheck requires `UISceneDelegateClassName` in plist.
- **PlistSanityCheck is disabled** (`enforceStrictPlistSanityCheck = false` in `MainViewController`).
  CMP 1.8.0 has an NSArray OOB crash in its own check code on arm64 simulator. Keep disabled until
  a CMP upgrade resolves it.
- **`Info.plist` must have** `CADisableMinimumFrameDurationOnPhone = true` (CMP requirement) and
  `UIWindowSceneSessionRoleApplication` with `UISceneDelegateClassName` (UIKit scene lifecycle).
- **JSON deserialization on K/N**: use `ListSerializer(Card.serializer())` explicitly — do NOT use
  `decodeFromString<List<Card>>()`. The generic-inferred path crashes in the naming-strategy
  annotation lookup on arm64 (null deref in `kotlin.Any#equals`). Also set `useAlternativeNames =
  false` in the `Json` config.
- **SQLite**: `libsqlite3.tbd` must be in the Xcode Frameworks build phase.
- **Simulator ID**: iPhone 17 Pro = `057ACF07-A2C3-446D-A734-99AA3CB773AE`.

## Agent skills
Match this project's module name `composeApp` and package `com.axveer.lancar`.

## Out of scope (v1) — clean seams exist for later
Scenarios / Claude role-play, STT/TTS, accounts/sync, monetization, SRS scheduling,
sentence audio, card-browse screen.
