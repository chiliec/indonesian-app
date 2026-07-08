---
name: run-lancar
description: Build, launch, screenshot, and drive the Lancar iOS app on a booted iOS Simulator. Use when asked to run / start / build / launch / screenshot / drive / smoke-test Lancar (the Indonesian vocab trainer) on iOS, or to tap through its screens (home, drill, results).
---

# Run Lancar (iOS Simulator)

Lancar is an offline Kotlin Multiplatform + Compose vocab trainer. This skill
drives the **iOS** build on a booted simulator. The programmatic handle is
`.claude/skills/run-lancar/driver.py` — a wrapper over `xcrun simctl` +
[`idb`](https://fbidb.io) that builds, installs, launches, dumps the
accessibility tree, taps elements **by label**, and captures
auto-uprighted screenshots.

> **macOS + Xcode only.** There is no Linux/headless path — the app runs on
> the iOS Simulator. All paths below are relative to the repo root (`<unit>/`).
> The Android build (`./gradlew :composeApp:assembleDebug`) is out of scope here.

## Prerequisites (one-time)

```bash
# Java 21 — the Xcode build invokes a Gradle task to produce the KMP framework
brew install openjdk@21

# idb — drives the simulator (tap / describe / connect). CLI installs to
# ~/Library/Python/3.9/bin (NOT on PATH; the driver adds it automatically).
brew tap facebook/fb && brew trust facebook/fb
brew install idb-companion
pip3 install fb-idb
```

A booted simulator is required. The driver auto-detects the booted device and
falls back to iPhone 17 Pro (`057ACF07-A2C3-446D-A734-99AA3CB773AE`).

## Run (agent path) — use the driver

```bash
# Full smoke: boot + build + install + launch + screenshot to /tmp/lancar.png
python3 .claude/skills/run-lancar/driver.py up

# Or step by step:
python3 .claude/skills/run-lancar/driver.py boot        # boot sim + idb connect
python3 .claude/skills/run-lancar/driver.py build       # xcodebuild (sets JAVA_HOME)
python3 .claude/skills/run-lancar/driver.py install     # install newest built .app
python3 .claude/skills/run-lancar/driver.py launch      # terminate + relaunch Lancar
```

Drive the running app:

```bash
python3 .claude/skills/run-lancar/driver.py orientation           # PORTRAIT/LANDSCAPE + frames
python3 .claude/skills/run-lancar/driver.py describe              # tappable elements + tap coords
python3 .claude/skills/run-lancar/driver.py tap "Module 1"        # tap by label substring
python3 .claude/skills/run-lancar/driver.py screenshot /tmp/x.png # capture (auto-uprighted)
python3 .claude/skills/run-lancar/driver.py tapxy 201 243         # tap app-space point
```

**Verified end-to-end flow this session** (home → drill → continue):

```bash
python3 .claude/skills/run-lancar/driver.py launch
python3 .claude/skills/run-lancar/driver.py tap "Module 1"     # opens the drill (1/12)
python3 .claude/skills/run-lancar/driver.py describe           # read the 4 answer labels
python3 .claude/skills/run-lancar/driver.py tap "banana"       # tap an answer -> reveals "Lanjut →"
python3 .claude/skills/run-lancar/driver.py tap "Lanjut"       # advance to next question
python3 .claude/skills/run-lancar/driver.py tap "Selesai"      # (on 12/12) -> Results screen
```

Answer labels change every run — always `describe` first, then tap a label you
just saw. **Always `Read` the screenshot** to confirm state; a screenshot is
proof, a return code is not.

## Gotchas (the non-obvious traps)

- **`idb` is not on PATH.** The CLI installs to `~/Library/Python/3.9/bin`. The
  driver prepends it; if you call `idb` by hand, add it yourself.
- **A second app shares this simulator: `cx.viz.slovo` (display name "iosApp").**
  It's an unrelated Russian-learning app and can end up foregrounded. Always
  `launch` (which relaunches `cx.viz.lancar`) before driving. Both apps' process
  name is `iosApp`, so `~/Library/Logs/DiagnosticReports/iosApp-*.ips` crash logs
  are **ambiguous** — check the `procPath` container UUID against
  `xcrun simctl get_app_container <udid> cx.viz.lancar` to attribute a crash.
- **Tap coordinate space ≠ AX frame space.** `idb ui tap` takes points in the
  device's native **portrait** space (402×874). `idb ui describe-all` reports
  frames in the app's **current orientation**. Lancar is portrait-locked, so
  they match (identity). If the app ever renders landscape (frame width > height),
  the driver rotates taps via `(ax, ay) -> (ay, W_app - ax)`; a raw `idb ui tap`
  with landscape AX coords lands off-screen and silently does nothing.
- **`simctl io screenshot` captures the native portrait buffer.** If the app is
  landscape the PNG comes out rotated 90°. The driver runs `sips -r 90` to
  upright it automatically; a manual screenshot of a landscape app needs the
  same.
- **No Screen Recording / Accessibility permission for the CLI shell.**
  `screencapture` fails ("could not create image from display") and AppleScript
  keystrokes to Simulator (e.g. rotate via ⌘←) may be silently dropped. Drive
  **only** through `idb` / `simctl`, never through host screen automation.
- **Reinstalling resets running state.** `install` over a running app drops it
  off whatever screen it was on; `launch` afterward to get a clean cold start.
- **PlistSanityCheck is disabled** in `MainViewController` (`enforceStrictPlistSanityCheck = false`)
  to dodge a CMP 1.8.0 arm64-sim crash. If you re-enable it and the app SIGABRTs
  at launch, that's why — the backtrace points at `PlistSanityCheck.performIfNeeded`.

## Run (human path)

`open -a Simulator`, then in Xcode run the `iosApp` scheme (or `xcodebuild ... build`
then `simctl install`/`launch`). A window opens; useless for automated driving.

## Test

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew :composeApp:testDebugUnitTest
```

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Unable to lookup in current state: Shutdown` | Simulator shut down. `driver.py boot`. |
| `idb` errors / `No Companion Connected` | `idb connect <udid>` (the driver's `boot` does this). |
| Build fails in a Gradle/`embedAndSign` step | `JAVA_HOME` not Java 21. The driver sets it; by hand use `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` (NOT `java_home -v 21`). |
| `tap` does nothing, app doesn't react | Coord-space mismatch — app is landscape. Confirm with `orientation`; the driver handles it, raw `idb ui tap` does not. |
| `describe` returns nothing | No foreground app or sim not connected — `launch` then `boot`/`idb connect`. |
