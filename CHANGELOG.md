# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.1.0] - 2026-02-18 (develop branch)
### Added
- Structured benchmarking system replacing ad-hoc System.out.println output
- BenchmarkLogger: thread-safe CSV writer with timestamped metrics, auto-file creation, and summary reports
- BenchmarkRunner: headless benchmark harness with Builder pattern for automated testing
- BenchmarkTest: four automated benchmark tests (quick, baseline, thread scaling, population sizing)
- Benchmark comparison utility for side-by-side run analysis
- GUI benchmark integration: automatic CSV logging when evolution runs (toggle with BENCHMARK_LOGGING flag)
- BENCHMARKING.md: comprehensive guide for running, analyzing, and comparing benchmarks
- Version bump to 3.1.0-SNAPSHOT across all POMs
- Window title updated to "ArtEvolver v3.1"

### Fixed
- Maven resource filtering on palette .txt files causing MalformedInputException (palette files now excluded from filtering)

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
