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

Compose Multiplatform screens with immutable `UiState` data classes and one `ViewModel` per screen. Navigation uses Compose Navigation with type-safe routes (`Home`, `Drill`, `Results`). The `App` composable owns the `NavHost`. `AppModule` is the hand-rolled DI container passed down from the platform entry points.

Key files:
- `ui/App.kt` — `NavHost` wiring, `MaterialTheme` wrapper
- `ui/AppModule.kt` — DI container (holds `ContentRepository`, `ProgressRepository`, `AudioPlayer`, `QuestionFactory`)
- `ui/Routes.kt` — type-safe route objects (`Home`, `Drill`, `Results`)
- `ui/home/HomeScreen.kt` + `HomeViewModel.kt`
- `ui/drill/DrillScreen.kt` + `DrillViewModel.kt`
- `ui/results/ResultsScreen.kt`

### Domain (`domain/`)

Pure Kotlin. No framework imports.

- `Models.kt` — `Card`, `Sentence`, `ModuleMeta`, `Module`, `Question`, `CardProgress`, `QuestionMode`
- `QuestionFactory.kt` — builds `Question` objects; selects mode (LISTEN/TEXT/PRODUCE), picks distractors, shuffles options via injected `Random` for testability
- `MasteryCalculator.kt` — `isMastered(CardProgress?)` and `modulePercent(cardIds, progressMap)` — both pure functions

### Data (`data/`)

- `ContentRepository.kt` — reads bundled JSON from `composeResources/files/content/` using `Res.readBytes`; caches in memory; exposes `modules()` and `cards(moduleId)`
- `ProgressRepository.kt` — SQLDelight-backed; stores per-card answer history; exposes `recordAnswer`, `forCards`, `modulePercent`
- `DriverFactory.kt` — `expect class` with `createDriver(): SqlDriver`; platform `actual` implementations in `androidMain` and `iosMain`

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
