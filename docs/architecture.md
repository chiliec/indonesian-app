# Architecture

## Layer diagram

```
UI (Compose, immutable UiState, one ViewModel per screen, unidirectional data flow)
→ domain (pure Kotlin: QuestionFactory, MasteryCalculator, Models)
→ data (ContentRepository = bundled JSON; ProgressRepository = SQLDelight)
→ platform (AudioPlayer + DriverFactory via expect/actual — androidMain / iosMain only)
```

## Dependency rule

Each layer may only import from the layer immediately below it. The `domain` layer has zero external dependencies — no Android, no iOS, no coroutines beyond stdlib.

## Four layers

### UI (`ui/`)

Compose Multiplatform screens with immutable `UiState` data classes and one ViewModel per screen. Navigation uses Compose Navigation with type-safe routes (`Home`, `Drill`, `Results`). The `App` composable owns the `NavHost`. `AppModule` is the hand-rolled DI container passed down from the platform entry points.

**ViewModels are not subclasses of `androidx.lifecycle.ViewModel`.** They own a `CoroutineScope(SupervisorJob() + Dispatchers.Main)` directly and expose a `dispose()` function. The host screen calls `DisposableEffect(vm) { onDispose { vm.dispose() } }` to cancel the scope on leave. This avoids the `lifecycle-viewmodel` dependency on K/N.

Key files:
- `ui/App.kt` — `NavHost` wiring; gates on `onboardingSeen`; wraps in `LancarTheme(accent=…)`
- `ui/AppModule.kt` — DI container (`ContentRepository`, `ProgressRepository`, `SettingsRepository`, `AudioPlayer`, `QuestionFactory`); owns `accent: StateFlow<Accent>` and `setAccent()`
- `ui/Routes.kt` — type-safe route objects: `Onboarding`, `Main`, `Drill`, `Results`; `startDestination(onboardingSeen)` gate function
- `ui/onboarding/OnboardingScreen.kt` + `OnboardingViewModel.kt` — 2-step welcome + name flow
- `ui/main/MainScaffold.kt` — floating pill tab bar hosting Beranda + Profil tabs
- `ui/home/HomeScreen.kt` + `HomeViewModel.kt` — greets by name; uses `LocalAccentColor`
- `ui/profile/ProfileScreen.kt` + `ProfileViewModel.kt` — name edit, accent picker, reset, replay, about
- `ui/drill/DrillScreen.kt` + `DrillViewModel.kt` — full-screen over Main; uses `LocalAccentColor`
- `ui/results/ResultsScreen.kt` — uses `LocalAccentColor`
- `ui/theme/Accent.kt` — `enum class Accent(color)` (TERRACOTTA/GREEN/BLUE) + `fromName()`; `LocalAccentColor` CompositionLocal in `Theme.kt`

### Domain (`domain/`)

Pure Kotlin. No framework imports.

- `Models.kt` — `Card`, `Sentence`, `ModuleMeta`, `Module`, `Question`, `CardProgress`, `QuestionMode`
- `QuestionFactory.kt` — builds `Question` objects; selects mode (LISTEN/TEXT/PRODUCE), picks distractors, shuffles options via injected `Random` for testability
- `MasteryCalculator.kt` — `isMastered(CardProgress?)` and `modulePercent(cardIds, progressMap)` — both pure functions

### Data (`data/`)

- `ContentRepository.kt` — reads bundled JSON from `composeResources/files/content/` using `Res.readBytes`; caches in memory; exposes `modules()` and `cards(moduleId)`
- `ProgressRepository.kt` — SQLDelight-backed; stores per-card answer history; exposes `recordAnswer`, `forCards`, `modulePercent`, `reset()`
- `SettingsRepository.kt` — SQLDelight-backed KV store (`app_settings` table); exposes `displayName`, `setDisplayName`, `onboardingSeen`, `markOnboardingSeen`, `accentName`, `setAccentName`
- `DriverFactory.kt` — `expect class` with `createDriver(): SqlDriver`; platform `actual` implementations in `androidMain` and `iosMain`

**Schema note:** Adding the `app_settings` table (Task 1) bumped the logical schema. Existing installs require a clean reinstall to get the new table — SQLDelight 2.0.2's Gradle DSL does not expose a `schemaVersion` setter, so formal migrations are deferred until a SQLDelight upgrade.

### iOS entry point

`iosApp/iosApp/` uses UIKit lifecycle — **not SwiftUI**:

- `AppDelegate.swift` — `@UIApplicationMain`, empty body (UIKit needs this class)
- `SceneDelegate.swift` — creates a `UIWindow`, sets `MainViewControllerKt.MainViewController()` as `rootViewController`
- `iOSApp.swift` — comment-only; kept to satisfy Xcode project references

`MainViewController.kt` (iosMain) wraps the Compose `App` in a `ComposeUIViewController`. The `LancarDatabase` and `AppModule` are created inside `remember {}` blocks so they survive recomposition.

### Platform (`platform/`)

Code that cannot be shared. Each seam is defined in `commonMain` and implemented in `androidMain` and `iosMain`.

## Platform seams

### AudioPlayer

Defined as an interface in `commonMain/platform/AudioPlayer.kt`:

```kotlin
interface AudioPlayer {
    suspend fun play(fileName: String)
}
```

`NoopAudioPlayer` (also in `commonMain`) is a silent stub used during tests and as a fallback.

| Source set | Implementation | Backing API |
|---|---|---|
| `androidMain` | `AudioPlayer.android.kt` | `MediaPlayer` |
| `iosMain` | `AudioPlayer.ios.kt` | `AVAudioPlayer` |

### DriverFactory

Defined as an `expect class` in `commonMain/data/DriverFactory.kt`:

```kotlin
expect class DriverFactory {
    fun createDriver(): SqlDriver
}
```

| Source set | Implementation | Driver |
|---|---|---|
| `androidMain` | `DriverFactory.android.kt` | `AndroidSqliteDriver` |
| `iosMain` | `DriverFactory.ios.kt` | `NativeSqliteDriver` |

## Deferred-feature seams

The following are intentionally absent from v1 and have placeholder stubs to mark the gap:

- **STT / TTS** — no speech input or output; `PRODUCE` mode uses multiple-choice only
- **SRS (spaced repetition)** — mastery is a simple correct-count threshold; no scheduling algorithm
- **Sync** — progress is local-only; no cloud backend
- `NoopAudioPlayer` is the canonical seam marker for audio: it compiles and tests cleanly while doing nothing
