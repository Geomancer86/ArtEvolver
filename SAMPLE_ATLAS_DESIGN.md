# Evolution Clicker — Sample Atlas Design

*Miyamoto: collection and discovery. Wright: emergent goals. Kojima: hidden content. Sakaguchi: meaningful progression.*

## Overview

The Sample Atlas has **50 samples** in two groups:

1. **Default tree (25):** Real-life, open-source, free photos. Load from `samples/{id}.jpg` or `samples/{id}.png`. Curate for **75–80%+ fitness**. See `artevolver-core/src/main/resources/samples/README.md`.
2. **Generated (25):** Programmatic fallbacks (gradients, geometry, fractals). Always available for testing. **Samples are always re-pickable** — even after completion, they never go into `usedImageFingerprints`.

## Default Tree — Real Photos (file-based)

### Starter (5) — always available
- landscape_01, ocean_01, forest_01, sunset_01, flowers_01

### Apprentice (5) — unlock by EP earned (lifetime)
- 500 EP: mountain_01
- 2,000 EP: canyon_01
- 5,000 EP: lake_01
- 10,000 EP: meadow_01
- 20,000 EP: beach_01

### Veteran (5) — unlock by ascensions
- 1: cityscape_01
- 2: portrait_01
- 3: architecture_01
- 5: wildlife_01
- 7: night_01

### Master (5) — unlock by masterpieces completed
- 1: abstract_01
- 2: macro_01
- 3: aerial_01
- 5: street_01
- 7: still_life_01

### Secret (5) — hidden until discovered
- ??? (5,000 clicks): secret_01
- ??? (30,000 EP): secret_02
- ??? (10 ascensions): secret_03
- ??? (10 masterpieces): secret_04
- ??? (100 GF): secret_05

## Generated Tree — Programmatic (always available)

All 25 generated samples (gradient_sunset, circles, checker, stripes, diamond, spiral, rings, grid_grad, waves, maze, starfield, hexagons, voronoi, noise_cloud, mandala, rose, fractal_tree, kaleidoscope, aurora, portal, easter_egg, hidden_gem, legendary, mythic, omega) are in category **Generated** with `unlockType: "always"` — kept for testing and discovery.

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
