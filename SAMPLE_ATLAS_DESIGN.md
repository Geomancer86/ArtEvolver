# Evolution Clicker — Sample Atlas Design

*Miyamoto: collection and discovery. Wright: emergent goals. Kojima: hidden content. Sakaguchi: meaningful progression.*

## Overview

The Sample Atlas is a built-in library of **25 programmatically generated images** that players can unlock and play. No external assets — all images are generated in-code (gradients, geometric patterns, fractals, etc.) for zero dependencies and offline play.

## Categories & Unlock Tree

### Starter (5) — always available
- Sunset Gradient, Concentric Circles, Checkerboard, Rainbow Stripes, Diamond Shape

### Apprentice (5) — unlock by EP earned (lifetime)
- 500 EP: Spiral
- 2,000 EP: Ripple Rings
- 5,000 EP: Grid Gradient
- 10,000 EP: Sine Waves
- 20,000 EP: Mini Maze

### Veteran (5) — unlock by ascensions
- 1: Starfield
- 2: Honeycomb
- 3: Voronoi
- 5: Cloud Noise
- 7: Mandala

### Master (5) — unlock by masterpieces completed
- 1: Rose Curve
- 2: Fractal Tree
- 3: Kaleidoscope
- 5: Aurora
- 7: Portal

### Secret (5) — hidden until discovered
- ??? (5,000 clicks): Easter Egg
- ??? (30,000 EP): Hidden Gem
- ??? (10 ascensions): Legendary
- ??? (10 masterpieces): Mythic
- ??? (100 GF): Omega

## Progress Tracking

- **Best fitness** — highest fitness ever achieved on that sample
- **Best gain** — highest fitness gain from start
- **Play count** — how many times the sample has been played
- **Completed** — whether the sample was finished as a masterpiece (≥85% fitness)

## UI

- **Init overlay**: Sample picker grid below the drop zone. Locked samples show `???`. Unlocked show thumbnail and name. Click to select, then Begin.
- **Sample Atlas** (footer button): Full overlay with all samples. Shows unlock condition for locked, progress stats for played. Click unlocked sample to start a game with it.
- **Gallery**: Continues to show user-completed images (ascensions/masterpieces). Separate from Atlas.

## Implementation

- `SampleImageProvider` — generates images, defines metadata
- `ClickerState` — tracks lifetimeClicks, lifetimeEpEarned, discoveredSamples, sampleProgress
- `GET /api/clicker/samples` — JSON with all samples, unlock status, progress
- `GET /api/clicker/sample/{id}?thumb=1` — image (full or 120x78 thumbnail)
- `POST /api/clicker/init?sample={id}` — start game with sample (bypasses fingerprint duplicate check)
