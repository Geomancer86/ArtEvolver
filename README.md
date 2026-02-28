# ArtEvolver

[![Java](https://img.shields.io/badge/Java-21%2B-orange)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-blue)](https://maven.apache.org/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-green.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Twitter](https://img.shields.io/twitter/follow/ArtEvolver?style=social)](https://twitter.com/ArtEvolver)

**ArtEvolver** is a Java desktop application that creates **Color Palette Puzzles** from any image using a multithreaded genetic algorithm. It takes a source photograph and evolves a triangle mosaic approximation using *only* colors from a real-world paint palette — primarily the **Sherwin-Williams** catalog of 1,535+ named colors.

The result is a low-poly triangle art representation where every color corresponds to a real, purchasable paint. The evolution process runs in real-time, viewable in the built-in GUI or live-streamed to an audience.

![Version](https://img.shields.io/badge/Version-3.2.0-blue)
![ArtEvolver Pipeline](https://img.shields.io/badge/Status-Stable-green)

---

## Table of Contents

- [How It Works](#how-it-works)
- [Features](#features)
- [Getting Started](#getting-started)
- [Usage Guide](#usage-guide)
- [Architecture Overview](#architecture-overview)
- [Palette System](#palette-system)
- [Configuration and Modes](#configuration-and-modes)
- [Video Export](#video-export)
- [Contributing](#contributing)
- [License](#license)

---

## How It Works

ArtEvolver overlays a fixed grid of isosceles triangles on a source image. The geometry never changes — only the color assigned to each triangle evolves over time. A population of candidate "drawings" competes through selection, crossover, and mutation until the mosaic converges on a close approximation of the original image.

1. **Load** a source image (JPG or PNG) through the Swing GUI.
2. The image is resized to match the triangle grid resolution.
3. A grid of isosceles triangles is generated covering the entire image area.
4. Each triangle is assigned a random color from the loaded paint palette.
5. Multiple candidate drawings (the population) are created, each with a different random color arrangement.
6. Multiple threads (8–32+) each run an independent `ImageEvolver` instance.
7. Each evolver selects parent drawings, produces children via crossover and mutation, and evaluates fitness.
8. **Fitness** is computed as a pixel-by-pixel RGB comparison between the rendered triangles and the source image, scored from `0.0` (no match) to `1.0` (perfect match).
9. Children that outscore their parents replace them in the population.
10. The best image across all threads is displayed in the GUI in real-time.
11. Frames can be auto-exported as PNG files for timelapse video generation via FFMPEG.

---

## Features

### Real Paint Colors
Unlike projects that evolve arbitrary RGB values, ArtEvolver constrains its palette to real manufacturer paint colors. Every triangle in the output maps to a named paint you could buy and apply. The primary palette is the **Sherwin-Williams** catalog (1,535 colors), but the system supports any palette defined in a simple text format.

### Fixed Triangle Geometry
The triangle grid is deterministic and fixed. Evolution operates exclusively on color assignment, making the problem analogous to a "paint by numbers" puzzle — given a fixed set of regions and a fixed set of paints, find the best color for each region.

### Multi-Palette Support
- **Sherwin-Williams** — 1,535 named paint colors
- **GBC Green** — 4 shades (Game Boy Color aesthetic)
- **Black & White** — 2 colors
- **Trilux** — 12 pen colors

### Scalable Resolution
Triangle scale parameters control output resolution, from tiny thumbnails to 4K renders. Larger grids produce finer detail at the cost of longer convergence times.

### Multithreaded Evolution
8 to 32+ concurrent evolver threads run in parallel, each maintaining its own sub-population. The best result across all threads is tracked globally and displayed in the GUI.

### Video Export Pipeline
Frames are exported as numbered PNG files at configurable intervals. These frames can be assembled into timelapse videos using FFMPEG, showing the image evolving from random noise to a recognizable mosaic.

### Streaming-Ready
The GUI refresh loop and frame export pipeline are designed with live-streaming in mind (Twitch, YouTube). Watch the evolution happen in real-time.

### Delta Fitness Engine (v3.1)
Pre-computed triangle pixel masks enable O(pixels_per_triangle) swap evaluation instead of full-image rendering + comparison. Achieves **50x faster iteration throughput** (3.3 million raw swaps/sec) with exact score accuracy.

### Smart Initialization (v3.1)
Greedy nearest-color assignment analyzes the source image at each triangle's region and assigns the best-matching unused palette color, producing a strong initial fitness before evolution begins.

### Adaptive Mutation Decay
Tournament-based selection combined with adaptive mutation decay gradually shifts the search from broad exploration to fine-tuning as fitness improves.

### Evolutionary Tournament System (v3.1)
Meta-genetic algorithm that evolves GA parameters themselves. Velocity-aware composite ranking,
multi-generational breeding with ancestry tracking, adaptive cutoff, multi-stage (geared) competitors,
prehistoric mode with 8 progressive eras, promoted hall of fame, 21 preset strategies, autopilot mode,
and real-time browser dashboard with REST API.

### Persistent Settings (v3.2)
All user preferences (sidebar parameters, tournament settings, window positions, last loaded image)
are saved automatically on exit and restored on next launch via `java.util.prefs.Preferences`.

### Help System (v3.2)
Help menu with Quick Start Guide and Parameter Reference. Context-sensitive tooltips on every
control with suggested starting values. Dashboard help overlay for metric explanations.

### Thumbnail Cache & Disk Cache (v3.2)
Persistent disk cache stores resized source images in `~/.artevolver/cache/` so high-resolution
camera images (Canon EOS R5 45MP, R1 50MP, etc.) load near-instantly on subsequent opens.
In-memory cache for dashboard thumbnails with HTTP ETag support reduces CPU during auto-refresh.

### Evolution Clicker (v3.2)
Browser-based idle/clicker game — evolve a triangle mosaic one swap at a time. Drop or pick an
image, click to attempt random color swaps. Each click may improve fitness or miss; upgrades
make clicks smarter. Earn EP, buy 21 upgrades (click power, automation, intelligence, prestige),
unlock the Sample Atlas with real landscapes and iconic art, ascend for Golden Frame (GF), and
complete masterpieces at 85%+ fitness. Procedural sound engine, 75+ achievements, gallery of
completed images. Run `clicker.bat` / `clicker.sh` for one-click launch — no Java UI needed.

### Retro Console Mode (v3.3, planned)
GB/GBC/GBA, Sega Genesis, and SNES palettes and resolutions — square pixels, limited colors.
Pixel mode (one color per pixel) for authentic retro look. Design validated against principles
of Yokoi, Miyamoto, Kojima, Carmack, and other top game designers. See `RETRO_CONSOLE_DESIGN.md`,
`RETRO_PRESETS.md`.

---

## Getting Started

### Prerequisites

| Dependency | Version | Download |
|------------|---------|----------|
| Java JDK   | 21+     | [Adoptium Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) |
| Apache Maven | 3.6+  | [Maven Downloads](https://maven.apache.org/download.cgi) |

#### Installing Java 21

**Windows** (via winget):
```bash
winget install EclipseAdoptium.Temurin.21.JDK
```

**macOS** (via Homebrew):
```bash
brew install --cask temurin@21
```

**Linux (Debian/Ubuntu)**:
```bash
sudo apt install temurin-21-jdk
```

Or download the installer for your platform directly from
[Adoptium](https://adoptium.net/temurin/releases/?version=21) — pick the `.msi` (Windows),
`.pkg` (macOS), or `.tar.gz` (Linux) for JDK 21 LTS.

#### Installing Maven

**Windows** (via winget):
```bash
winget install Apache.Maven
```

**macOS** (via Homebrew):
```bash
brew install maven
```

**Linux (Debian/Ubuntu)**:
```bash
sudo apt install maven
```

Or download from [maven.apache.org](https://maven.apache.org/download.cgi) and add `bin/` to your PATH.

#### Verify Installation

```bash
java -version    # Should show 21.x.x or higher
mvn -version     # Should show 3.6.x or higher
```

### Clone

```bash
git clone https://github.com/Geomancer86/ArtEvolver.git
cd ArtEvolver
```

### Quick Start — Evolution Clicker (Browser)

The fastest way to play. No Java UI knowledge needed — pick an image in your browser and start evolving.

**Windows:**
```bash
clicker.bat
```

**Linux/Mac:**
```bash
chmod +x clicker.sh
./clicker.sh
```

This builds the project, starts the server, and opens the clicker game in your browser.
Drop or pick an image, click to evolve.

### Quick Start — Full Application (Swing GUI)

For the complete ArtEvolver experience with tournaments, benchmarks, and all advanced features.

**Windows:**
```bash
start.bat
```

**Linux/Mac:**
```bash
chmod +x start.sh
./start.sh
```

The script checks for Java and Maven, builds the project if needed, and launches the GUI.

Use `--rebuild` to force a clean build: `start.bat --rebuild`

### Manual Build

```bash
mvn compile
```

> **Note:** If you encounter `MalformedInputException` during a full build, palette `.txt` files have non-UTF-8 characters. This is handled automatically in v3.1+, but for older checkouts use:
>
> ```bash
> mvn compiler:compile -pl artevolver-core
> ```

### Manual Run

Execute the main class to launch the GUI:

```bash
mvn exec:java -pl artevolver-core -Dexec.mainClass="com.rndmodgames.evolver.ArtEvolver"
```

Or run `com.rndmodgames.evolver.ArtEvolver` directly from your IDE.

### Testing

All tests run in **headless mode** (no Swing windows open). The test suite takes ~3 minutes.

```bash
# Run all tests (27 tests across 5 test classes)
mvn test -pl artevolver-core

# Run a specific test class
mvn test -pl artevolver-core -Dtest=BenchmarkTest

# Run a specific test method
mvn test -pl artevolver-core -Dtest=BenchmarkTest#lapSolverUnitTest
```

**Test classes:**

| Class | Tests | Description |
|-------|-------|-------------|
| `ArtEvolverTest` | 7 | Palette loading, shuffling, color operations |
| `ArtEvolverToolsTest` | 5 | ImageEvolver creation and evolution |
| `BenchmarkTest` | 9 | Delta fitness, LAP solver, permutation integrity, benchmarks |
| `CrossOverTest` | 4 | CrossOver parameter management and evolution |
| `ImageEvolverTest` | 2 | ImageEvolver instantiation and iteration counting |

---

## Usage Guide

1. **Launch** the application. The Swing GUI window will appear.
2. Click **Load** and select a source image (JPG or PNG).
3. Click **Start** to begin the evolution process.
4. Watch the triangle mosaic refine in real-time as the genetic algorithm iterates.
5. The fitness score is tracked and displayed — higher values mean a closer match to the source image.
6. If frame export is enabled, PNG files are written to disk automatically for later video assembly.

---

## Architecture Overview

ArtEvolver is organized as a Maven multi-module project:

```
artevolver/                              # v3.2.0
├── pom.xml                              # Parent POM
├── artevolver-core/                     # Core evolution engine + GUI
│   └── src/main/java/com/rndmodgames/evolver/
│       ├── ArtEvolver.java              # Swing GUI + threading orchestration
│       ├── ArtEvolverTools.java         # Offline/test evolver factory
│       ├── AbstractEvolver.java         # Fitness comparison (pixel-level RGB diff)
│       ├── ImageEvolver.java            # Main evolution loop, rendering, mutations
│       ├── CrossOver.java               # Crossover + mutation operators
│       ├── Triangle.java                # Triangle polygon with assigned color
│       ├── TriangleList.java            # Scored population member (drawing)
│       ├── Palette.java                 # Palette loader (parses text files)
│       ├── PalleteColor.java            # Named color with RGB values
│       ├── DeltaFitnessEngine.java      # O(pixels_per_tri) swap evaluation
│       ├── LAPSolver.java               # Jonker-Volgenant optimal assignment
│       ├── EvolutionConfig.java         # Parameter bundle with gene array
│       ├── EvolutionaryTournament.java  # Meta-GA: ranking, breeding, lifecycle
│       ├── TournamentContestant.java    # Single evolution run encapsulation
│       ├── TournamentManagerWindow.java # Tournament UI, autopilot, settings
│       ├── FitnessChartWindow.java      # Real-time multi-series fitness chart
│       ├── FitnessTracker.java          # Velocity/acceleration tracking
│       ├── DashboardServer.java         # HTTP server + REST API + dashboard
│       ├── PrehistoricMode.java         # Genesis mode (8 progressive eras)
│       ├── LineageNode.java             # Ancestry tree for breeding
│       ├── SystemMonitor.java           # JMX-based CPU/RAM/Disk monitoring
│       ├── SettingsManager.java         # Persistent user preferences (java.util.prefs)
│       ├── ImageDiskCache.java          # Persistent disk cache for resized source images
│       ├── ClickerState.java            # Evolution Clicker game logic
│       ├── benchmark/
│       │   ├── BenchmarkLogger.java     # Thread-safe CSV benchmark writer
│       │   └── BenchmarkRunner.java     # Headless benchmark harness
│       └── render/
│           └── Renderer.java            # PNG export with scaling
│   └── src/main/resources/
│       ├── dashboard.html               # Browser dashboard (dark theme)
│       ├── palette4.txt                 # Sherwin-Williams (1,535 colors)
│       ├── sherwin.txt                  # Sherwin-Williams (original format)
│       ├── palette2.txt                 # Game Boy Color green (4 shades)
│       ├── palette3.txt                 # Black & White (2 colors)
│       └── trilux.txt                   # Trilux pens (12 colors)
├── artevolver-desktop/                  # Desktop launcher module
└── README.md
```

### Key Components

**`ArtEvolver`** — Entry point. Builds the Swing UI, spawns evolver threads, and coordinates the global best-image display loop.

**`ImageEvolver`** — The core evolution loop running on each thread. Handles parent selection, child generation (via `CrossOver`), fitness evaluation (via `AbstractEvolver`), and population replacement. Also responsible for rendering the triangle mosaic to a `BufferedImage` using Java2D.

**`AbstractEvolver`** — Computes fitness by comparing each pixel of the rendered mosaic against the corresponding pixel of the source image. The RGB difference across all pixels is normalized to a `0.0`–`1.0` score.

**`DeltaFitnessEngine`** — Pre-computes which pixels belong to each triangle at initialization, then evaluates any color swap by computing only the affected pixels (~650 instead of 1.85M). Eliminates rendering and full-image comparison from the hot loop, achieving 50x throughput improvement.

**`CrossOver`** — Implements crossover (block exchange of triangle colors between two parents) and mutation (random color reassignment from the palette). Supports multiple mutation strategies: Grid, Random, Close (neighbor swap), and Random Grid.

**`Palette` / `PalleteColor`** — Loads palette definitions from text resource files into in-memory color lists used during initialization and mutation.

**`Renderer`** — Exports the current best mosaic as a scaled PNG file for frame capture and video generation.

**`EvolutionaryTournament`** — Meta-genetic algorithm that evolves GA parameters. Ranks contestants by a weighted blend of fitness, velocity, acceleration, and lineage. Culls the worst, breeds replacements via BLX-alpha crossover with Gaussian mutation, tracks best-ever configuration, and detects convergence.

**`TournamentManagerWindow`** — Full-featured Swing UI for managing the tournament: contestant table, detail panel, history log, autopilot controls, prehistoric mode, and evo settings dialog.

**`DashboardServer`** — Embedded HTTP server using `com.sun.net.httpserver.HttpServer`. Serves a browser-based real-time leaderboard with REST API endpoints for tournament state, image thumbnails, and full-resolution exports. Zero external dependencies.

**`FitnessChartWindow`** — Separate resizable JFrame with real-time multi-series fitness chart. Dark theme, gradient area fill, auto-scaling axes, peak indicator, color-coded legend, and per-series stats.

---

## Palette System

Palettes are defined in plain text files with one color per line:

```
ID ColorName R G B
```

**Example (`palette4.txt` — Sherwin-Williams):**

```
1 Darkest Green 15 16 15
2 Tricorn Black 17 14 16
3 Caviar 18 17 18
...
1535 High Reflective White 253 254 252
```

Fields are space-separated. The color name can contain spaces — the parser treats the last three tokens as R, G, B values and everything between the ID and the RGB values as the color name.

### Available Palettes

| File           | Palette            | Colors | Use Case                              |
|----------------|--------------------|--------|---------------------------------------|
| `palette4.txt` | Sherwin-Williams   | 1,535  | Full-color realistic paint matching   |
| `sherwin.txt`  | Sherwin-Williams   | varies | Original format variant               |
| `palette2.txt` | GBC Green          | 4      | Retro Game Boy Color aesthetic         |
| `palette3.txt` | Black & White      | 2      | High-contrast monochrome              |
| `trilux.txt`   | Trilux Pens        | 12     | Pen-art style with limited colors     |

### Custom Palettes

To add your own palette, create a text file following the `ID ColorName R G B` format and place it in `artevolver-core/src/main/resources/`. Update the palette loading path in the source code to reference your new file.

---

## Configuration and Modes

### Running Modes

ArtEvolver provides several preset configurations that balance speed and quality:

| Mode                    | Description                                              |
|-------------------------|----------------------------------------------------------|
| `QUICK_MODE`            | Fast convergence, lower fidelity                         |
| `QUICK_EXTENDED_MODE`   | Extended quick run with more iterations                  |
| `FASTEST_MODE`          | Maximum speed, minimal quality checks                    |
| `QUALITY_MODE`          | High fidelity, slower convergence                        |
| `QUALITY_MODE_STREAM`   | Quality mode tuned for live-streaming frame rates        |

### Genetic Algorithm Parameters

| Parameter            | Typical Range | Description                                        |
|----------------------|---------------|----------------------------------------------------|
| Population size      | 2–8 per thread | Number of candidate drawings per evolver thread   |
| Thread count         | 8–32+         | Number of concurrent `ImageEvolver` instances      |
| `EVOLVE_ITERATIONS`  | 2             | Mutation/evaluation cycles per batch               |
| Mutation types       | Grid, Random, Close, Random Grid | Strategies for color reassignment |
| Crossover            | Block exchange | Swaps rectangular regions of triangle colors       |
| Selection            | Best-parent / Fitness-proportional | Parent selection strategy    |
| Mutation decay       | Adaptive      | Mutation probability decreases as fitness improves |

Parameters can be adjusted in the source code to experiment with different evolutionary strategies.

---

## Video Export

ArtEvolver can export each frame of the evolution as a numbered PNG file. To assemble these frames into a timelapse video:

```bash
ffmpeg -framerate 30 -i frame_%06d.png -c:v libx264 -pix_fmt yuv420p output.mp4
```

Adjust `-framerate` to control playback speed. Higher frame rates compress more generations into less video time.

---

## Contributing

Contributions are welcome. To get started:

1. Fork the repository.
2. Create a feature branch from `develop`.
3. Make your changes with clear commit messages.
4. Ensure the project builds successfully with `mvn compile`.
5. Open a pull request against `develop`.

Areas where contributions are especially appreciated:

- New palette files from other paint manufacturers
- Performance optimizations in the fitness evaluation loop
- Additional mutation and crossover strategies
- UI improvements and visualization features
- Documentation and examples

---

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

---

## Links

- **GitHub:** [https://github.com/Geomancer86/ArtEvolver](https://github.com/Geomancer86/ArtEvolver)
- **Twitter/X:** [@ArtEvolver](https://twitter.com/ArtEvolver)
