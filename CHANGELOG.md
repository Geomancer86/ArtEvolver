# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.1.0] - 2026-02-18 (develop branch)
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
