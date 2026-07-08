# Content pipeline

## Source dataset

```
../indonesian/content/quiz/
  module-{1..8}.yaml    # vocabulary cards in YAML
  audio/*.ogg           # source audio files (Ogg Vorbis)
```

The source repo is a sibling directory of this project. The pipeline reads from it but does not modify it.

## Running the pipeline

```bash
cd content-prep
npm install
npm run prep
```

Requires Node 20+ and ffmpeg on PATH. Re-run only when the upstream YAML or audio changes. The output is committed to the repo; there is no build-time generation step.

## What the pipeline produces

Output lands in `composeApp/src/commonMain/composeResources/files/`:

```
files/
  content/
    manifest.json         # module list with card counts
    module-1.json
    ...
    module-8.json
  audio/
    *.m4a                 # transcoded audio (1,769 files)
```

Totals: 1,796 cards, 1,769 m4a files, 8 modules.

## JSON contract

### manifest.json

```json
{
  "modules": [
    { "id": "module-1", "title": "Module 1", "cardCount": 256 },
    ...
  ]
}
```

### module-N.json (array of cards)

Each card object:

```json
{
  "id": "string",
  "indonesian": "string",
  "english": "string",
  "note": "string | null",
  "audio": "string | null",
  "sentences": [
    { "text": "string", "blank": "string", "en": "string" }
  ]
}
```

Field notes:
- `id` — stable identifier; used as the primary key in the progress database
- `audio` — filename (without path) of the corresponding m4a in `files/audio/`; `null` when no audio exists for the card
- `note` — optional usage note shown in the UI
- `sentences` — fill-in-the-blank examples; may be empty

## Audio transcode

Source: `.ogg` (Ogg Vorbis)
Output: `.m4a` (AAC, 64 kbps)
Tool: ffmpeg

The transcode is performed by `content-prep/src/prep.ts`. Each source file `audio/foo.ogg` produces `files/audio/foo.m4a`. Cards whose `audio` field is non-null reference these filenames.

## When to re-run

Re-run the pipeline only when:
- New cards are added to the upstream YAML
- Existing card fields change (id, indonesian, english, note, audio)
- Audio files are added or replaced

Do not re-run as part of the normal app build. The generated files are stable and committed.
