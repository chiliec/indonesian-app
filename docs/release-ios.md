# Lancar — iOS / App Store Release Runbook

How to cut an App Store (and TestFlight) release of Lancar. This is the **iOS**
runbook; store copy (shared with Android) lives in
[`store-listing.md`](store-listing.md). The Android counterpart is
[`release-android.md`](release-android.md).

> Status (2026-07-28): **LIVE on TestFlight (internal).** The internal group was
> first established with build 1 of `1.0.2`, archived and uploaded by CI (§10), and
> is **VALID** in App Store Connect (App ID `6795209576`, Team `7JF6XQC536`) — the
> privacy-manifest validator passed. New builds flow to that group automatically;
> the current user-facing version is **`1.0.5`** (auto-play audio — CI build in
> flight; see release history below). All prep (usage strings, UIKit scene lifecycle,
> `ITSAppUsesNonExemptEncryption`, bundled `PrivacyInfo.xcprivacy` §4,
> `ExportOptions.plist` §6, live privacy-policy URL §8, 6.9" screenshots §7) is in
> place. **Releases are now automated** — push a `v*` tag to ship a build (§10) and
> it flows to the internal group automatically. Next optional steps: external testing
> or App Store submission (§11 "Going wider").

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

- `MARKETING_VERSION` — user-facing version. Currently **`1.0.4`**. This maps to
  `CFBundleShortVersionString`. Keep in sync with Android `versionName`. Bump it in
  Xcode for each new user-facing version before tagging.
- `CURRENT_PROJECT_VERSION` — build number. Currently **`1`** in the repo, but CI
  **auto-increments** it from the latest TestFlight build at archive time (§10), so
  the committed value is just a floor. Maps to `CFBundleVersion`. **Must strictly
  increase** for every TestFlight/App Store upload.

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
- [x] `MARKETING_VERSION 1.0.4` set; `CURRENT_PROJECT_VERSION` auto-incremented by CI (§10)
- [x] `PrivacyInfo.xcprivacy` present and in Copy Bundle Resources (§4)
- [x] 1024² marketing icon present in the asset catalog
- [x] 6.9" screenshots captured under `docs/store-assets/ios/` (§7) — 6 screens @ 1320×2868
- [~] Store text + screenshots automated via `fastlane ios release` (§12) — staged in
  `fastlane/metadata/`; run the lane to push them to App Store Connect
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

## 11. Assign internal testers — App Store Connect (one-time)

> **DONE (2026-07-28):** build 1 of `1.0.2` is assigned to the internal testing
> group and installable via TestFlight. The steps below are retained as the
> reference procedure (and for adding more testers later).

Build 1 of `1.0.2` is already **VALID** in TestFlight (App ID `6795209576`, Team
`7JF6XQC536`). Export compliance is pre-answered via
`ITSAppUsesNonExemptEncryption=false`, so the build has no "Missing Compliance"
gate. The only remaining step is putting testers on it.

### Step 1 — Testers must be App Store Connect users

Internal testers (up to 100) must be users on the account. The Account Holder
(you) is already covered. To add another:

1. https://appstoreconnect.apple.com → **Users and Access**.
2. **+** → name + Apple ID email → role **Developer** (or **App Manager**) →
   optionally scope to Lancar → **Invite**.
3. They accept the email invite before they can be added as a tester.

Skip this entirely to test only on your own device.

### Step 2 — Create an internal group and attach the build

1. **My Apps → Lancar → TestFlight** tab.
2. Under **Internal Testing**, click **+** (or use the default **App Store
   Connect Users** group).
3. Name it e.g. `Internal` → **Create**.
4. Open the group → **Testers** → **+** → tick yourself (and anyone from Step 1)
   → **Add**.
5. Group's **Builds** → **+** → select **1.0.2 (1)** → **Add**.

Because export compliance is already declared, the build shows **Ready to Test**
immediately (no "Manage Compliance" prompt). Each tester gets an email + a
TestFlight push.

### Step 3 — Install on device

1. Install **TestFlight** from the App Store on the iPhone.
2. Open the invite → **Start Testing**, or open TestFlight and find Lancar under
   available apps.
3. **Install** → launch. Internal builds need **no** UDID registration and **no**
   Beta App Review.

### After this

Future releases flow to these testers automatically: bump `MARKETING_VERSION` in
Xcode for a new user-facing version, then `git tag v1.0.3 && git push origin
v1.0.3` — CI (§10) builds, auto-increments the build number, uploads, and
TestFlight pushes it to the internal group.

### Going wider (later, optional)

- **External testing** — up to 10,000 testers via a public link, but needs a
  one-time light **Beta App Review** per version plus "Test Information" (what to
  test, contact email). TestFlight tab → **External Testing**.
- **App Store submission** — metadata + screenshots are now automated by the
  `release` lane (§12). Run it to populate the 1.0.5 App Store version, finish the
  App Privacy + age-rating forms and pick the build in App Store Connect, then
  **Submit for Review**.

---

## 12. App Store metadata upload — `fastlane ios release` (prepare only)

> **DONE (2026-07-28):** ran successfully — the `1.0.5` App Store listing (text +
> review contact + 6 screenshots) is populated in App Store Connect. No binary
> uploaded, not submitted. Remaining steps are the manual ASC forms below.

The `release` lane (`fastlane/Fastfile`) pushes the App Store **listing** — text
and screenshots — to the `1.0.5` version via `deliver` (`upload_to_app_store`). It
**does not submit for review** and **does not upload a binary** (the build is
already on TestFlight from the `beta` lane / tag-triggered CI, §10).

Sources of truth:
- **Text:** `fastlane/metadata/` — mirrors [`store-listing.md`](store-listing.md)
  (name, subtitle, description, keywords, promotional text, categories
  EDUCATION/REFERENCE, support/marketing URL → GitHub Pages `docs/index.html`,
  privacy URL → `docs/privacy.html`, release notes, copyright).
- **Review contact:** `fastlane/metadata/review_information/` — name/email/notes
  are tracked; `phone_number.txt` is **git-ignored** (personal number, kept local).
- **Screenshots:** `docs/store-assets/ios/` (the six 1320×2868 6.9" PNGs). The lane
  stages copies into a git-ignored `fastlane/screenshots/en-US/` each run, so
  `docs/store-assets/ios/` stays the single source.

Credentials: the ASC API `.p8` is `AuthKey_*.p8` in the repo root (auto-discovered);
`ASC_KEY_ID` / `ASC_ISSUER_ID` come from the git-ignored `fastlane/.env` (dotenv
auto-load; template in `fastlane/.env.example`). Then:

```bash
bundle exec fastlane ios release
```

> **Gotcha — review contact is required.** `deliver` unconditionally fetches the
> app-store *review detail*; on a first-ever App Store version that record doesn't
> exist, so it logs `Error fetching app store review detail - No data` and then
> *creates* it — which the ASC API rejects unless `contactPhone` is present and in
> `+<country> <number>` form. Hence the `review_information/` files (incl. the phone)
> are mandatory for this lane, not optional. The "No data" line itself is benign.

### Submission gates beyond the listing

Apple surfaces several gates only at submit time (`fastlane ios submit` returns them
as "appStoreVersions ... is not in valid state"). Findings + automation from the
2026-07-28 pass:

- **Age rating → 4+ — AUTOMATED.** `scripts/asc_age_rating.rb` sets the age-rating
  declaration (all content `NONE`, all booleans `false`) via the public Connect API
  (`AppInfo → AgeRatingDeclaration`, ASC API 1.3+ — it moved off `AppStoreVersion`).
  Idempotent; confirmed `FOUR_PLUS`.
- **Content Rights → no third-party content — AUTOMATED.** One-off PATCH
  `v1/apps/<id>` with `contentRightsDeclaration = DOES_NOT_USE_THIRD_PARTY_CONTENT`.
- **iPad Pro 12.9" screenshots — CAPTURED.** The app is universal
  (`TARGETED_DEVICE_FAMILY = "1,2"`), so Apple requires 2048×2732 iPad shots on top
  of the iPhone set. Captured on an `iPad Pro (12.9-inch) (6th gen)` sim
  (iOS 26.4 runtime) driven by `.claude/skills/run-lancar/driver.py` + a seeded DB
  (`scripts/gen_seed_sql.rb` → 983 mastered, 32 due today; sqlite3 into the sim
  container's `lancar.db`, then relaunch). Stored in `docs/store-assets/ios-ipad/`;
  the `release` lane stages them (prefixed `ipad-`) and deliver classifies by pixel
  size. Gotcha: the app pre-requests **Speech Recognition** at launch — a system
  alert simctl can't pre-grant (no such privacy service); tap **Allow** via idb once
  and it stops prompting.
- **Pricing → Free — WEB UI.** Set in App Store Connect → Pricing and Availability.
  No safe API (appPriceSchedule needs a territory + free price-point graph; not worth
  mis-pricing a live app). Confirmed set (manualPrices: 1, base USA).
- **App Privacy → No data collected — WEB UI ONLY.** No reachable API for the privacy
  nutrition labels with our key (app resource exposes no data-usage relationship;
  all `appDataUsages*` paths 404 on public + Iris; spaceship targets removed
  endpoints; Ruby 2.6 caps fastlane at 2.231.1). **Browser:** ASC → Lancar →
  **App Privacy** → *Data Collection* → **No, we do not collect data** → **Publish**.
  ⚠️ The **Publish** button is separate from answering — an un-published draft still
  fails submit with "You must have published answers to your app's data usages."

`scripts/asc_state.rb` prints version/build/age-rating state (read-only).

### Final submit — `fastlane ios submit`

Once App Privacy + Pricing are set in the browser (everything else is automated),
run the `submit` lane (§10 creds): it selects the latest build (1.0.5 (4)) and
submits for review. As of end-of-session 2026-07-28 the submit is **blocked only on
App Privacy being published** — build selection succeeds; re-run `fastlane ios
submit` after publishing.

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

**Done (internal testing, 2026-07-28):** build 1 of `1.0.2` assigned to the
internal testing group in App Store Connect (§11) — now installable via TestFlight.

**Release history (tag-triggered CI, §10):**
- `v1.0.2` — first upload; established the internal testing group.
- `v1.0.3` — on-device `TextField`-focus crash fix (CMP 1.8.2 → 1.9.3).
- `v1.0.4` — on-device audio silence fix: activate the shared `AVAudioSession`
  with the `.playback` category so clips are audible regardless of the hardware
  mute switch (the sim has no switch, so it never reproduced there).
- `v1.0.5` — auto-play audio feature: LISTEN quiz questions play automatically on
  entry; Kartu flashcards play on card arrival (keyed on `settledPage`, not flip);
  Profile toggle "🔊 Putar audio otomatis" (default on).

**Done (App Store metadata automation, 2026-07-28):** added the `release` lane (§12)
— `deliver` pushes `fastlane/metadata/` (text + review contact) + `docs/store-assets/ios/`
(screenshots) to the 1.0.5 App Store version, no submit, no binary. Added a
support/marketing landing page `docs/index.html` (GitHub Pages) for the listing's
support + marketing URLs. **Ran the lane** — the 1.0.5 listing is populated in ASC.

**Done (submission automation, 2026-07-28 evening):** added the `submit` lane (§10),
automated age rating (4+) + content rights via ASC API (`scripts/asc_age_rating.rb`),
captured + uploaded iPad Pro 12.9" screenshots (`docs/store-assets/ios-ipad/`,
`scripts/gen_seed_sql.rb` seed), and cleared the metadata/screenshot/pricing gates.
Ran `fastlane ios submit`: build 1.0.5 (4) selects successfully.

**Still open (ONE step to ship):**
- **Publish App Privacy in the browser**, then run `bundle exec fastlane ios submit`.
  This is the *only* remaining blocker — submit currently fails with "You must have
  published answers to your app's data usages" because the App Privacy questionnaire
  isn't Published yet (answering ≠ publishing; there's a separate Publish button).
  Everything else (age rating, content rights, pricing, listing, screenshots incl.
  iPad, review contact, build 4 VALID) is done. See §12 "Submission gates".
- **External testing** — optional wider beta; needs a one-time light Beta App Review
  + Test Information (§11 "Going wider").
