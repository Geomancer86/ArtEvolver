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

## Progress Tracking

- **Best fitness** — highest fitness ever achieved on that sample
- **Best gain** — highest fitness gain from start
- **Play count** — how many times the sample has been played
- **Completed** — whether the sample was finished as a masterpiece (≥85% fitness)

## UI

- **Init overlay**: Sample picker grid below the drop zone. Locked samples show `???`. Unlocked show thumbnail and name. Click to select, then Begin.
- **Sample Atlas** (footer button): Full overlay with all samples. Shows unlock condition for locked, progress stats for played. Click unlocked sample to start a game with it.
- **Gallery**: Continues to show user-completed images (ascensions/masterpieces). Separate from Atlas.

## Fitness Curation (75–80%+)

Good images reach **75–80%+ fitness** with the default palette (Sherwin-Williams 4×) and grid (80×53). We measure this via `DeltaFitnessEngine` at init. Suggested sources: Unsplash, Pexels, Pixabay (CC0 / free). See `samples/README.md`.

## Implementation

- `SampleImageProvider` — loads from `samples/{id}.jpg|.png` first, falls back to programmatic for Generated ids
- `GenerateSamplePlaceholders` — run once to create placeholder JPGs; replace with curated CC0 images
- `ClickerState` — tracks lifetimeClicks, lifetimeEpEarned, discoveredSamples, sampleProgress; **samples bypass usedImageFingerprints** (always re-pickable)
- `GET /api/clicker/samples` — JSON with all samples, unlock status, progress
- `GET /api/clicker/sample/{id}?thumb=1` — image (full or 120x78 thumbnail)
- `POST /api/clicker/init?sample={id}` — start game with sample (bypasses fingerprint duplicate check)
