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
- iOS: open `iosApp/iosApp.xcodeproj` in Xcode, or
  `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
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

## Agent skills
This repo is scaffolded for the kotlin-kmp-claude-agent-skills toolkit (to be installed under
`.claude/` per its README at https://github.com/mmiani/kotlin-kmp-claude-agent-skills).
Use the `kotlin-*` skills for architecture/state/UI/data/testing/build reviews and the
`execute-ticket` pipeline for ticketed work. Match this project's module name `composeApp`
and package `com.axveer.lancar`.

## Out of scope (v1) — clean seams exist for later
Scenarios / Claude role-play, STT/TTS, accounts/sync, monetization, SRS scheduling,
sentence audio, card-browse screen.
