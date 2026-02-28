# Evolution Clicker — Sample Atlas Design

*Miyamoto: discovery. Wright: multiple paths. Kojima: secrets. Sid Meier: meaningful gates. Sakaguchi: payoff for investment.*

## Overview

Gamedev-designed sample tree with **iconic art** (Mona Lisa, Starry Night, Great Wave, etc.) and **multiple unlock paths** — different playstyles unlock different samples.

1. **Main tree (32):** File-based real photos / public-domain art. **OR logic:** meet any condition to unlock.
2. **Generated (25):** Programmatic fallbacks. Always available. **Samples always re-pickable** — never in `usedImageFingerprints`.

## Main Tree — Multi-Path Unlocks

### Starter (5) — always available
- landscape, ocean, forest, sunset, flowers

### Apprentice (5) — Path A: EP | Path B: Clicks
- mountain: 500 EP or 1,000 clicks
- canyon: 2,000 EP or 5,000 clicks
- lake: 5,000 EP or 10,000 clicks
- meadow: 10,000 EP or 20,000 clicks
- beach: 20,000 EP or 50,000 clicks

### Veteran (5) — Path A: Ascensions | Path B: Masterpieces
- cityscape: 1 ascension or 1 masterpiece
- architecture: 2 ascensions or 2 masterpieces
- portrait: 3 ascensions or 2 masterpieces
- wildlife: 5 ascensions or 3 masterpieces
- night: 7 ascensions or 5 masterpieces

### Master (5) — Cross-path
- abstract: 5 ascensions or 3 masterpieces
- macro: 7 ascensions or 30k EP
- aerial: 30k clicks or 5 masterpieces
- street: 50k clicks or 5 ascensions
- still_life: 40k EP or 7 masterpieces

### Legendary (7) — Iconic public-domain art
- **Mona Lisa** (Leonardo): 5 masterpieces or 10 ascensions
- **Starry Night** (Van Gogh): 3 masterpieces or 50k EP
- **Great Wave** (Hokusai): 5 ascensions or 75k clicks
- **Girl with a Pearl Earring** (Vermeer): 7 masterpieces or 7 ascensions
- **Birth of Venus** (Botticelli): 50 GF or 10 masterpieces
- **American Gothic** (Grant Wood): 10 ascensions or 75k EP
- **Persistence of Memory** (Dalí): 100 GF or 10 masterpieces

### Secret (5) — Kojima-style
- ???: 20 miss streak or 100k clicks (suffering rewards)
- ???: 100k clicks or 100k EP (grind)
- ???: 15 ascensions or 15 masterpieces
- ???: 200 GF
- ???: 500 GF or 20 masterpieces (omega)

## Generated Tree — Programmatic (always available)

25 samples (gradient_sunset, circles, checker, …) in category **Generated**. Kept for testing.

## Retro Tree — Console-Specific (always available)

10 samples in category **Retro**, each with a `recommendedPreset` linking to its home console:

| ID | Name | Resolution | Home Preset |
|----|------|-----------|-------------|
| retro_gb_castle | GB Castle | 160×144 | gb_dmg |
| retro_gb_hills | GB Hills | 160×144 | gb_gray |
| retro_gbc_town | GBC Town | 160×144 | gbc |
| retro_gbc_underwater | GBC Underwater | 160×144 | gbc |
| retro_gen_skyline | Genesis Skyline | 320×224 | genesis_64 |
| retro_gen_ruins | Genesis Ruins | 320×224 | genesis |
| retro_gba_forest | GBA Forest | 240×160 | gba |
| retro_gba_beach | GBA Beach | 240×160 | gba |
| retro_snes_mountains | SNES Mountains | 256×224 | snes_256 |
| retro_snes_village | SNES Village | 256×224 | snes |

These appear in the "Retro Specials" section when the matching console tab is selected.
Generated at native resolution using `Graphics2D` — no external files needed.

## Progress Tracking

- **Best fitness** — highest fitness ever achieved on that sample
- **Best gain** — highest fitness gain from start
- **Play count** — how many times the sample has been played
- **Completed** — whether the sample was finished as a masterpiece (≥85% fitness)
- **ascensionsCount** — total ascensions on that sample
- **totalPlayTimeMs** — cumulative play time in milliseconds
- **totalClicks** — total clicks across all plays

These stats are shown in the init picker and Sample Atlas (e.g. "5 plays | 2 asc | max 78.3%").

## Real Sample Images

- **DownloadSampleImages** — Downloads real CC0 images from Picsum (landscapes, nature) and optionally Wikimedia Commons (iconic art).
- Starter and Apprentice samples use actual photos (landscape, ocean, forest, sunset, flowers, etc.).
- Legendary art (Mona Lisa, Starry Night, etc.) may use placeholders if Wikimedia rate limits; replace manually or run with delays.
- See `samples/README.md` and `DownloadSampleImages.java`.

## Custom Images (My Images)

- **First-time upload** — Custom images are saved to `~/.artevolver/clicker-uploads/{fingerprint}.png` on first use.
- **My Images section** — Init overlay shows stored custom images (thumbnails) when available. No need to browse folders again.
- **Gallery "Play again"** — Gallery cards for custom images show a "Play again" button (standalone mode). Loads the stored image and opens the init overlay for a fresh run.
- **API**: `GET /api/clicker/my-images`, `GET /api/clicker/my-image/{fingerprint}?thumb=1`

## UI

- **Level select screen** (init overlay): Console tab bar (Classic / Game Boy / Genesis / GBA / SNES) at top. Each tab shows sub-preset buttons (e.g. DMG / Gray / Color / Color High for Game Boy), a "Retro Specials" grid of console-specific samples, and an "All Images" grid of universal samples. Thumbnails load with `?preset=X` to show quantized previews. Preview panel at bottom shows selected image rendered through the active preset. "Begin" button reflects the selected mode.
- **Sample Atlas** (footer button): Full overlay with all samples. Shows unlock condition for locked, progress stats for played. Click unlocked sample to start a game with it.
- **Gallery**: User-completed images (ascensions/masterpieces). Custom images have "Play again" for quick re-use. Separate from Atlas.

## Fitness Curation (75–80%+)

Good images reach **75–80%+ fitness** with the default palette (Sherwin-Williams 4×) and grid (80×53). We measure this via `DeltaFitnessEngine` at init. Suggested sources: Unsplash, Pexels, Pixabay (CC0 / free). See `samples/README.md`.

## Implementation

- `SampleImageProvider` — loads from `samples/{id}.jpg|.png` first, falls back to programmatic for Generated ids
- `DownloadSampleImages` — run to fetch real images from Picsum/Wikimedia; stores in `samples/`
- `GenerateSamplePlaceholders` — fallback for missing files; creates gradient JPGs
- `ClickerState` — tracks lifetimeClicks, lifetimeEpEarned, discoveredSamples, sampleProgress (incl. ascensionsCount, totalPlayTimeMs, totalClicks); **samples bypass usedImageFingerprints** (always re-pickable)
- `saveCustomImageToGallery(BufferedImage)` — saves uploads to `~/.artevolver/clicker-uploads/` on first use
- `GET /api/clicker/samples` — JSON with all samples, unlock status, progress stats
- `GET /api/clicker/sample/{id}?thumb=1` — image (full or 120x78 thumbnail)
- `GET /api/clicker/sample/{id}?preset=X&thumb=1` — quantized preview through a retro preset's palette
- `GET /api/clicker/my-images` — list of stored custom images
- `GET /api/clicker/my-image/{fingerprint}?thumb=1` — serve stored custom image
- `POST /api/clicker/init?sample={id}` — start game with sample (bypasses fingerprint duplicate check); `sampleId=custom:{fingerprint}` for My Images
