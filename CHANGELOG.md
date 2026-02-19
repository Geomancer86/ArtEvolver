# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.1.0] - 2026-02-19 (develop branch)

### Added — Tournament Quick Setup Preset
- **Quick Setup** button in Tournament Manager — auto-generates contestants from CPU core count
  - User picks total CPU cores and threads-per-contestant; contestants are calculated automatically
  - Up to 8 distinct strategy presets: Balanced, Aggressive Explorer, Grid Refiner, Targeted Precision,
    Heavy Random, Close Mutation Focus, Fast Convergence, Wide Search
  - Each strategy applies different mutation/swap parameter ratios from the base UI settings
  - "Clear existing contestants" option for fresh setup or additive configuration

### Fixed — Tournament Mode Chart & Delta Evolution
- **Chart not showing data**: Fixed `bestScore > Double.MIN_VALUE` condition that prevented data points
  from being fed to the chart; changed to `> 0` across all code paths
- **`isDirty` flag consumed prematurely**: `TournamentContestant.updateBest()` no longer clears the
  evolver's dirty flag when the evolver hasn't produced a real score yet (still at `Double.MIN_VALUE`)
- **Chart single-point rendering**: `FitnessChartWindow` now renders with just 1 data point (as a dot)
  instead of requiring >= 2 points, eliminating the initial "Collecting data..." delay
- **`evolveDelta()` ignored per-contestant config**: Delta evolution was reading mutation parameters
  from static `CrossOver` fields instead of the evolver's `EvolutionConfig`, making all tournament
  contestants behave identically. Now reads `gridMutationChances`, `randomMutationChances`,
  `randomMutationPercent`, and `targetedSwapAttempts` from the per-instance config
- **`TournamentContestant.bestScore` initialization**: Changed from `Double.MIN_VALUE` to `0` for
  reliable comparison and display

### Fixed — Code Review Polish Pass
- **Active contestant combo resets to first**: `refreshContestantCombo()` was calling
  `removeAllItems()` which fired the action listener, resetting `selectedContestant` to null.
  Now uses a `refreshingCombo` guard flag and preserves the selected index across rebuilds.
- **Thread safety: `TournamentContestant.running`**: Made `volatile` for visibility across
  evolver threads and the EDT timer
- **Thread safety: `colorIndex`**: Changed from plain `int` to `AtomicInteger` to prevent
  duplicate colors under concurrent contestant creation
- **NPE guard in `updateBest()`**: Added null check on `ev.getBestImage()` before assignment
- **Tournament table selection lost on refresh**: `refreshTable()` now preserves the selected
  row across `fireTableDataChanged()` calls
- **Contestant list changes not synced to main combo**: Add/duplicate/remove/quick-setup in
  `TournamentManagerWindow` now calls `notifyContestantsChanged()` which updates both the table
  and the main ArtEvolver contestant combo box
- **`stopTournament()` combo refresh**: Now refreshes the contestant combo after stopping
- **`startTournament()` exception handling**: Wrapped per-contestant initialization in try-catch
  to prevent one failing contestant from blocking others
- **Table model bounds check**: Added defensive row bounds check in `getValueAt()`
- **`onContestantSelected` marks display dirty**: Setting `isDirty = true` when switching contestant
  ensures the canvas repaints immediately with the selected contestant's image

### Added — Tournament Mode
- **`EvolutionConfig`** — parameter bundle class replacing static CrossOver fields with instance-level configuration
  - Encapsulates all 15+ tunable GA parameters (mutation rates, probabilities, decay, crossover, population)
  - `fromCurrentSettings()` factory reads current static values; `applyToStaticFields()` writes back
  - `clone()` support for duplicating contestant configurations
  - `toSummary()` for compact parameter display
- **`TournamentContestant`** — encapsulates one independent evolution run
  - Own list of `ImageEvolver` threads, own `EvolutionConfig`, own best score/image/population
  - `createEvolvers()` + `initializeWithImage()` for initialization with shared source image
  - `start()`/`stop()` with daemon threads named per-contestant for easy identification
  - `updateBest()` polled by the process timer; syncs best population within contestant only
  - 10 preset chart colors automatically assigned to new contestants
- **`TournamentManagerWindow`** — full-featured management UI (separate JFrame)
  - Table view: #, color swatch, name, score, running status, parameter summary
  - **Add Contestant**: creates with current UI settings as defaults
  - **Duplicate Selected**: clones an existing contestant's full configuration
  - **Remove Selected**: with running-check guard
  - **Start All / Stop All**: controls entire tournament
  - **Edit Parameters**: double-click or button opens full parameter editor dialog
  - Detail panel showing all parameters for selected contestant
- **CrossOver config accessor layer** — `cfg()` helper + 10 accessor methods
  - When `evolverInstance.getConfig()` is non-null, reads parameters from config
  - Falls back to static fields when config is null (backward compat for tests/single mode)
  - Zero-breakage: all 27 existing tests pass unchanged
- **ImageEvolver config integration**
  - New `config` field with getter/setter
  - `run()` reads `evolveIterations` from config when available
- **FitnessChartWindow multi-series support**
  - `Map<String, Series>` replaces single data list
  - Each series: unique name, color, independent data/peak tracking
  - Color-coded legend drawn at bottom when multiple series active
  - Stats bar rebuilds dynamically as series are added
  - Backward-compatible `addDataPoint(long, double)` still works for single mode
- **ArtEvolver tournament integration**
  - `List<TournamentContestant>` field with selected-contestant tracking
  - "Tournament Mode" sidebar section with "Open Tournament Manager" button
  - Active Contestant combo box to select which contestant's image is displayed
  - Process timer polls all contestants via `updateBest()`
  - Chart receives data for each contestant with distinct series ID and color
  - `startTournament()` / `stopTournament()` orchestration
  - `populateConfigFromUI()` for seeding new contestants from current sidebar values
  - Population counter aggregates both single-mode evolvers and all tournament contestants

### Added — Configurable UI Parameter Panels & Real-Time Fitness Chart
- **Sidebar reorganized into three clear categories**:
  - **QUALITY** — Grid Width, Grid Height, Palette Repetitions, Color Assignment Method
  - **GENETIC ALGORITHM** — Evolution Method, Population, Crossover, Iterations, Max Iterations, all Mutation operators with probability/decay sub-controls
  - **SPEED & PERFORMANCE** — Threads (with detected CPU core count), Evolution Loop FPS, GUI Update FPS, Validate Permutation, Benchmark Logging, Export Video
- **New UI controls exposed**:
  - Grid Width (`widthTriangles`) and Grid Height (`heightTriangles`) spinners — with reload-required tooltip
  - Evolution Loop FPS (`FPS`) — controls evolution timer speed, live-adjustable
  - Max Iterations (in thousands) — set iteration limit or 0 for unlimited
  - Grid Mutation Decay (x1000) — controls how fast grid mutations reduce per tournament
  - Random Mutation Probability (x10000) — fine-tune random mutation likelihood
  - Close Mutation Probability (x10000) — fine-tune close mutation likelihood
- **FitnessChartWindow** — separate movable/resizable JFrame with real-time fitness chart
  - Dark theme with gradient area fill and antialiased line rendering
  - Auto-scaling Y-axis tracking score progression
  - X-axis shows iterations with K/M formatting
  - Stats bar: current score, peak score, sample count, gain per minute
  - Peak score dashed indicator line
  - Data thinning when exceeding 10,000 samples
  - Opens from "Show Fitness Chart" button in the STATUS section
- **Apply Live** button now handles all GA + Speed parameters without restart
  - Population, Crossover Max, Evolution Method, Validate Permutation, Max Iterations all live-adjustable
  - Evolution Loop FPS changes timer delay immediately via `processTimer.setDelay()`

### Changed — Default Settings Restored to Master Branch (paint-by-colors)
- `CURRENT_MODE` changed from `QUALITY_MODE_STREAM` back to `QUALITY_MODE`
- `widthTriangles` changed from 100 back to **80** (40 visual columns)
- `heightTriangles` changed from 57 back to **53** (26 visual rows)
- `triangleScaleHeight/Width` defaults restored to **1.0f** (from 3.0f)
- `width/height` formula restored to **2.5f * scale** (from 3.0f * scale)
- `QUALITY_MODE` threads restored to **24** (from 32), width formula to **2f * scale** (from 3.0f)
- `EXPORT_VIDEO` and `VIDEO_4K_RESOLUTION_EXPORT` both set to **false** (were true)
- These match the `master` branch settings used for actual paint-by-colors production

### Added — Paint-by-Colors Export System
- **PalleteColor tracking through entire evolution pipeline**
  - `PalleteColor` now has proper encapsulation with `getId()`, `getName()`, `getPallete()` accessors
  - `Triangle` carries `PalleteColor palleteColor` alongside `Color color` through all operations
  - All swap methods (`switchColor`, `switchRandomColor`, `switchGridColor`, `switchCloseColor`, `targetedSwap`) propagate PalleteColor
  - `DeltaFitnessEngine` tracks `PalleteColor[]` array alongside RGB, swapping in `applySwap()`/`applySwapWithDelta()`
  - `syncDeltaToTriangles()` writes PalleteColor back from engine to TriangleList
  - `CrossOver.getChild()` spatial block crossover propagates PalleteColor during color swaps
  - `CrossOver.mutate()` and `getSecuentialChild()` preserve PalleteColor in child copies
  - `smartAssignColors()` and `lapAssignColors()` both reassign PalleteColor with their color assignments
- **Renderer.renderPaintByNumbersPNG()**: high-res export with Sherwin-Williams color names rendered on each triangle
  - Two-pass rendering: fill triangles, then overlay centered labels with black outline + white text
  - Font size scales with export scale factor for readability at any resolution
- **Renderer.renderOutlineGuidePNG()**: clean white-background outline guide with color names
  - Designed for printing as physical tile placement reference
  - Gray triangle outlines with black text labels
- **PaintByColorsExporter** (`com.rndmodgames.evolver.exporter`)
  - `printColorAssignment()`: console summary of all triangle color assignments
  - `exportToCSV()`: CSV file with TriangleIndex, ColorId, ColorName, R, G, B columns
  - `exportMaterialsList()`: bill of materials sorted by color name with counts
  - `validateAssignment()`: checks for triangles missing PalleteColor and reports gaps
- **ArtEvolver UI export buttons**
  - "Export Paint-by-Numbers" button — exports high-res labeled image
  - "Export Outline Guide" button — exports printable guide sheet
  - "Export Color Map CSV" button — exports CSV mapping + materials list

### Fixed — Pre-existing Scale Bugs
- `QUALITY_MODE_STREAM` / `VIDEO_4K_RESOLUTION_EXPORT`: fixed 4 copy-paste bugs where `triangleScaleHeight` was written as `triangleScaleWidth`
  - Lines 586, 592, 599: second assignment set width instead of height
  - Line 612: `height = 3.0f * triangleScaleWidth` corrected to `triangleScaleHeight`
  - These bugs had no visible effect when both scales were equal (the default), but would cause distorted aspect ratios in video export modes

### Added — LAP Solver Implementation
- **LAPSolver.java**: Jonker-Volgenant algorithm for provably optimal color assignment
  - O(N^3) solver computing the exact minimum-cost permutation
  - Cost matrix builder using DeltaFitnessEngine pixel masks (~40ms for N=1482)
  - Solve time: ~5-6 seconds for 1-palette (1482 triangles)
  - Achieves **provably optimal** score of 0.4893319 vs Smart's 0.4892470 (+0.000085)
  - Selectable from UI as "LAP Optimal (Jonker-Volgenant)" initialization method
- **ImageEvolver.INITIALIZATION_METHOD**: new static field with INIT_RANDOM, INIT_SMART, INIT_LAP_OPTIMAL
- **lapAssignColors()**: integration method that builds DeltaFitnessEngine + cost matrix + solves
- **BenchmarkTest.lapSolverOptimal()**: validates LAP > Smart > Random scores

### Added — Major UI Overhaul
- **Professional sidebar control panel** replacing the basic button list
  - Organized into labeled sections: Status, Actions, Initialization, Evolution, Mutations, Display
  - Color-coded score display with large monospaced font
  - Progress bar showing fitness percentage
  - Score gain tracking (shows improvement since start)
  - Elapsed time with hours/minutes/seconds formatting
  - Iteration speed with comma-formatted numbers
  - Triangle count with grid dimensions
  - Active method display (init + evolve combination)
- **User-selectable parameters** (all configurable from UI before/during evolution):
  - Initialization method: Random / Smart Greedy / LAP Optimal
  - Evolution method: Legacy (render+compare) / Delta Fitness (50x faster)
  - Threads (1-128), Population per thread (1-256)
  - Palette repetitions (1-64), Crossover max (1-64)
  - Evolve iterations per batch (1-100)
  - Grid mutations, Random mutations, Close mutations, Targeted swap attempts
  - Block crossover toggle, Permutation validation toggle
  - Benchmark CSV logging toggle, Video export toggle
  - GUI update FPS (1-60)
- **"Apply Settings Live" button**: changes mutation/display params during active evolution
- **Styled action buttons**: colored Start (green) / Stop (red), grid layout
- **Tooltips** on all controls explaining what each parameter does
- **Scrollable sidebar** for small screens

### Fixed — Test Infrastructure & Stability
- **Headless test mode**: Surefire now passes `-Djava.awt.headless=true`, tests run without
  opening Swing windows (no more blocking during automated builds)
- **JVM heap for tests**: Surefire fork gets `-Xmx2g` preventing OOM crash when all 27 tests
  run in a single JVM fork
- **ArtEvolver headless guard**: constructor skips `initComponents()` in headless mode,
  enabling tests that instantiate ArtEvolver without a display
- **CrossOver null-safety**: constructor null-guards `evolverInstance` access, fixing NPE
  in 3 unit tests that create CrossOver with `null` evolver
- **CrossOverTest.getAverageSuccessfulJumpSize**: fixed flaky test — was using 80x53 grid
  (4240 positions > 1535 palette colors) leaving most triangles null-colored; now uses
  38x39 grid (1482 <= 1535) where all triangles get assigned colors
- **BenchmarkTest.lapSolverOptimal**: save/restore static fields in try-finally to prevent
  test pollution between benchmark tests
- **Divide-by-zero guard**: ArtEvolver video export frame interval calculation now guards
  against `EXPORT_VIDEO_FRAMES_FPS = 0`
- **LAP solver unit tests**: new `lapSolverUnitTest` validates correctness on 1x1, 2x2, 3x3,
  and 4x4 known cost matrices with deterministic expected assignments

### Code Cleanup
- Removed dead methods: `evolveGreedy()`, `evolve2()`, `updateStats()`, `updateFitness()`,
  `GEN_SIZE` field, `KILL_PARENTS` field
- Removed unused imports: `DataBufferInt`, `DecimalFormatSymbols`, `Locale`, `BufferedImage`
  (from LAPSolver)
- Removed 3 dead `@SuppressWarnings("unused")` UI helper methods from ArtEvolver
- **LAPSolver.buildCostMatrix** optimization: hoisted `getRefR/G/B()` out of the per-triangle
  loop (same arrays, were fetched N times)
- **syncDeltaToTriangles** optimization: skips Color object allocation when RGB components
  haven't changed (reduces GC pressure in the hot delta evolution loop)

### Fixed — UI Responsiveness
- **Image repaint rate**: was 0.8 FPS due to bug using `GUI_UPDATE_MS` (50ms value) as frame-count
  modulus instead of `FPS/GUI_FPS` (=2). Now repaints at intended 20 FPS.
- **Swing painting**: replaced `getGraphics().drawImage()` anti-pattern with proper `paintComponent()`
  override + `repaint()`. Image no longer vanishes on window resize/minimize.
- **Console logging**: moved outside `HEALTH_ITERATIONS` block so it fires every 5 seconds
  (was nested inside a block that only ran every 25 seconds).
- **Delta sync interval**: reduced from every 50 to every 10 iterations for fresher display updates.

### Added — LAP Solver Analysis (GA_OPTIMIZATION_DESIGN.md)
- Identified that the color assignment problem is exactly the **Linear Assignment Problem**
- Jonker-Volgenant algorithm can solve it **optimally** in O(N^3): ~1 second for 1-palette,
  ~10-20 seconds for 4-palette — vs hours of GA approximation
- Documented integration architecture, memory requirements, and implementation plan

### Added — Delta Fitness Engine (Phase 1 Optimization)
- **DeltaFitnessEngine**: pre-computed triangle pixel masks with O(pixels_per_triangle) swap evaluation
  - Builds all masks in a single render pass using index-encoded colors (~200ms init)
  - Accounts for background pixels (exact score match with full render+compare: 0.00 difference)
  - `computeSwapDelta()`: evaluates a swap in ~650 pixel ops instead of 1.85M
  - `trySwap()`: atomic evaluate-and-accept for hill-climbing
  - `getTriangleError()`: per-triangle error for targeted mutation guidance
  - Achieves **3.3 million raw swaps/sec** (single thread)
- **evolveDelta()**: new evolution loop using DeltaFitnessEngine
  - Grid-localized swaps, random global swaps, and targeted swaps — all via delta evaluation
  - In-place mutation (no deep copies, no rendering per iteration)
  - Periodic sync to TriangleList + UI render (every 50 iterations)
  - **51.6x faster iteration throughput** (83 → 4,295 iter/sec, benchmarked)
- **GA_OPTIMIZATION_DESIGN.md**: comprehensive analysis and optimization roadmap
  - CPU time breakdown (rendering 40-55%, comparison 20-30%, allocation 10-15%)
  - 4-phase optimization plan with expected speedups
  - Identified 12 issues across algorithmic, implementation, and missed opportunities
- Delta fitness benchmark tests: accuracy validation + head-to-head legacy comparison

### Added
- **Smart Initialization**: greedy nearest-color assignment based on source image analysis
  - Each triangle's centroid region is sampled (7-point sampling) from the source image
  - Palette colors are assigned by minimizing RGB distance to target, prioritizing high-saturation regions
  - Initial fitness jumps from ~0.49 (random) to ~0.51+ immediately
  - Controlled by `ImageEvolver.SMART_INITIALIZATION` flag (default: true)
- **Targeted Swap mutation**: source-image-guided color swapping
  - Finds worst-matching triangles (largest color distance from source) using centroid sampling
  - Searches for swap candidates that maximize **net improvement** for both positions
  - Pre-computes centroid coordinates and source pixel values for efficiency
  - Configurable via `CrossOver.TARGETED_SWAP_ATTEMPTS` (default: 12)
- **Real two-parent crossover**: spatial block crossover preserving color permutation
  - Copies primary parent, then injects a contiguous block from secondary parent
  - Uses HashMap-based color index for O(1) permutation-safe swap resolution
  - Block size varies randomly (1/12 to 1/4 of total triangles per crossover)
  - Controlled by `CrossOver.CROSSOVER_BLOCK_ENABLED` flag (default: true)
- **Dynamic grid size**: `CrossOver.DEFAULT_GRID_SIZE` now computed from actual triangle count
  - Eliminates "grid outside population" errors when triangle count doesn't match hardcoded values
  - Grid size = totalTriangles / TOTAL_GRIDS, ensuring all grid mutations are valid
- Structured benchmarking system replacing ad-hoc System.out.println output
- BenchmarkLogger: thread-safe CSV writer with timestamped metrics, auto-file creation, and summary reports
- BenchmarkRunner: headless benchmark harness with Builder pattern for automated testing
  - Properly resizes source image to match triangle grid dimensions (fixes evaluation accuracy)
- BenchmarkTest: four automated benchmark tests (quick, baseline, thread scaling, population sizing)
- Benchmark comparison utility for side-by-side run analysis
- GUI benchmark integration: automatic CSV logging when evolution runs (toggle with BENCHMARK_LOGGING flag)
- BENCHMARKING.md: comprehensive guide for running, analyzing, and comparing benchmarks
- Version bump to 3.1.0-SNAPSHOT across all POMs
- Window title updated to "ArtEvolver v3.1"
- start.bat / start.sh: one-click launcher with Java/Maven detection, auto-build, and --rebuild flag
- benchmark.bat / benchmark.sh: interactive benchmark runner with menu-driven test selection
- exec-maven-plugin configuration for `mvn exec:java` support

### Changed
- CrossOver.getChild() now performs real two-parent crossover instead of single-parent copy
- Mutation rebalanced: targeted swaps (guided) complement grid/random swaps (blind)
- BenchmarkRunner now properly resizes source image to match triangle grid (was using raw image)
- **Mutation pipeline reordered**: random mutations (exploration) run first, then targeted swap (repair)
  - Previously targeted swap ran first, but random mutations would undo its gains
  - New order: crossover block -> grid swaps -> random swaps -> close mutations -> targeted swap (refinement)
- **Dead mutation loops eliminated**: close mutations and random grid mutations now gated behind
  meaningful probability thresholds (>0.001) instead of always looping with near-zero probability
  - Random Close: was 20 loop iterations for 0.002 expected mutations -> now skipped when CLOSE_MUTATIONS_PER_CHILD=0
  - Random Grid: was 10 loop iterations for 0.001 expected mutations -> now skipped when probability < 0.001
- **Probabilistic random swap**: instead of always doing exactly 1 swap (or looping 1000x for ~1),
  samples from proper distribution preserving variance (sometimes 0, sometimes 2+ swaps)
- **Best image buffer reuse**: renderTrianglesToNewImage() now reuses a single BufferedImage
  instead of allocating a new one every time a new best score is found (reduces GC pressure)

### Fixed
- Maven resource filtering on palette .txt files causing MalformedInputException (palette files now excluded from filtering)
- CrossOver.DEFAULT_GRID_SIZE was hardcoded for 4-palette config (76*77/8=731), causing IndexOutOfBoundsException and wasted grid mutations on other configurations

## [3.0.0] - 2026-02-18 (develop branch)
### Added
- Bulk pixel comparison in AbstractEvolver (3-5x faster fitness evaluation)
- Reusable image buffer pool in ImageEvolver (renderTriangles/renderTrianglesToNewImage)
- Reference pixel caching (cacheReferencePixels) - one-time extraction, reused every iteration
- ThreadLocal SplittableRandom in ImageEvolver (eliminates thread contention on RNG)
- ThreadLocalRandom in CrossOver (contention-free random number generation)
- Thread sleep when paused instead of busy-spin (CPU-friendly idle)
- Volatile flags for cross-thread visibility (isRunning, isStarted)
- Synchronized halveGridSize() for thread-safe mutation parameter decay
- Volatile shared mutation fields in CrossOver (GRID_MUTATION_CHANCES, RANDOM_MUTATION_CHANCES, RANDOM_CLOSE_MUTATION_CHANCES)
- pop.size() caching in hot loops
- OPTIMIZATION_MIGRATION.md documenting all optimization sources and rationale

### Removed
- Redundant drawPolygon() calls in fitness evaluation rendering (only fillPolygon needed)
- Per-pixel getRGB(x,y) calls replaced with bulk array access
- new BufferedImage() allocations in hot paths replaced with buffer reuse
- Shared java.util.Random replaced with thread-local alternatives

### Fixed
- Busy-spin in ImageEvolver.run() consuming 100% CPU when evolution paused
- Thread-safety: non-volatile boolean flags for cross-thread communication
- Thread-safety: unsynchronized static mutable field modifications in CrossOver.halveGridSize()
- Potential integer overflow in pixel diff accumulator (int -> long)
- Null pointer risk in evolve() when parentA.getScore() is null

## [2.05] - 2023-12-16 (master branch)
### Added
- PalleteColor tracking on Triangle (carries palette reference through evolution)
- Color name rendering on exported images (Renderer.java)
- PaintByColorsExporter for generating paint-by-number guides
- Proper encapsulation on PalleteColor (getId(), getColor() accessors)
- TOTAL_GRIDS changed to 24 for wider thread support
- Rectangular grid size defaults (80x53)

### Fixed
- Bug in switchGridColor() loop condition (while b==a changed to while b==a || a>=triangles.size())
- All color switch methods now also swap PalleteColor alongside Color
- All Triangle copy constructors now preserve PalleteColor reference

## [2.04] - 2022-07-30 (develop branch baseline)
### Added
- Process runner work in progress
- ArtEvolverTools for offline/test evolver factory
- Unit tests (ImageEvolverTest, CrossOverTest, ArtEvolverToolsTest)

## [2.03] - 2022-07-29
### Added
- Clean up and path fixes for test units
- Baseline for long-term optimization runs

## [2.02] - 2022-05-08 (before_fitness_development branch)
### Added
- Fitness-based parent selection (FBPS) in sequential mode
- New palette support (GBC Green, B&W, expanded Sherwin-Williams)
- Square production values for benchmarking
- Grid mutation system with configurable grid sizes
- Tournament mode with adaptive mutation decay
- Multiple resolution modes (1x through 64x palettes)
- Video frame export pipeline (PNG frames at configurable FPS)
- 4K and Full HD video export support
- Dynamic health monitoring with auto-parameter adjustment
- Multi-threaded evolution (8-64 threads)

## [2.01] - 2021-02-26 (release/v2.05 branch)
### Added
- Basic multithreading (multiple ImageEvolver instances on separate threads)
- Best score/image synchronization across threads
- Sequential evolution mode
- Multiple quality/speed presets

## [1.0.0] - 2021-01-xx (initial)
### Added
- Initial genetic algorithm implementation
- Sherwin-Williams palette loading
- Isosceles triangle grid generation
- Basic crossover and mutation operators
- Swing GUI with image display
- PNG export via Renderer
