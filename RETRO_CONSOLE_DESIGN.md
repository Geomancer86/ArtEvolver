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

### Phase 1: Foundation (Clicker-only) ✅ IMPLEMENTED

1. ✅ **PaletteLoader** — `PaletteLoader.java`: loads from resource files or generates algorithmically.
2. ✅ **RetroPreset** — `RetroPreset.java`: enum with resolution, palette resource, color count.
3. ✅ **PixelGrid** — `PixelGrid.java`: permutation init (nearest-color histogram + shuffle), O(1) swap.
4. ✅ **PixelFitnessEngine** — `PixelFitnessEngine.java`: O(1) swap delta, same scoring formula.
5. ✅ **ClickerEngine** — Dual mode: triangle (original) + pixel (retro). Same API for both.
6. ✅ **DashboardServer** — `?preset=gb_dmg` routes to pixel mode, resizes source automatically.
7. ✅ **clicker.html** — 10 preset buttons (Classic + 9 retro) with info descriptions.
8. ✅ **Unit tests** — 8 tests in `PixelModeTest.java` covering all new components.

### Phase 2: Integration

8. **ArtEvolver** — Add retro preset to sidebar (optional).
9. **Tournament** — Support retro presets in EvolutionConfig (optional).

### Phase 3: Polish (Retro Gallery — ✅ IMPLEMENTED)

10. ✅ **Level select UI** — Console tab bar (Classic/GB/Genesis/GBA/SNES), sub-preset buttons, preview panel with quantized thumbnails.
11. ✅ **Retro samples** — 10 console-specific programmatic samples (2 per family) in new "Retro" category with `recommendedPreset` field.
12. ✅ **Preview endpoint** — `GET /api/clicker/sample/{id}?preset=X` quantizes any image through a console palette. `PixelGrid.quantize()` / `quantizeScaled()` static utilities.
13. ✅ **RetroPreset.family** — Family grouping field for tab organization.
14. **Export** — Optional 1:1 pixel export (no scaling) for retro presets.

---

## File Impact

| File | Change |
|-----|--------|
| `Palette.java` | Add `loadFromResource(String name)` or constructor(name) |
| `DashboardServer.java` | Handle `preset` param, pass to init |
| `ClickerState.java` | `initEngine(..., String preset)` |
| `ClickerEngine.java` | Branch: pixel mode vs triangle mode |
| **New** `PixelGrid.java` | ✅ Pixel grid data structure |
| **New** `PixelFitnessEngine.java` | ✅ O(1) swap delta for pixels |
| **New** `RetroPreset.java` | ✅ Enum of presets |
| **New** `PaletteLoader.java` | ✅ Resource + algorithmic palette loading |
| **New** `gb_dmg.txt`, `gb_gray.txt`, `gbc.txt`, `gbc_high.txt` | ✅ Palette files |
| **New** `PixelModeTest.java` | ✅ 8 unit tests |
| `clicker.html` | ✅ Preset selector UI |

---

## Effort Estimate

| Phase | Effort | Risk | Status |
|-------|--------|------|--------|
| Phase 1 | Medium (3–5 days) | Low — isolated to clicker | ✅ Complete |
| Phase 3 | Medium (2–3 days) | Low — UI + samples | ✅ Complete |
| Phase 2 | Low (1–2 days) | Low | Pending |

**Phases 1 & 3 complete.** Full level-select UI with console tabs, retro sample gallery,
and preview endpoint live. Human testing: run `clicker.bat`, pick a console tab,
browse images with quantized previews, and click Begin.

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

## Algorithm Deep Analysis: Triangles vs Pixels

*This section addresses a fundamental question: the triangle evolution uses a fixed number
of colors in a permutation. Pixels are different — each color can be freely assigned.
What does this mean for the algorithm?*

### Why Triangle Evolution Works

In the triangle system, the problem is **genuinely hard**:

1. Each triangle covers **hundreds of source pixels** — there is no single "correct" color
2. The number of triangles equals the number of palette color tokens (with repetitions)
3. It's a **permutation problem**: assign N colors to N triangles, each color used exactly once
4. Even the LAP solver takes seconds to find the optimal assignment
5. Smart init gives ~51% fitness; evolution pushes it to 85%+

The hardness comes from the **many-to-one mapping**: many source pixels → one triangle color.

### The Pixel Problem: Too Easy?

With pixels, each cell covers **exactly one source pixel**. The nearest palette color is
trivially computable:

```
for each pixel (x, y):
    bestColor = nearestColor(sourcePixel[x][y], palette)
    output[x][y] = bestColor
```

**If colors are freely assignable, the problem is solved in O(W×H) with zero evolution.**

This would make the game meaningless. No clicks needed. No progression. Done.

### The Three Approaches

#### Approach 1: Permutation Mode (Fixed Histogram)

**"Same game, different pieces."**

Treat pixels like triangles — fix the color histogram, scramble, evolve via swaps.

**Setup:**
1. For each source pixel, compute the nearest palette color
2. Count occurrences → this is the **target histogram**
   - GB example: {color0: 2,100, color1: 5,400, color2: 9,800, color3: 5,740} = 23,040 px
   - Genesis: 512 colors, each appearing ~140× on average (but unequally!)
3. Create a pool of exactly those color tokens
4. Randomly shuffle them across the pixel grid
5. Evolution = swap two pixels' colors. Accept if fitness improves.

**Why this is NOT trivially solved:**

Even with the "right" histogram, the *arrangement* matters. Consider:
- 5,400 pixels want color1, but only 5,400 tokens of color1 exist
- If pixel A and pixel B both want color1, but only one copy remains...
- **Which pixel gets the scarce color?** This is a constrained assignment problem.
- For large palettes (Genesis 512, SNES 32k), this is computationally hard

The permutation constraint creates **scarcity** — not every pixel can have its ideal color.
Some pixels must accept a sub-optimal color because the quota is filled elsewhere. The
evolution finds the best arrangement given those constraints.

**Starting fitness from random permutation:**

| Console | Pixels | Colors | Avg Copies | Random Start Fitness | Notes |
|---------|--------|--------|------------|---------------------|-------|
| GB | 23,040 | 4 | 5,760 | ~40-55% | Right tones, wrong positions |
| GBC | 23,040 | 32 | 720 | ~30-45% | Closer colors, more options |
| Genesis | 71,680 | 512 | 140 | ~25-40% | Many similar colors |
| GBA | 38,400 | 32,768 | 1.17 | ~15-25% | Nearly unique assignment |
| SNES | 57,344 | 32,768 | 1.75 | ~15-25% | Nearly unique assignment |

For GB/GBC/Genesis, random permutation already has the right **overall color balance** — the
image looks like a noisy, blurred version of the target. Evolution sharpens it. This creates
a satisfying visual progression: colored noise → vague shapes → recognizable image → sharp.

**GBA/SNES problem:** With 32,768 colors and ~40-57k pixels, many colors appear only 1-2
times. The histogram is nearly flat. Some colors might not appear at all (pixels ÷ colors ≈ 1.2-1.75).
The "permutation" becomes almost a free assignment. This is fine — the evolution still works,
and the visual progression from random to sorted is engaging.

**Pixel counts for equal vs optimal histogram:**

| Console | Pixels | Colors | Equal Reps | Nearest-Color Histogram |
|---------|--------|--------|-----------|------------------------|
| GB | 23,040 | 4 | 5,760 each | Skewed (e.g., 60/20/12/8%) |
| GBC | 23,040 | 32 | 720 each | Variable (some colors dominant) |
| Genesis | 71,680 | 512 | 140 each | Some colors 0, others 500+ |
| GBA | 38,400 | 32,768 | ~1.17 each | Many colors 0-1, some 5+ |
| SNES | 57,344 | 32,768 | ~1.75 each | Many colors 0-1, some 10+ |

**Recommendation:** Use **nearest-color histogram** (not equal). This respects the source
image's color distribution. A sunset image needs more reds/oranges; a forest needs more greens.
Equal repetitions would force unnatural color balance.

**Algorithm:**
```
1. Resize source to console resolution
2. For each pixel, find nearest palette color → target histogram
3. Create color token pool matching histogram
4. Randomly distribute tokens across grid
5. Evolution: swap two random pixels' colors
6. Delta fitness: O(1) — only 2 pixels change per swap
7. Accept if fitness improves
```

**Pros:**
- Direct analogy to triangle system — proven, understood
- Same clicker gameplay (click = swap attempt)
- Satisfying visual progression (scrambled → sorted)
- Long runway: tens of thousands of swaps to converge
- Works for ALL consoles (GB through SNES)

**Cons:**
- Histogram is fixed at init — can't discover that the image needs MORE of color X
- For high-color consoles (GBA/SNES), histogram is nearly trivial

---

#### Approach 2: Free Assignment Mode

**"Each pixel chooses its own color."**

No permutation constraint. Each mutation picks a random pixel and changes it to a random
palette color. Accept if fitness improves.

**Setup:**
1. Assign random palette colors to all pixels
2. Each mutation: pick pixel, pick random color from palette, evaluate
3. Accept if better

**Why this is problematic:**

For **small palettes** (GB, 4 colors): each pixel has 25% chance of starting correct.
Each mutation on a wrong pixel has ~25-100% chance of improvement (depending on distance).
**Convergence is extremely fast** — the image is near-optimal in a few hundred mutations
per pixel. For 23,040 pixels × ~3 attempts each ≈ 70,000 clicks to near-perfect.
In clicker terms, that's trivially short.

For **large palettes** (Genesis 512, SNES 32k): each pixel starts with ~0.2% chance of
being correct. Random color selection has very low hit rate. Each mutation tests one color
from 512+. **Convergence is painfully slow** — random search through a huge space.
A "smart" mutation (pick nearest color) would solve it instantly, defeating the purpose.

The fundamental problem: **no spatial correlation**. Each pixel is independent. There's no
interesting emergence — just per-pixel nearest-color search. No unscrambling, no shapes
appearing from chaos. Visually boring.

**Pros:**
- Simplest implementation
- No histogram computation needed
- Natural histogram emerges

**Cons:**
- GB/GBC: converges too fast (trivial game)
- Genesis/SNES: converges too slowly (frustrating) OR trivially solved with smart mutation
- No interesting visual progression
- Each pixel is independent — no emergent behavior
- Not a real game

**Verdict: Not recommended as primary mode.** But elements can be borrowed (see Approach 3).

---

#### Approach 3: Palette Evolution Mode (New Algorithm)

**"Evolve which colors to use, not just where to put them."**

For consoles where on-screen colors are a SUBSET of the full palette (Genesis: 64 from 512,
SNES: 256 from 32,768), the real artistic challenge is **choosing the palette**.

**The real hardware constraint:**
- Genesis: 512 colors in ROM, but only 64 on screen (4 palettes × 16 colors)
- SNES: 32,768 possible, but 256 palette entries
- GBA: 32,768 possible, but 256 entries in most modes
- GBC: 32,768 possible (15-bit), but 8 palettes × 4 colors = 32

**Two-level optimization:**
1. **Level 1 — Palette selection**: Choose which N colors from the master palette to use
2. **Level 2 — Pixel assignment**: Given the chosen palette, assign each pixel its nearest color

Level 2 is trivially solved once Level 1 is decided. So the evolution operates on Level 1:

```
1. Select a random subset of N colors from master palette
2. Assign each pixel its nearest color from the subset (instant)
3. Compute fitness
4. Mutation: swap one palette entry for a different master palette color
5. Reassign affected pixels (all that used the old color)
6. Evaluate new fitness
7. Accept if better
```

This is genuinely interesting because:
- Changing one palette color cascades to many pixels
- Small palette changes can dramatically shift the image's character
- The search space is C(512, 64) ≈ 10^73 for Genesis — enormous!
- Visual effect: image shifts between "color moods" as the palette evolves

**Clicker integration:**
- Click = attempt to swap one palette entry
- Each click is impactful (affects many pixels)
- Visual feedback is dramatic (entire regions shift color)
- Palette displayed as a sidebar — you see it evolve

**Pros:**
- Authentic to real hardware constraints
- Genuinely hard optimization problem
- Dramatic visual feedback per click
- Works naturally for Genesis/SNES/GBA
- New gameplay that doesn't exist in triangle mode

**Cons:**
- Doesn't apply to GB (only 4 fixed colors, nothing to select)
- New algorithm, not a simple adaptation of swap-and-evaluate
- Per-mutation cost is O(affected_pixels) not O(1)
- More complex implementation

---

### Recommended Strategy: Phased Hybrid

**Phase 1: Permutation Mode (all consoles)**

Start with Approach 1 for all consoles. It works, it's proven, it integrates with the
clicker immediately. The scramble-to-sorted visual progression is satisfying and familiar.

```
gb_dmg:  4 colors, 23,040 pixels → histogram from nearest, scramble, swap to sort
gbc:     32 colors, 23,040 pixels → same
genesis: 512 colors, 71,680 pixels → same
gba:     32,768 colors, 38,400 pixels → same (some colors unused)
snes:    32,768 colors, 57,344 pixels → same (some colors unused)
```

**Phase 2: Palette Evolution (Genesis/SNES)**

Add Approach 3 as an alternate mode for Genesis, SNES, GBA. The player chooses:
- "Pixel Scramble" (permutation) — unscramble the image
- "Palette Evolution" — evolve which colors the console uses

**Phase 3: Hybrid Mode**

Mix permutation swaps (spatial refinement) with occasional histogram-changing reassigns.
The evolution can both move colors around AND adjust the color balance.

```
90% swaps (permutation) + 10% reassigns (free)
```

This lets the algorithm escape from a bad initial histogram while still having the
satisfying swap-based progression.

### Impact on Clicker Gameplay

| Mode | Click Effect | Visual | Convergence | Fun Factor |
|------|-------------|--------|-------------|------------|
| Permutation | Swap 2 pixels | Gradual sharpening | Long runway | ★★★★ |
| Free Assign | Change 1 pixel | Per-pixel dots | Too fast/slow | ★★ |
| Palette Evo | Swap palette entry | Color mood shifts | Medium | ★★★★★ |
| Hybrid | Mixed | Best of both | Long + adaptive | ★★★★★ |

### Mathematical Summary

| Property | Triangles (current) | Pixels (Approach 1) | Pixels (Approach 3) |
|----------|-------------------|--------------------|--------------------|
| Cells | ~1,482 (1x pal) | 23,040–71,680 | N/A (palette entries) |
| Colors | 1,535 (Sherwin) | 4–32,768 | N from master palette |
| Constraint | Permutation | Permutation | Subset selection |
| Swap cost | O(pixels_per_tri) | O(1) | O(affected_pixels) |
| Optimal solver | LAP O(N³) | LAP (simpler) | NP-hard (subset) |
| Smart init | ~51% | ~40-55% (GB) | Random palette |
| Game loop | Swap triangles | Swap pixels | Swap palette entries |

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
