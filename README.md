# Lancar

Lancar is a Kotlin Multiplatform + Compose Multiplatform flashcard app for learning Indonesian. It bundles 1,796 vocabulary cards across 8 modules with audio, and presents them as multiple-choice listen/read/produce drills that run entirely offline on Android and iOS.

## Install (Android)

Scan the QR code or open the [latest release](https://github.com/chiliec/indonesian-app/releases/latest) on your phone, download the APK, and tap to install. Requires **Android 7.0+**. Fully offline — no account, no network, no data collected.

<img src="docs/install-qr.png" width="200" alt="QR code to the latest Lancar Android release" />

> On first install, Android asks you to allow "install unknown apps" for your browser — that's expected for apps distributed outside the Play Store.

> iOS is not yet distributed (requires an Apple Developer account — see [`docs/release-ios.md`](docs/release-ios.md)).

**Privacy:** Lancar collects no data and works fully offline — [privacy policy](https://chiliec.github.io/indonesian-app/privacy.html).

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 21 (required) |
| Android Studio | Ladybug or later, with Android SDK |
| Xcode | 15+ (macOS only) |
| Node.js | 20+ (content pipeline only) |
| ffmpeg | any recent release (content pipeline only) |

Set JAVA_HOME before building:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

## Run Android

Start an emulator or connect a device, then:

```bash
./gradlew :composeApp:installDebug
```

## Run iOS

Open `iosApp/iosApp.xcodeproj` in Xcode, select a simulator, and press Run (⌘R).

Alternatively, build the framework only:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Test

```bash
./gradlew :composeApp:testDebugUnitTest
```

11 tests covering: `QuestionFactory` (×5), `MasteryCalculator` (×3), `ProgressRepository` (×2), `DrillFlowTest` (×1).

## Regenerate content

The generated JSON and m4a files are committed. Only re-run this when the upstream YAML or audio changes:

```bash
cd content-prep
npm install
npm run prep
```

Requires ffmpeg on PATH and Node 20+. Source content lives at `../indonesian/content/quiz/`.

## Docs

- [Architecture](docs/architecture.md) — layer diagram, dependency rules, platform seams
- [Content pipeline](docs/content-pipeline.md) — source format, JSON contract, transcode step
- [Design import](docs/design-import.md) — how to pull the visual reference into the project
