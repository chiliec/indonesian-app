# Design import

## Source of truth

The design was delivered as a **Claude Design handoff bundle**:
`Bahasa Indonesia iOS app-handoff.zip` (repo root). Inside:
`bahasa-indonesia-ios-app/project/Lancar iOS App.dc.html` — an HTML/CSS/JS
prototype of the full app (onboarding, home, flashcards, lesson, progress,
profile) with the color and type tokens inline.

## Status: imported (palette + typography + screen restyle)

As of 2026-07-08 the design tokens are applied to the existing screens
(no new features — see scope note). Implementation lives in
`composeApp/src/commonMain/kotlin/com/axveer/lancar/ui/theme/Theme.kt`.

### Color tokens (`Theme.kt`)
| Token | Hex | Role |
|---|---|---|
| Terracotta | `#C8502B` | primary / CTAs / progress fill |
| Green | `#2F6B4F` | secondary / correct-answer border |
| Amber | `#E9A93D` | tertiary / stats |
| Ink | `#1E2B24` | text, dark panels |
| Cream | `#FAF4E8` | app background |
| Surface | `#FFFDF8` | cards |
| Secondary text | `#5A5244` | captions |
| Border | `#E5D9C3` | card outlines |
| Panel | `#F1E7D4` | muted fills / progress track |
| Correct | bg `#E4F0E8` / text `#1E4433` | right answer |
| Wrong | bg `#F9E2DB` / text `#7A2313` / border `#B3341C` | wrong answer |

Alternate accents offered by the design: green `#2F6B4F`, blue `#31547E`.

### Typography (`Theme.kt`)
- **Headings:** Bricolage Grotesque (Bold 700, ExtraBold 800)
- **Body:** Instrument Sans (Regular/Medium/SemiBold/Bold)
- Fonts bundled as **static instances** sliced from the Google Fonts variable
  TTFs (via `fonttools varLib.instancer`) into
  `composeApp/src/commonMain/composeResources/font/` — variable-axis fonts
  were avoided for K/N rendering safety. Regenerate with `fonttools
  varLib.instancer <var>.ttf wght=<w> [opsz=14 wdth=100] -o <out>.ttf`.

## Not yet built (design shows, v1 scope excludes)

The prototype depicts features intentionally **out of v1 scope** in `CLAUDE.md`:
onboarding flow, streak/XP/levels, flip-style flashcards, Profile tab, and the
"Kalimat hari ini" / lesson framing. The current app is module-browse →
4-option drill → results. Revisit scope before building these.
