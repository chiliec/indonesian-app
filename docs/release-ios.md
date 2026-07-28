# Lancar — iOS / App Store Release Runbook

How to cut an App Store (and TestFlight) release of Lancar. This is the **iOS**
runbook; store copy (shared with Android) lives in
[`store-listing.md`](store-listing.md). The Android counterpart is
[`release-android.md`](release-android.md).

> Status (2026-07-28): **SHIPPED to TestFlight.** Build 1 of `1.0.2` was archived
> and uploaded by CI (§10) and is **VALID** in App Store Connect (App ID
> `6795209576`, Team `7JF6XQC536`) — the privacy-manifest validator passed. All
> prep (usage strings, UIKit scene lifecycle, `ITSAppUsesNonExemptEncryption`,
> bundled `PrivacyInfo.xcprivacy` §4, `ExportOptions.plist` §6, live privacy-policy
> URL §8, 6.9" screenshots §7) is in place. **Releases are now automated** — push a
> `v*` tag to ship a build (§10). The only manual step left is a one-time
> **internal-tester assignment** in the App Store Connect TestFlight UI.

> **iOS has no free sideload.** Unlike Android — where a signed `.apk` installs
> directly (see `release-android.md`) — there is **no way to hand an iPhone user a
> file**. Reaching real users requires either **TestFlight** or the **App Store**,
> both of which need the paid membership. Ad-hoc distribution also needs the
> membership *plus* every tester's device UDID registered. There is no equivalent
> to the Android APK for iOS.

---

## 0. One-time prerequisites (need a human + money)

1. **Apple Developer Program** — US$99/yr. https://developer.apple.com/programs/
   Enroll as an individual or organization; identity verification can take a day
   or two. This yields a **Team ID** (needed in §2).
2. **App Store Connect record** — https://appstoreconnect.apple.com → My Apps → **+**.
   - Name: `Lancar` · Primary language: English (U.S.) · Bundle ID: **`cx.viz.lancar`**
     (register it first under Certificates, IDs & Profiles → Identifiers, or let
     Xcode's automatic signing create it) · SKU: `lancar` (any unique string).
   - **DONE (2026-07-28):** record created. **Apple App ID: `6795209576`**
     (Team ID `7JF6XQC536`).

Nothing below reaches TestFlight or the store until step 0 is done.

---

## 1. Machine / toolchain

- **Xcode** (matching the CMP/Kotlin toolchain — the project builds today under the
  Xcode used for the sim smoke tests). App Store uploads must be built with a
  current Xcode; Apple periodically raises the minimum SDK.
- **Java 21** + `JAVA_HOME` — the Xcode build shells out to Gradle to produce the
  `ComposeApp.framework`. Export it before any command that touches the framework
  (see repo `CLAUDE.md`).
- The iOS host uses the **UIKit** lifecycle (`AppDelegate` + `SceneDelegate`), not
  SwiftUI `@main` — required by CMP's `PlistSanityCheck`. Do not revert to SwiftUI.

---

## 2. Signing (needs the paid account)

The project uses **automatic signing** (`CODE_SIGN_STYLE = Automatic`).
`DEVELOPMENT_TEAM` is currently **empty** in `project.pbxproj` — this is the one
build-setting change the account unblocks:

1. Open `iosApp/iosApp.xcodeproj` in Xcode → target **iosApp** → **Signing &
   Capabilities**.
2. Check **Automatically manage signing** and pick your **Team** (the paid
   membership's team). Xcode fills `DEVELOPMENT_TEAM`, creates the App ID for
   `cx.viz.lancar`, and provisions a distribution certificate + profile.
3. Commit the resulting `DEVELOPMENT_TEAM = <TEAMID>;` change if you want CI/other
   machines to archive without re-selecting the team. (Safe to commit — the Team ID
   is not a secret; the signing *certificate* and its private key stay in the
   Keychain and are never in git.)

There is no gitignored secrets file on iOS (unlike Android's `keystore.properties`)
— the signing identity lives in the login Keychain / Apple's servers.

---

## 3. Versioning

Set in the Xcode target build settings (both configs):

- `MARKETING_VERSION` — user-facing version. Currently **`1.0`**. This maps to
  `CFBundleShortVersionString`. Keep in sync with Android `versionName`.
- `CURRENT_PROJECT_VERSION` — build number. Currently **`1`**. Maps to
  `CFBundleVersion`. **Must strictly increase** for every TestFlight/App Store
  upload of the same `MARKETING_VERSION` (bump to `2`, `3`, … for re-uploads).

Both are already wired through `Info.plist` via `$(MARKETING_VERSION)` /
`$(CURRENT_PROJECT_VERSION)` — edit the build setting, not the plist.

---

## 4. Privacy manifest — `PrivacyInfo.xcprivacy` (⚠️ still to create)

App Store submissions **require** a privacy manifest. Lancar collects nothing and
uses no network, so the data-collection section is empty; the only entries needed
are **required-reason API** declarations for what the KMP/SQLite stack touches.

Create `iosApp/iosApp/PrivacyInfo.xcprivacy` and **add it to the iosApp target's
"Copy Bundle Resources"** build phase (in Xcode: drag it into the project, tick the
iosApp target). Starting point:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>NSPrivacyTracking</key><false/>
  <key>NSPrivacyTrackingDomains</key><array/>
  <key>NSPrivacyCollectedDataTypes</key><array/>
  <key>NSPrivacyAccessedAPITypes</key>
  <array>
    <!-- SQLite reads/writes file modification times -->
    <dict>
      <key>NSPrivacyAccessedAPIType</key>
      <string>NSPrivacyAccessedAPICategoryFileTimestamp</string>
      <key>NSPrivacyAccessedAPITypeReasons</key>
      <array><string>C617.1</string></array>
    </dict>
    <!-- SQLite / free-space checks -->
    <dict>
      <key>NSPrivacyAccessedAPIType</key>
      <string>NSPrivacyAccessedAPICategoryDiskSpace</string>
      <key>NSPrivacyAccessedAPITypeReasons</key>
      <array><string>E174.1</string></array>
    </dict>
  </array>
</dict>
</plist>
```

> **Finalize against the upload validation.** The exact required-reason list depends
> on what the compiled framework + its transitive deps call. Apple's validator (and
> the "Missing privacy manifest" / "required reason" emails after a TestFlight
> upload) will name any category you must add or may remove. Treat the block above
> as a starting point, upload once, and adjust. Data-collection stays empty — the
> app is fully offline and stores progress/settings only on-device (SQLDelight).

- **Data collection:** none. No network at runtime.
- **Tracking:** none. No ATT prompt (`NSPrivacyTracking = false`).

---

## 5. Build the archive

Java 21 required; export `JAVA_HOME` first.

**From Xcode (simplest):** select **Any iOS Device (arm64)** as the destination →
**Product → Archive** → the Organizer opens with the archive.

**From CLI:**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath build/Lancar.xcarchive archive
```

(Requires §2 signing configured — a device archive can't be code-signed without a
team + distribution certificate.)

---

## 6. Upload to App Store Connect / TestFlight

**From the Xcode Organizer** (after §5 archive): **Distribute App → App Store
Connect → Upload**. Xcode validates, signs, and uploads; the build appears under
TestFlight in a few minutes (processing can take longer).

**From CLI** (needs an `ExportOptions.plist` with `method = app-store-connect` and
an App Store Connect API key or app-specific password):

```bash
xcodebuild -exportArchive -archivePath build/Lancar.xcarchive \
  -exportOptionsPlist iosApp/ExportOptions.plist \
  -exportPath build/export
xcrun altool --upload-app -f build/export/Lancar.ipa \
  -t ios --apiKey <KEY_ID> --apiIssuer <ISSUER_ID>
```

(`ExportOptions.plist` is not in the repo yet — create it when the account exists;
it references the Team ID from §2.)

### First release path — TestFlight internal testing (recommended)

1. App Store Connect → **TestFlight** → the uploaded build.
2. Complete the **Export Compliance** question: Lancar uses no encryption beyond
   Apple's standard OS crypto → answer accordingly (typically "No" to custom
   encryption; may still need the standard-exemption declaration). To skip the
   per-build prompt, add `ITSAppUsesNonExemptEncryption = false` to `Info.plist`.
3. Add **Internal Testers** (up to 100, must be App Store Connect users). They
   install via the TestFlight app — no UDID registration needed for internal.
4. Promote to **External** (up to 10,000, needs a light Beta App Review) or submit
   for **App Store** review when ready.

---

## 7. Store listing assets

Text fields (name, subtitle, description, keywords, category) — copy from
[`store-listing.md`](store-listing.md).

### App icon
The App Store pulls the marketing icon from the app's asset catalog
(`iosApp/iosApp/Assets.xcassets/AppIcon.appiconset`, already wired). No separate
1024² upload is needed if the 1024² marketing slot is filled in the catalog —
confirm it is before archiving.

### Screenshots (⚠️ still to capture)

App Store Connect currently requires **6.9-inch iPhone** screenshots
(**1320 × 2868**, portrait) — the iPhone 16 Pro Max class. A 6.5-inch set
(1284 × 2778) is also accepted for older sizing. **Our usual smoke-test sim is
iPhone 16 Pro (6.3", 1206 × 2622), which is *not* an accepted upload size** — use an
**iPhone 16 Pro Max** simulator to capture at the native 6.9" resolution (no padding
needed, unlike the Android 2:1 fix).

Capture the same six screens as Android (see `release-android.md` §4) so the two
stores match:

1. Beranda — review banner + module list
2. Kartu — module picker
3. Kartu — flashcard front
4. Kartu — flashcard back
5. Progres — stats + Leitner boxes
6. Drill — LISTEN-mode quiz

Procedure (mirrors Android, using `simctl`):
```bash
open -a Simulator                                   # foreground first (avoids surface timeout)
xcrun simctl boot "<iPhone 16 Pro Max sim id>"
xcrun simctl install <sim id> <app .app path>
xcrun simctl launch <sim id> cx.viz.lancar
# drive the UI, then:
xcrun simctl io <sim id> screenshot docs/store-assets/ios/01-beranda.png
```
Store the results under `docs/store-assets/ios/` (create it) to match
`docs/store-assets/android/`.

---

## 8. App Store review content forms (answers for Lancar)

Same posture as Android — fully offline, collects nothing:

- **App Privacy (Data collection):** No data collected. (Matches
  `PrivacyInfo.xcprivacy`.)
- **Privacy policy URL:** live at
  **https://chiliec.github.io/indonesian-app/privacy.html** (GitHub Pages, source
  `docs/privacy.html`; shared with the Android listing).
- **Age rating:** educational vocabulary, no objectionable content → 4+.
- **Export compliance:** no non-exempt encryption (see §6).
- **Content rights:** original content + bundled audio owned/licensed.

---

## 9. Pre-upload checklist

- [x] Apple Developer membership active; Team `7JF6XQC536` set in `project.pbxproj` (§2)
- [x] `MARKETING_VERSION 1.0.2` / `CURRENT_PROJECT_VERSION 1` set (bump build for re-uploads)
- [x] `PrivacyInfo.xcprivacy` present and in Copy Bundle Resources (§4)
- [x] 1024² marketing icon present in the asset catalog
- [x] 6.9" screenshots captured under `docs/store-assets/ios/` (§7) — 6 screens @ 1320×2868
- [ ] Store text copied from `store-listing.md` (into App Store Connect)
- [x] Privacy-policy URL live (§8)
- [x] Export-compliance answer set — `ITSAppUsesNonExemptEncryption=false` in `Info.plist`
- [x] `ExportOptions.plist` present for CLI upload (§6): `iosApp/ExportOptions.plist`
- [x] Archive + upload succeeds — done via CI (§10); build 1 is **VALID** in TestFlight

---

## 10. Automated releases (CI)

A GitHub Actions workflow (`.github/workflows/ios-testflight.yml`) builds and
uploads to TestFlight automatically. Fastlane config lives in `fastlane/`.

### Trigger

- **Push a version tag:** `git tag v1.0.3 && git push origin v1.0.3`
- **Manual:** Actions tab → "iOS TestFlight" → Run workflow.

Runs on a `macos-15` runner: sets up Java 21 + Ruby, imports the signing cert
into a temp keychain, then runs `bundle exec fastlane ios beta` (fetches the App
Store profile via the API key, auto-increments the build number from the latest
TestFlight build, archives with `gym`, uploads with `pilot`). The build number
auto-increments; bump `MARKETING_VERSION` in Xcode manually for a new
user-facing version.

### One-time setup: GitHub Actions secrets

Settings → Secrets and variables → Actions → New repository secret:

| Secret | Value |
|---|---|
| `ASC_KEY_ID` | App Store Connect API key ID |
| `ASC_ISSUER_ID` | ASC API issuer ID (UUID) |
| `ASC_KEY_P8` | base64 of the `.p8` key contents (`base64 -i AuthKey_XXXX.p8 \| pbcopy`) |
| `DIST_CERT_P12` | base64 of the Apple Distribution cert+key `.p12` |
| `DIST_CERT_PASSWORD` | the `.p12` export password (may be empty) |

Procedure:
1. App Store Connect → Users and Access → Integrations → App Store Connect API →
   create a key (App Manager role) → download the `.p8` (one-time). Note the Key
   ID and Issuer ID.
2. Locally: `bundle install`, then `bundle exec fastlane ios signing_assets` to
   create/download the Apple Distribution certificate + App Store profile. Export
   the cert+key from Keychain Access as a `.p12`, then `base64 -i dist.p12 |
   pbcopy`.
3. Add the five secrets above.

### Local dry run

`bundle exec fastlane ios beta` runs on your Mac (uses the login-keychain cert
and a git-ignored `AuthKey_*.p8` in the repo root) without pushing a tag.

### Gotcha: `Gemfile.lock` is intentionally **not** committed

`Gemfile.lock` is git-ignored. It is resolved fresh by CI under the runner's
Ruby 3.3 (`ruby/setup-ruby`). A lock committed from a machine with an older Ruby
pins gem versions (and a Bundler version) incompatible with Ruby 3.3 — e.g.
Bundler `1.17.2` calls the removed `String#untaint`, and `CFPropertyList 3.0.9`
caps at Ruby `< 3.2` — which then fails CI's deployment-mode install. Leaving the
lock out lets CI resolve a compatible set each run; `fastlane` stays bounded by
`~> 2.227` in the `Gemfile`.

### First upload — DONE

CI did the first upload: **build 1 of `1.0.2` is VALID** in TestFlight. The
privacy-manifest validator passed (no `PrivacyInfo.xcprivacy` changes needed) and
export compliance is handled by `ITSAppUsesNonExemptEncryption=false`. The one
remaining manual step: **assign internal testers** in App Store Connect →
TestFlight so the build becomes installable (one-time; new builds then flow to
them automatically).

---

## Known follow-ups (2026-07-28)

**Done (this prep pass):** `PrivacyInfo.xcprivacy` authored + bundled (§4),
privacy-policy URL live (§8), `ExportOptions.plist` created (§6),
`DEVELOPMENT_TEAM 7JF6XQC536` set (§2), version `1.0.2`, export-compliance flag.

**Done (screenshots, 2026-07-28):** captured 6 screens @ 1320×2868 on an iPhone 16
Pro Max sim under `docs/store-assets/ios/` (Beranda w/ 🔥 banner, Kartu picker, card
front, card back, Progres w/ Leitner boxes, LISTEN drill). Driven via `idb`
(`~/Library/Python/3.9/bin/idb`) + a seeded realistic review state in the sim DB.

**Done (CI + first upload, 2026-07-28):** automated TestFlight CI added (§10) —
GitHub Actions `macos-15` + fastlane, tag-triggered. First upload succeeded:
build 1 of `1.0.2` is **VALID** in TestFlight. Validator passed, so
`PrivacyInfo.xcprivacy` needed no changes, and the archive/bundling was confirmed
end-to-end by the successful `gym` build + upload.

**Still open:**
- **Assign internal testers** in the App Store Connect TestFlight UI (one-time).
- **External testing / App Store submission** — when ready, promote past internal.
