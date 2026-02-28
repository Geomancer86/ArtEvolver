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
| **Sega Genesis** | 320×224, 256×224 | 512 | 9-bit RGB (3-3-3) | 64 on-screen; 512 in palette RAM |
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

---

## Designer Validation — Would the Greats Concur?

*Gunpei Yokoi, Shigeru Miyamoto, Hideo Kojima, John Carmack, Will Wright, Sid Meier, Sandy Petersen, Tim Cain, Gary Gygax, Gabe Newell — what would they validate?*

### Yokoi: Lateral Thinking with Withered Technology ✓

> "The best way to create something new is to use old technology in new ways."

Retro mode embraces this: fixed resolutions and palettes are **constraints that inspire creativity**, not limitations to overcome. A 4-color GB image forces different artistic choices than 32k SNES — both are valid expressions.

### Miyamoto: Constraints as Playground ✓

> "A delayed game is eventually good, but a rushed game is forever bad."

Pixel mode is **simpler** than triangle mode — fewer moving parts, faster iteration. Clicker-first rollout lets us validate the core loop before expanding. The preset progression (GB → GBC → Genesis → GBA → SNES) mirrors hardware evolution: players can "graduate" from monochrome to full color.

### Kojima: Authenticity Over Approximation ✓

> "I don't want to make something that looks like something — I want to make the thing itself."

**Technical accuracy matters.** Resolutions (160×144, 256×224, 320×224, 240×160) and palette bit depths (4, 32, 512, 32k) match real hardware. DMG green, Genesis 9-bit ramp, SNES BGR555 — these produce **authentic** output, not "retro-style" approximations.

### Carmack: Clean Architecture ✓

> "Focus on making something that works, then make it work better."

**Pixel mode is a parallel path**, not a refactor. `PixelGrid` + `PixelFitnessEngine` mirror `TriangleList` + `DeltaFitnessEngine`. Same swap-and-evaluate algorithm; different data structure. No risk to existing triangle evolution. O(1) per-pixel swap delta is simpler than triangle masks.

### Wright: Emergent Complexity ✓

> "The goal is to create a system where interesting things emerge from simple rules."

Same evolution loop: select two cells, evaluate swap delta, accept if better. The **emergent** result — recognizable images from limited palettes — arises from the same genetic algorithm. Constraint changes the aesthetic; the algorithm stays elegant.

### Meier: Meaningful Progression ✓

> "A game is a series of interesting choices."

Preset selector = **meaningful choice**. GB for stark monochrome, GBC for Pokémon/Zelda feel, Genesis for Sonic/Streets of Rage, SNES for DKC/Chrono Trigger. Each preset is a distinct "game mode" with different goals and aesthetics.

### Petersen: Creative Constraints ✓

> "Limitations force you to be creative."

4 colors (GB) vs 32 (GBC) vs 512 (Genesis) vs 32k (SNES) — each tier demands different techniques. Dithering, color cycling, palette swapping: the **constraint** is the design space.

### Cain: Systems That Respect the Player ✓

> "Give the player tools and let them discover."

Palette files are **extensible**. Users can add custom `gb_*.txt`, `genesis_*.txt` for homebrew palettes. The system doesn't lock them into our defaults.

### Gygax: Rules as Framework ✓

> "The secret we should never let the gamemasters know is that they don't need any rules."

The "rules" (resolution, palette, swap algorithm) are fixed. The **expression** (which image, which preset) is player choice. Clear framework, infinite outcomes.

### Newell: Iterate, Ship, Learn ✓

> "We think of ourselves as a service. We're constantly updating."

Clicker-first = **minimum viable retro**. Ship pixel mode in clicker, validate, then expand to full ArtEvolver and tournament. No big-bang rewrite.

---

**Verdict**: The design respects hardware authenticity, preserves algorithmic elegance, and delivers progressive complexity. The greats would concur.

---

## References

- [Sega Genesis VDP Guide](https://megacatstudios.com/blogs/press/sega-genesis-mega-drive-vdp-graphics-guide-v1-2a-03-14-17)
- [SNES Specs](https://en.wikibooks.org/wiki/Super_NES_Programming/SNES_Specs)
- [SNES Hi-Res 512×448](https://sneslab.net/wiki/Horizontal_Pseudo_512_Mode)
- [GBA Graphics](https://www.coranac.com/tonc/text/video.htm)
- [GB/GBC Pandocs](https://gbdev.io/pandocs/)
- [Genesis 512 Palette](https://pixeltao.itch.io/genesis-512-color-palette)
