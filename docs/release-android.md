# Lancar — Android / Google Play Release Runbook

How to cut a Google Play release of Lancar. This is the **Android** runbook;
store copy (shared with iOS) lives in [`store-listing.md`](store-listing.md).

> Status: release **infra is wired and dry-run-validated** (signed AAB builds and
> verifies locally; store assets generated). Not yet published — the remaining
> steps need a Google Play Console account, which does not exist yet.

---

## 0. One-time prerequisites (need a human + money)

1. **Google Play Console account** — one-time US$25. https://play.google.com/console
   Sign up, accept the Developer Distribution Agreement, complete identity/D-U-N-S
   verification (can take a couple of days).
2. **Create the app** in the console: name `Lancar`, default language English (US),
   type App, Free. This reserves the package name **`cx.viz.lancar`** on first upload.

Nothing below can reach the store until step 0 is done. Everything else is already
prepared in-repo.

---

## 1. Signing model (already wired)

We use **Play App Signing**: Google holds the *app signing key*; we sign uploads
with an *upload key*. The build reads the upload key from a **gitignored**
`keystore.properties` at the repo root (see `composeApp/build.gradle.kts`). When
that file is absent, release builds are produced **unsigned** — so CI and other
machines still build without the secret.

`keystore.properties` format:

```properties
storeFile=lancar-release.jks
storePassword=<store password>
keyAlias=lancar
keyPassword=<key password>
```

Both `keystore.properties` and `*.jks` / `*.keystore` / `*.bak` are in `.gitignore`
— **never commit them**.

### The real upload key is in place

The active upload key is **`lancar-release.jks`** (alias `lancar`, valid to 2053,
SHA-256 `C1:17:F4:9B:…:2F:22`), referenced by `keystore.properties`. It replaced the
original throwaway placeholder (`upload-keystore.jks`, alias `upload`, password
`lancar-placeholder`), which has been removed from the repo.

> Confirm this key's SHA-256 matches **Play Console → App integrity → Upload key
> certificate**. If Play shows a different fingerprint, the wrong key was enrolled —
> resolve before the next upload.

**Back up `lancar-release.jks` and its password** somewhere durable (password
manager + offsite). Losing the upload key means filing a Play upload-key reset.

---

## 2. Versioning

Set in `composeApp/build.gradle.kts` `defaultConfig`:

- `versionCode` — integer, **must strictly increase** with every uploaded build.
  Currently `1`. Bump by 1 each upload (even for re-uploads to the same track).
- `versionName` — human string shown to users. Currently `"1.0"`.

Keep `versionName` in sync with the iOS `MARKETING_VERSION` when releasing both.

---

## 3. Build the release artifact

Java 21 is required; export `JAVA_HOME` first (see repo `CLAUDE.md`).

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew :composeApp:bundleRelease
```

Output (the file you upload to Play):

```
composeApp/build/outputs/bundle/release/composeApp-release.aab
```

### Verify it is signed with the upload key

```bash
jarsigner -verify -verbose:summary -certs \
  composeApp/build/outputs/bundle/release/composeApp-release.aab
# expect: "jar verified." and cert CN=Lancar
```

(Dry run on 2026-07-12 produced a ~26 MB AAB, `jar verified`, cert `CN=Lancar`.)

> **Local install / test of a release build:** Play delivers per-device APKs from the
> AAB, so an `.aab` can't be `adb install`ed directly. Use
> [`bundletool`](https://github.com/google/bundletool) (`build-apks --mode=universal`)
> to produce a universal APK, or just smoke-test the debug APK — the UI is identical
> (release has `isMinifyEnabled = false`, so no R8 divergence).

---

## 4. Store listing assets (already generated)

| Asset | Requirement | File |
|---|---|---|
| App icon | 512×512, 32-bit PNG | `design/icon/play-store-512.png` |
| Feature graphic | 1024×500, PNG/JPG | `docs/store-assets/android/feature-graphic.png` |
| Phone screenshots | 2–8, 1200×2400 (2:1) | `docs/store-assets/android/0*.png` |

Text fields (name, short/full description, category) — copy verbatim from
[`store-listing.md`](store-listing.md).

### Screenshots

Six phone screenshots in `docs/store-assets/android/`, all **1200×2400** (padded to
exactly 2:1 with the app's cream `#FAF4E8` — raw device frames are 1080×2400 ≈ 2.22:1,
which **exceeds Play's max 2:1 side ratio and would be rejected**):

1. `01-beranda.png` — Home: review banner + module list with mastery
2. `02-kartu-modules.png` — Kartu: browse module picker
3. `03-kartu-card-front.png` — flashcard front (word + audio)
4. `04-kartu-card-back.png` — flashcard back (translation, note, example)
5. `05-progres.png` — stats: mastery, accuracy, Leitner box distribution
6. `06-drill.png` — LISTEN-mode quiz

To regenerate: boot an emulator, install the debug APK, drive the UI with
`adb shell input tap`, capture with `adb exec-out screencap -p > out.png`, then pad:
`sips -p 2400 1200 --padColor FAF4E8 out.png --out out.png`.

### Feature graphic

Source `design/icon/feature-graphic.svg` (square canvas, banner in the centered band).
Regenerate:
```bash
TMP=$(mktemp -d)
qlmanage -t -s 1024 -o "$TMP" design/icon/feature-graphic.svg
sips -c 500 1024 "$TMP/feature-graphic.svg.png" --out docs/store-assets/android/feature-graphic.png
```

---

## 5. Play Console content forms (answers for Lancar)

Lancar is fully offline and collects nothing — these forms are quick:

- **Data safety:** No data collected, no data shared. (No network at runtime; progress
  and settings stay on-device in SQLDelight.)
- **App content / privacy policy:** Play requires a privacy-policy URL. Draft one
  stating "no data collected, no accounts, fully offline" and host it (e.g. a GitHub
  Pages page). **← the one artifact still to create.**
- **Content rating (IARC questionnaire):** educational vocabulary app, no objectionable
  content → expect "Everyone / PEGI 3".
- **Target audience:** not directed at children (choose 13+ to avoid Families policy
  overhead), unless you intend otherwise.
- **Ads:** contains no ads.
- **Government app / financial / health:** no to all.

---

## 6. First release — Internal testing track

Recommended path for the first upload (fastest review, up to 100 testers):

1. Play Console → **Testing → Internal testing → Create new release**.
2. **Play App Signing:** accept "Use Google-generated key" (Google generates and holds
   the app signing key; your `lancar-release.jks` becomes the upload key). This is the
   default and recommended.
3. Upload `composeApp-release.aab`.
4. Fill release name (e.g. `1.0 (1)`) and release notes (reuse "What's new — v1.0").
5. Add testers (email list), save, review, **roll out to Internal testing**.
6. Share the opt-in URL with testers; they install via Play.

Promote Internal → Closed → Open/Production from the console when ready. Production
requires the full store listing + all content forms complete.

---

## 7. Pre-upload checklist

- [ ] `versionCode` bumped (strictly greater than last uploaded)
- [x] Real upload keystore in place (`lancar-release.jks`, alias `lancar`), password set — **back up offsite**
- [ ] `./gradlew :composeApp:bundleRelease` succeeds
- [ ] `jarsigner -verify` reports "jar verified" with `CN=Lancar`
- [ ] Screenshots + feature graphic + 512 icon on hand
- [ ] Store text copied from `store-listing.md`
- [ ] Privacy-policy URL live
- [ ] Data-safety / content-rating / target-audience forms answered

---

## Known follow-ups (not blockers for internal testing)

- **Privacy-policy URL** — must be authored + hosted before Production (and for the
  App content form).
- **R8/minify** — release currently ships un-minified (`isMinifyEnabled = false`).
  Enabling R8 shrinks the APK but needs keep-rules verified against the KMP/SQLDelight/
  serialization stack; defer until there's a reason.
- **Feature graphic** — functional and on-brand, but authored programmatically; refine
  in a real design tool if desired.
