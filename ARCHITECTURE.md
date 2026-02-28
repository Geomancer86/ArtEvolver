# ArtEvolver Architecture

This document describes the internal architecture of ArtEvolver, a Java genetic algorithm
application that evolves triangle mosaic art. It is intended for developers who need to
understand, modify, or extend the system.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Module Structure](#module-structure)
3. [Component Architecture](#component-architecture)
4. [Data Flow](#data-flow)
5. [Threading Model](#threading-model)
6. [Fitness Function](#fitness-function)
7. [Triangle Grid Layout](#triangle-grid-layout)
8. [Resolution Modes](#resolution-modes)
9. [Configuration Constants](#configuration-constants)

---

## System Overview

ArtEvolver takes a source image and attempts to reproduce it using a mosaic of colored
triangles drawn from a fixed color palette. It does this by running a parallel genetic
algorithm: multiple threads independently evolve populations of triangle drawings, competing
to minimize pixel-level differences against the source image. A central GUI periodically
synchronizes the best solutions across all threads and displays progress in real time.

The application is built as a multi-module Maven project targeting Java with a Swing-based
desktop interface.

---

## Module Structure

```
artevolver/
|
+-- artevolver-core/          Core evolution engine, rendering, GUI
|   +-- src/main/java/
|       +-- com/rndmodgames/evolver/
|           +-- ArtEvolver.java          GUI + orchestrator
|           +-- AbstractEvolver.java     Fitness function base class
|           +-- ImageEvolver.java        Core evolution engine (per-thread)
|           +-- DeltaFitnessEngine.java  Pre-computed pixel masks + delta evaluation
|           +-- LAPSolver.java          Jonker-Volgenant optimal color assignment
|           +-- CrossOver.java           Genetic operators (mutation, crossover)
|           +-- Triangle.java            Triangle polygon with color
|           +-- TriangleList.java        Scored list of triangles
|           +-- Palette.java             Color palette loader
|           +-- PalleteColor.java        Named color with RGB
|           +-- Renderer.java            PNG export
|
+-- artevolver-desktop/       Desktop launcher (thin module)
```

**artevolver-core** contains the entirety of the application logic: the evolution engine,
fitness evaluation, triangle rendering, palette loading, and the Swing GUI.

**artevolver-desktop** is a thin launcher module that provides the entry point for running
the application as a desktop program. It depends on `artevolver-core` and contains minimal
code of its own.

---

## Component Architecture

### ArtEvolver (GUI + Orchestrator)

`com.rndmodgames.evolver.ArtEvolver`

The top-level class that serves as both the Swing GUI and the central orchestrator for all
evolution threads.

**GUI responsibilities:**
- Swing JFrame with an image panel displaying the current best evolved image
- Professional sidebar control panel (v3.1) with organized sections:
  - Status dashboard: score, progress bar, gain, speed, elapsed, population, method
  - Actions: Load, Start/Stop, Toggle Source, Export
  - Settings: initialization method, evolution method, threads, population, mutations
  - Live parameter adjustment during evolution via "Apply Settings Live"
- All evolution parameters user-selectable from dropdown/spinner/checkbox controls

**Orchestration responsibilities:**
- Creates N `ImageEvolver` instances, each assigned to its own thread
- Runs a Swing `Timer` that fires at regular intervals to:
  - Poll all evolvers for their current best scores and images
  - Identify the global best solution across all threads
  - Synchronize the best population back to all evolvers (shared best solution)
  - Update the GUI display with the current best image
  - Export video frames as PNGs at a configurable FPS (optional)
- Tracks health metrics: monitors the count of "good" iterations (improvements) over a
  sliding window of the last 1000 frames
- Dynamically adjusts evolution parameters (e.g., mutation rates, grid sizes) based on
  health metrics to escape stagnation

**Control flow:**
```
ArtEvolver
    |
    +-- creates N ImageEvolver instances
    |       |
    |       +-- Thread 1
    |       +-- Thread 2
    |       +-- ...
    |       +-- Thread N
    |
    +-- Swing Timer (periodic)
            |
            +-- poll all evolvers
            +-- find global best
            +-- sync best to all evolvers
            +-- update display
            +-- export frame (optional)
```

### AbstractEvolver (Fitness Function Base)

`com.rndmodgames.evolver.AbstractEvolver`

Base class that provides the pixel-comparison fitness function used by all evolvers.

**Key method:**
- `compare(BufferedImage img1, BufferedImage img2)` -- Compares two images pixel by pixel
  and returns a fitness score as a `double` in the range 0.0 to 1.0, where 1.0 represents
  a perfect match.

**Performance optimizations (post-optimization):**
- Bulk pixel array access via `getRaster().getDataBuffer()` instead of per-pixel `getRGB()`
- One-time reference pixel caching via `cacheReferencePixels(BufferedImage ref)`, which
  extracts the source image pixels into a reusable int array
- Zero-copy `DataBufferInt` access to avoid intermediate array allocation

### LAPSolver (Optimal Color Assignment) — v3.1

`com.rndmodgames.evolver.LAPSolver`

Implements the Jonker-Volgenant (1987) shortest-augmenting-path algorithm for the Linear
Assignment Problem. Given an N x N cost matrix, finds the permutation that minimizes total
cost in O(N^3) time.

**Key methods:**
- `solve(int[] cost, int n)`: Core JV solver operating on a flat row-major cost matrix
- `buildCostMatrix(DeltaFitnessEngine, Color[])`: Builds cost[i][j] = pixel error when
  triangle i gets color j, using pre-computed pixel masks for efficiency
- `solveOptimal(DeltaFitnessEngine, Color[])`: Convenience method that builds + solves + logs timing

**Performance (N=1482, 1-palette):**
- Cost matrix build: ~40ms
- Solve: ~5-6 seconds
- Result: provably optimal color assignment (score 0.4893319 vs Smart's 0.4892470)

**Integration:** Called from `ImageEvolver.lapAssignColors()` when `INITIALIZATION_METHOD == INIT_LAP_OPTIMAL`.
Selectable from the UI initialization method dropdown.

---

### ImageEvolver (Core Evolution Engine)

`com.rndmodgames.evolver.ImageEvolver` extends `AbstractEvolver` implements `Runnable`

The core evolution engine. Each instance runs in its own thread and independently evolves a
small population of triangle drawings.

**Population:**
- Each `ImageEvolver` maintains its own population of `TriangleList<Triangle>` objects
- Population size is small: typically 2-8 individuals
- Each individual is a complete triangle mosaic (all triangles with assigned colors)

**Main evolution loop -- `evolve(long start, int iterations)`:**

```
for each iteration:
    1. Select two parents
       - Best individual, or fitness-proportional selection
    2. Create child via CrossOver.getChild(parent1, parent2)
       - Applies crossover and mutation operators
    3. Render the parent (if not yet scored) and the child
       - renderTriangles() draws all triangles to a BufferedImage
    4. Compare fitness via compare(rendered, sourceImage)
       - Pixel-level RGB difference, normalized to 0.0-1.0
    5. Keep the better solution, replace the worse one
       - Population improves monotonically within each thread
```

**Thread entry point -- `run()`:**
- Loops indefinitely, calling `evolve()` in batches
- Sleeps when paused (controlled by volatile flag)

**Rendering:**
- `renderTriangles()` -- Renders triangles into a reusable image buffer (avoids allocation)
- `renderTrianglesToNewImage()` -- Renders to a newly allocated BufferedImage (used when a
  fresh copy is needed)

**Static utility methods:**
- `switchColor()` -- Swaps the color of a triangle with another specific color
- `switchRandomColor()` -- Assigns a completely random palette color to a triangle
- `switchGridColor()` -- Swaps colors within a localized grid section
- `switchCloseColor()` -- Swaps a color with a nearby color in palette order
- `roll()` -- Random chance check against a threshold

**Performance optimizations (post-optimization):**
- Reusable image buffers to avoid per-frame allocation
- `ThreadLocal<SplittableRandom>` for fast, uncontended random number generation
- Volatile flags for pause/stop cross-thread visibility

### CrossOver (Genetic Operators)

`com.rndmodgames.evolver.CrossOver`

Contains all genetic operators: crossover, mutation, and parameter decay.

**Primary method -- `getChild(parent1, parent2)`:**
1. Selects a base parent (50/50 chance of either parent)
2. Copies the base parent's triangle colors into the child
3. Applies a series of mutation operators, each with independent configurable probability

**Mutation types:**

| Mutation         | Description                                                     |
|------------------|-----------------------------------------------------------------|
| Random Close     | Swap two colors that are nearby in palette order, bounded by    |
|                  | `randomJumpDistance`                                            |
| Fully Random     | Swap any two randomly selected colors (unrestricted)            |
| Grid             | Swap colors within a specific grid section (localized mutation  |
|                  | preserving spatial locality)                                    |
| Random Grid      | Swap colors in a randomly chosen grid section                   |

**Additional methods:**
- `getGeneticChild()` -- Chunked crossover that alternates blocks of triangles from each
  parent, producing offspring that inherit contiguous regions from both parents
- `mutate()` -- Applies close-color mutations (small perturbations in color space)
- `halveGridSize()` -- Tournament decay function that reduces mutation chances over time,
  narrowing the search as the population converges

**Performance optimizations (post-optimization):**
- `ThreadLocalRandom` for uncontended random number generation across threads
- `synchronized` on `halveGridSize()` to prevent race conditions during decay
- `volatile` on shared fields that are read across threads

### Triangle

`com.rndmodgames.evolver.Triangle` extends `java.awt.Polygon`

Represents a single triangle in the mosaic grid.

**Fields:**
- `xPoly`, `yPoly` -- Polygon vertex coordinates (inherited from `java.awt.Polygon`)
- `color` -- The `java.awt.Color` assigned to this triangle
- `colorId` -- Integer index into the palette
- `PalleteColor` reference (on master branch) -- Links to the named palette color object

Each triangle in a drawing has exactly one color from the palette. Evolution operates by
reassigning colors to triangles, not by moving or reshaping them. The grid geometry is fixed
for the lifetime of a run.

### TriangleList\<E\>

`com.rndmodgames.evolver.TriangleList<E>` extends `ArrayList`

A simple extension of `ArrayList` that adds a `score` field (`Double`) for fitness tracking.

Used in two contexts:
1. As an individual drawing: a list of `Triangle` objects with an associated fitness score
2. As the population collection within an `ImageEvolver`: a list of scored drawings

### Palette (Color Palette Loader)

`com.rndmodgames.evolver.Palette`

Loads color palettes from text files and provides them to the evolution engine.

**File format:**
```
ID  Name  R  G  B
1   Snowbound  233  227  214
2   Creamy     232  222  196
...
```

**Features:**
- Supports repetitions: each color can be duplicated N times, effectively increasing the
  resolution of the mosaic (more triangles share the same palette, allowing finer detail)
- Sorting options: Red, Green, Blue, Luminescence, or random shuffle
- Sorting order affects the behavior of "close color" mutations, since closeness is
  defined by palette index distance

**Available palettes:**

| Palette          | Colors |
|------------------|--------|
| Sherwin-Williams | 1,535  |
| GBC Green        | 4      |
| Black & White    | 2      |
| Trilux           | 12     |

### PalleteColor (Named Color)

`com.rndmodgames.evolver.PalleteColor`

Represents a single named color entry from a palette file.

**Fields:**
- Unique integer ID
- Human-readable name (e.g., "Snowbound")
- `java.awt.Color` instance with RGB values
- Reference back to the parent `Palette`

### Renderer (PNG Export)

`com.rndmodgames.evolver.Renderer`

Handles exporting evolved images to PNG files at arbitrary resolutions.

**Key method -- `renderToPNG()`:**
- Renders the triangle mosaic to a scaled PNG file
- Supports arbitrary scale factors (1x through 16x or higher)
- Uses `AffineTransform` for scaling the graphics context
- Draws both `fillPolygon` (solid color) and `drawPolygon` (outline), so triangle outlines
  are visible in exported images

---

## Data Flow

The end-to-end pipeline from source image to evolved output:

```
+-------------------+
| Source Image      |    User loads a JPG/PNG via the GUI
| (JPG/PNG)         |
+---------+---------+
          |
          v
+---------+---------+
| Resize to grid    |    Image is scaled to match the triangle grid
| dimensions        |    dimensions (widthTriangles * scale, heightTriangles * scale)
+---------+---------+
          |
          v
+---------+------------------+
| Initialize population      |    N drawings created, each containing all
| (random color assignments) |    palette colors randomly assigned to triangles
+---------+------------------+
          |
          +-------+--------+--------+--- ... ---+
          |       |        |        |            |
          v       v        v        v            v
      +-------+-------+-------+-------+     +-------+
      |Thread1|Thread2|Thread3|Thread4| ... |ThreadN|
      |Evolver|Evolver|Evolver|Evolver|     |Evolver|
      +---+---+---+---+---+---+---+---+     +---+---+
          |       |        |        |            |
          |  Each thread independently:          |
          |  1. Select parents                   |
          |  2. CrossOver -> child               |
          |  3. Render triangles to buffer       |
          |  4. Compare pixels vs source         |
          |  5. Keep better / discard worse       |
          |       |        |        |            |
          v       v        v        v            v
      +---+-------+--------+--------+----+------+---+
      |          ArtEvolver Timer (periodic)         |
      |                                              |
      |  - Poll all threads for best score/image     |
      |  - Identify global best across all threads   |
      |  - Sync best population to all evolvers      |
      |  - Update GUI display                        |
      |  - Track health (good iterations / 1000)     |
      |  - Adjust parameters if stagnating           |
      |  - Export video frame PNG (optional)          |
      +----------------------------------------------+
```

---

## Threading Model

```
+------------------------------------------------------------------+
|                        JVM Process                                |
|                                                                   |
|  +---------------------+    +---------------------+              |
|  | Swing EDT           |    | Swing Timer Thread   |              |
|  | (Event Dispatch)    |    | (ArtEvolver timer)   |              |
|  | - GUI rendering     |    | - Polls evolvers     |              |
|  | - Button handlers   |    | - Syncs best solution|              |
|  +---------------------+    | - Exports frames     |              |
|                              +----------+----------+              |
|                                         |                         |
|              reads best score/image from each evolver             |
|              writes best population back to each evolver          |
|                                         |                         |
|    +------------+------------+----------+---+--- ... --+          |
|    |            |            |               |          |          |
|    v            v            v               v          v          |
|  +----+      +----+      +----+           +----+    +----+        |
|  | T1 |      | T2 |      | T3 |           | TN |   |T32 |        |
|  | IE |      | IE |      | IE |    ...    | IE |   | IE |        |
|  +----+      +----+      +----+           +----+    +----+        |
|  Worker threads (8-32), each running one ImageEvolver             |
|  Each has its own population -- no locking between workers        |
+------------------------------------------------------------------+

T = Thread, IE = ImageEvolver
```

**Thread roles:**

| Thread                  | Count | Role                                          |
|-------------------------|-------|-----------------------------------------------|
| Worker (ImageEvolver)   | 8-32  | Independent evolution, each with own population|
| Swing Timer             | 1     | Periodic polling, synchronization, GUI update  |
| Swing EDT               | 1     | Event dispatch, rendering                      |

**Synchronization strategy:**
- No explicit locking between worker threads; each `ImageEvolver` operates on its own
  population in isolation
- The Swing Timer thread reads best scores and writes the globally best population back to
  each evolver during its periodic tick
- After optimization: `volatile` flags ensure cross-thread visibility for pause/stop
  controls and shared state
- `CrossOver.halveGridSize()` is `synchronized` to prevent concurrent decay from multiple
  timer invocations

---

## Fitness Function

The fitness function lives in `AbstractEvolver.compare()` and measures how closely an
evolved image matches the source image.

**Algorithm:**

```
score = 0
for each pixel (x, y) in the image:
    score += |R_source - R_evolved|
    score += |G_source - G_evolved|
    score += |B_source - B_evolved|

normalized = 1.0 - (score / max_possible_score)

where max_possible_score = width * height * 3 * 255
```

**Result:** A `double` in the range [0.0, 1.0], where:
- `1.0` = pixel-perfect match
- `0.0` = maximum possible difference (all pixels maximally wrong)

**Performance characteristics (post-optimization):**
- Bulk array comparison: pixels are accessed as flat `int[]` arrays via `DataBufferInt`,
  avoiding per-pixel method call overhead from `getRGB()`
- Cached reference pixels: the source image pixels are extracted once via
  `cacheReferencePixels()` and reused across all comparisons
- `long` accumulator: avoids overflow risk when summing differences across large images

---

## Triangle Grid Layout

The mosaic is composed of isosceles triangles arranged in a regular grid pattern.

```
Row 0:   /\  /\  /\  /\  /\  /\
        /  \/  \/  \/  \/  \/  \
Row 1:  \  /\  /\  /\  /\  /\  /
         \/  \/  \/  \/  \/  \/
Row 2:   /\  /\  /\  /\  /\  /\
        /  \/  \/  \/  \/  \/  \
        ...
```

**Parameters:**
- `widthTriangles` -- Number of triangles per row
- `heightTriangles` -- Number of triangle rows
- `triangleScaleWidth` -- Horizontal size of each triangle in pixels
- `triangleScaleHeight` -- Vertical size of each triangle in pixels

**Total triangle count:** `widthTriangles * heightTriangles`

For the default 4-palette mode: 100 x 57 = 5,700 triangles.

Each triangle in the grid has exactly one color assigned from the palette. The grid geometry
is fixed at initialization and does not change during evolution. Only the color assignments
evolve.

---

## Evolution Algorithm (v3.1)

The evolution loop in `ImageEvolver.evolve()` uses a steady-state genetic algorithm with
several mutation operators working in concert.

### Initialization

1. **Geometry creation**: Isosceles triangles are arranged on a grid
2. **Smart color assignment** (v3.1): Each triangle's region is sampled from the source image
   (7 points: centroid, 3 vertices, 3 midpoints). A greedy algorithm assigns the nearest
   unused palette color to each triangle, prioritizing high-saturation regions first.
   This produces an initial fitness of ~0.51 vs ~0.49 for random assignment.

### Mutation Operators (in CrossOver.getChild)

Operators are applied in this order (exploration first, then guided refinement):

| Order | Operator | Description | Per-child count |
|-------|----------|-------------|-----------------|
| 1 | **Spatial Block Crossover** | Injects a contiguous block of colors from secondary parent into primary parent copy, maintaining permutation via HashMap tracking | 1 block (1/12 to 1/4 of triangles) |
| 2 | **Grid Swap** | Swaps two random colors within a grid section (localized) | ~32 per child (decays) |
| 3 | **Random Swap** | Swaps two fully random colors (global), with probabilistic count preserving variance | ~1 per child (0-3 range) |
| 4 | **Close Swap** | Swaps nearby colors in palette order (gated: only active when CLOSE_MUTATIONS_PER_CHILD > 0) | 0 (disabled by default) |
| 5 | **Targeted Swap** | Finds worst-matching triangle, searches for best net-improvement swap partner (guided repair after random noise) | 12 per child |

The ordering is critical: random mutations (steps 1-4) add exploration diversity,
then targeted swap (step 5) repairs the worst damage, giving the child both diversity
and guided refinement.

### Targeted Swap Algorithm (v3.1)

```
for each attempt:
    1. Sample 384 triangles, find the one with largest
       RGB distance between its color and source image centroid
    2. Search 512 candidates for the swap that maximizes:
       improvement = (before_distA + before_distB) - (after_distA + after_distB)
    3. Only execute swap if net improvement > 0
```

This ensures every targeted swap improves overall local fitness for both positions involved.

### Delta Fitness Engine (v3.1 Phase 1)

The `DeltaFitnessEngine` eliminates the two most expensive operations (rendering + full pixel
comparison) by pre-computing triangle pixel masks at initialization.

**Architecture:**

```
At initialization (one-time, ~200ms):
  1. Render all triangles with index-encoded colors (single pass)
  2. Read back pixel array → build int[][] masks (which pixels belong to each triangle)
  3. Cache reference image R/G/B channels as flat arrays
  4. Compute initial total diff including background pixels

Per swap evaluation (in-place, no rendering):
  1. For triangle A's pixels: subtract old diff, add new diff with B's color
  2. For triangle B's pixels: subtract old diff, add new diff with A's color
  3. Return delta (negative = improvement)
  4. If accepted: update internal color state + totalDiff
```

**Key methods:**
- `computeSwapDelta(triA, triB)` — O(pixels_per_triangle) swap evaluation (~650 ops)
- `trySwap(triA, triB)` — evaluate + accept if improving (hill-climbing)
- `applySwapWithDelta(triA, triB, delta)` — commit with pre-computed delta
- `getTriangleError(triIdx)` — per-triangle error for targeted mutation guidance
- `getScore()` — convert totalDiff to 0..1 score (identical to AbstractEvolver.compare())
- `syncFromTriangles()` — resync from TriangleList after external modifications

**Performance:**
- Score accuracy: exact match with full render+compare (0.00 difference)
- Raw swap throughput: 3.3 million swaps/sec (single thread)
- Iteration throughput: 51.6x faster than legacy (83 → 4,295 iter/sec)

### evolveDelta() — Delta-Based Evolution Loop

The `evolveDelta()` method in ImageEvolver uses the DeltaFitnessEngine for all fitness
evaluation. It works in-place on the best individual with no deep copies or rendering.

**Per iteration:**
1. **Grid-localized swaps** — `GRID_MUTATION_CHANCES` random swaps within grid sections
2. **Random global swaps** — probabilistic count preserving variance (~1 per iteration)
3. **Targeted swaps** — find worst-error triangle, search for best swap partner via delta

**UI sync:** Every 50 iterations, colors are synced back to the TriangleList and the image
is rendered for display.

The `run()` method automatically selects `evolveDelta()` when a DeltaFitnessEngine is available,
falling back to legacy `evolve()` otherwise.

### Selection and Replacement (Legacy evolve())

- **Parent selection**: ParentA = last in population (worst), ParentB = random
- **Replacement**: Child replaces parent if child's fitness > parent's fitness
- **Elitism**: Global best score is tracked; best image is preserved across all threads

### Parameter Decay (Tournament)

Every `tournamentRoundSize` iterations (default 80):
- `GRID_MUTATION_CHANCES` decreases by `GRID_MUTATION_DECAY` (0.1)
- `RANDOM_MUTATION_CHANCES` decreases by `RANDOM_MUTATION_CHANCES_SUBSTRACT`
- `randomJumpDistance` is halved (minimum 1)

This progressively shifts from exploration (many random mutations) to exploitation
(fewer, more precise mutations) as evolution progresses.

---

## Resolution Modes

Resolution is controlled by the number of palette repetitions. More repetitions mean more
unique color slots, which requires more triangles to hold them, producing a finer grid and
higher-resolution output.

| Mode | Palette Repetitions | Approx. Triangles | Grid (W x H)  |
|------|---------------------|--------------------|----------------|
| 1x   | 1                   | ~1,482             | 38 x 39        |
| 2x   | 2                   | ~2,970             | 54 x 55        |
| 4x   | 4                   | ~5,852             | 76 x 77        |
| 8x   | 8                   | ~12,210            | 110 x 111      |
| 16x  | 16                  | ~24,492            | 156 x 157      |
| 32x  | 32                  | ~33,972            | 228 x 149      |
| 64x  | 64                  | ~68,160            | 320 x 213      |

Higher modes produce more detailed images but require proportionally more computation per
fitness evaluation (more pixels to compare) and per evolution step (more triangles to
render and mutate).

---

## Configuration Constants

Key constants defined in `ArtEvolver.java`:

| Constant            | Description                                          | Default       |
|---------------------|------------------------------------------------------|---------------|
| `CURRENT_MODE`      | Selects the resolution mode, which determines thread | (varies)      |
|                     | count, population size, and triangle scale            |               |
| `EVOLVE_ITERATIONS` | Number of evolution iterations per batch call         | 2             |
| `IMAGE_TYPE`        | BufferedImage type used for rendering                | TYPE_INT_ARGB |
| Video FPS           | Frame rate for video frame export                    | (configurable)|
| Export resolution   | Scale factor for PNG export                          | (configurable)|

Mode selection via `CURRENT_MODE` adjusts multiple parameters simultaneously: the number of
worker threads, the population size per evolver, the triangle grid dimensions, and the
triangle scale factors. This provides preset configurations tuned for different hardware
capabilities and desired output quality.

---

## Planned: Retro Console Mode (v3.3)

Retro game console palettes and resolutions — GB (160×144, 4 colors), GBC (32), GBA (240×160),
Genesis (320×224, 512), SNES (256×224). **Pixel mode**: one color per pixel, square pixels,
true pixel art output. New components: `PixelGrid`, `PixelFitnessEngine`, `RetroPreset`.
Same swap-and-evaluate algorithm; data structure changes from triangles to pixels.

**Design validation**: Architecture and presets validated against principles of Yokoi (lateral
thinking with withered technology), Miyamoto (constraints as playground), Kojima (authenticity),
Carmack (clean parallel path), and others. See `RETRO_CONSOLE_DESIGN.md` (Designer Validation
section), `RETRO_PRESETS.md`.
