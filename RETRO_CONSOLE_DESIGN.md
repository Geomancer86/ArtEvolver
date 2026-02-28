# Retro Console Mode — Design Document

*Gunpei Yokoi: lateral thinking with withered technology. Old constraints, new expression.*

## Overview

Add support for **retro game console palettes and resolutions** to ArtEvolver. Output will resemble Game Boy, Game Boy Color, GBA, Sega Genesis, and SNES graphics — square pixels, limited palettes, authentic resolutions. The evolution algorithm (swap colors, evaluate fitness, accept if better) stays the same; the **data structure** (pixels vs triangles) and **palette** change.

**Primary target**: Evolution Clicker mode for rapid iteration. Full ArtEvolver and tournament support follow.

---

## Retro Console Specs (Reference)

| Console | Resolution | Colors | Palette Format | Notes |
|---------|------------|--------|----------------|-------|
| **Game Boy (GB)** | 160×144 | 4 | 4 shades (monochrome) | DMG green or grayscale |
| **Game Boy Color (GBC)** | 160×144 | 32 | 8 palettes × 4 colors | Or 10 palettes × 4 |
| **Game Boy Advance (GBA)** | 240×160 | 32,768 | 15-bit RGB (5-5-5) | Full color |
| **Sega Genesis** | 320×224, 256×224 | 512 | 9-bit RGB (3-3-3) | 64 on-screen typical |
| **SNES** | 256×224 | 32,768 | 15-bit BGR555 | 256 palette entries |

**Square pixels**: All use 1:1 pixel aspect ratio (no non-square pixels in these modes).

---

## Architecture Options

### Option A: Triangle Mode + Retro Palettes (Lower Effort)

- Keep current triangle grid and `DeltaFitnessEngine`.
- Add retro palettes (GB 4-color, GBC 32, Genesis 512, etc.).
- Resize source to retro resolution; grid dimensions derived from triangle count.
- **Output**: Triangle mosaic at retro resolution — not true pixel art, but retro colors.

**Pros**: Minimal code change. **Cons**: Still triangle-based; not authentic pixel look.

### Option B: Pixel Mode (True Retro)

- New data structure: **pixel grid** — `width × height` cells, each = one color index.
- Each cell = one pixel. Swap = swap two pixels' color indices.
- **Delta fitness**: Trivial — each pixel contributes independently. `delta = |ref[px]-newA| - |ref[px]-oldA| + |ref[px]-newB| - |ref[px]-oldB|` for the two swapped pixels.
- **Render**: Direct `BufferedImage` — `img.setRGB(x, y, palette[colorIndex].getRGB())`.

**Pros**: True pixel art, square pixels, authentic look. **Cons**: New engine path.

### Option C: Hybrid (Recommended)

- **Pixel mode** for low-color systems (GB, GBC, Genesis) — true pixel art.
- **Triangle mode** for high-color (GBA, SNES) — or pixel mode throughout for consistency.
- Shared: `RetroPreset` enum, palette loading, resolution config.

**Recommendation**: Implement **pixel mode** for all retro presets. Simpler and gives the desired look. Triangle mode remains for "quality" / Sherwin-Williams presets.

---

## Data Structures

### RetroPreset (new)

```java
public enum RetroPreset {
    GB(160, 144, "gb", 4, "Game Boy"),
    GBC(160, 144, "gbc", 32, "Game Boy Color"),
    GBA(240, 160, "gba", 32768, "Game Boy Advance"),
    GENESIS(320, 224, "genesis", 512, "Sega Genesis"),
    GENESIS_256(256, 224, "genesis_256", 512, "Sega Genesis (256)"),
    SNES(256, 224, "snes", 32768, "Super Nintendo");
    // ...
}
```

### PixelGrid (new)

```java
/** 2D grid of color indices. Each cell = one pixel. */
public class PixelGrid {
    private final int width, height;
    private final int[] colorIndices;  // length = width * height
    private final Palette palette;
    // swap(i, j), getColorIndex, setColorIndex, render(BufferedImage)
}
```

### PixelFitnessEngine (new)

- Tracks `refR[], refG[], refB[]` per pixel.
- `computeSwapDelta(pxA, pxB)`: only 2 pixels change — O(1) per swap.
- `applySwap(pxA, pxB)`: swap color indices, update totalDiff.

---

## Palette Files

Create new palette resources:

| File | Colors | Format | Source |
|------|--------|--------|--------|
| `gb_dmg.txt` | 4 | ID Name R G B | DMG green shades |
| `gb_gray.txt` | 4 | Same | Grayscale |
| `gbc.txt` | 32 | Same | GBC default palettes |
| `genesis.txt` | 512 | Same | Genesis 9-bit (R,G,B ∈ {0,32,64,96,128,160,192,224,255}) |
| `gba.txt` | (optional) | 15-bit sample | Subset for evolution |
| `snes.txt` | (optional) | 15-bit sample | Subset |

**Palette loader**: Extend `Palette` or add `PaletteLoader.load(name)` to load by resource name. Current `Palette` is hardcoded to `sherwin.txt`; we need parameterized loading.

---

## Resolution × Palette Matrix (Test Combinations)

| Preset | Resolution | Colors | Use Case |
|--------|------------|--------|----------|
| GB DMG | 160×144 | 4 | Monochrome, Link's Awakening style |
| GB Grayscale | 160×144 | 4 | Classic GB look |
| GBC | 160×144 | 32 | Pokémon Gold/Silver, Zelda Oracles |
| GBA | 240×160 | 32,768 | Full color handheld |
| Genesis | 320×224 | 512 | Sonic, Streets of Rage |
| Genesis 256 | 256×224 | 512 | Some games |
| SNES | 256×224 | 32,768 | Donkey Kong Country, Chrono Trigger |

**High-end references** (user mentioned):
- Alone in the Dark (256 colors, 320×200 PC) — closest: Genesis 512
- Donkey Kong Country GBC — GBC 32
- Cannon Fodder — Genesis/Amiga palette
- SNES — 256×224, 32k colors

---

## Implementation Plan

### Phase 1: Foundation (Clicker-only)

1. **PaletteLoader** — Load palette by name (`gb_dmg`, `gbc`, `genesis`, etc.).
2. **RetroPreset** — Enum with resolution, palette name, color count.
3. **PixelGrid** — 2D color index array, swap, render.
4. **PixelFitnessEngine** — Delta fitness for pixel swaps (O(1) per swap).
5. **ClickerEngine** — Add `initPixelMode(RetroPreset)` path. When preset != null, use PixelGrid + PixelFitnessEngine instead of TriangleList + DeltaFitnessEngine.
6. **DashboardServer** — `handleClickerInit` accepts `?preset=gb` (or similar). Resize source to preset resolution, init pixel mode.
7. **clicker.html** — Preset selector (GB, GBC, GBA, Genesis, SNES) in init overlay.

### Phase 2: Integration

8. **ArtEvolver** — Add retro preset to sidebar (optional).
9. **Tournament** — Support retro presets in EvolutionConfig (optional).

### Phase 3: Polish

10. **Palette files** — Create gb_dmg.txt, gbc.txt, genesis.txt.
11. **Sample images** — Add retro-style samples to Sample Atlas.
12. **Export** — Optional 1:1 pixel export (no scaling) for retro presets.

---

## File Impact

| File | Change |
|-----|--------|
| `Palette.java` | Add `loadFromResource(String name)` or constructor(name) |
| `DashboardServer.java` | Handle `preset` param, pass to init |
| `ClickerState.java` | `initEngine(..., String preset)` |
| `ClickerEngine.java` | Branch: pixel mode vs triangle mode |
| **New** `PixelGrid.java` | Pixel grid data structure |
| **New** `PixelFitnessEngine.java` | O(1) swap delta for pixels |
| **New** `RetroPreset.java` | Enum of presets |
| **New** `gb_dmg.txt`, `gbc.txt`, `genesis.txt` | Palette files |
| `clicker.html` | Preset selector UI |

---

## Effort Estimate

| Phase | Effort | Risk |
|-------|--------|------|
| Phase 1 | Medium (3–5 days) | Low — isolated to clicker |
| Phase 2 | Low (1–2 days) | Low |
| Phase 3 | Low (1–2 days) | Low |

**Total**: ~1–2 weeks for full implementation. Phase 1 alone delivers clicker retro mode.

---

## Algorithm Equivalence

The core evolution loop is unchanged:

1. **Select** two cells (triangles or pixels) at random (within distance limit).
2. **Evaluate** delta fitness if their colors were swapped.
3. **Apply** swap only if delta < 0 (improves fitness).
4. **Repeat** for retry cycles, pick best.

For pixels, `computeSwapDelta(pxA, pxB)` is simpler than triangles (2 pixels vs hundreds per triangle). Fitness formula: `1 - totalDiff / (width*height * 3 * 255)` — same as current.

---

## References

- [Sega Genesis VDP Guide](https://megacatstudios.com/blogs/press/sega-genesis-mega-drive-vdp-graphics-guide-v1-2a-03-14-17)
- [SNES Specs](https://en.wikibooks.org/wiki/Super_NES_Programming/SNES_Specs)
- [GBA Graphics](https://www.coranac.com/tonc/text/video.htm)
- [GB/GBC Pandocs](https://gbdev.io/pandocs/)
