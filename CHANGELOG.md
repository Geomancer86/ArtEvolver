# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.1.0] - 2026-02-18 (develop branch)
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
