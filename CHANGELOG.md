# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.1.0] - 2026-02-20 (develop branch)

### Fixed — Evolution History Panel & Tournament Speed

- **History panel only showed Generation 1**: The update check compared `txtHistory.getLineCount()` 
  against `records.size()`, but since `toNarrative()` produces ~10 lines per record, after Gen 1
  the line count always exceeded the record count. Fixed by tracking record count directly
- **Smart announcer live status block**: The history panel now shows a live-updating status block 
  at the top with a mini-leaderboard, per-contestant velocity/promise indicators, spread analysis,
  and countdown to next cycle. Updates between generations without replacing history
- **Promise indicators**: Each contestant shows velocity arrows (up/down/flat), promise tags 
  (FAST, rising, fading, at risk), grace period markers, and uptime
- **Enhanced generation narratives**: `toNarrative()` now shows score deltas between generations,
  stall warnings, contestant birth generation, and cycle speed annotations (machine gun/fast/extended)
- **Prehistoric mode history counter**: Fixed era history using fragile text-search check, now uses
  proper record counter
- **FitnessTracker thread safety**: All snapshot access is now synchronized to prevent 
  `ConcurrentModificationException` when the live status block reads while evolvers write.
  `velocityWindowMs` is now `volatile`

### Enhanced — Tournament Speed (Machine Gun Start)

- **Default cutoff reduced from 60s to 10s**: Tournament cycles start fast for rapid churn
- **Adaptive cutoff min reduced from 15s to 5s**: Allows very rapid cycling when stalled
- **Autopilot machine-gun start**: When autopilot creates the evolutionary tournament, it starts
  at the adaptive minimum (5s), ramping up as contestants differentiate
- **Smarter adaptive algorithm**: Starts fast, ramps UP as contestants improve (giving them more
  time), drops FAST when stalled (2/3 of current interval when stalled 5+ gens). Early generations
  ramp up in larger increments (+10s when under 30s, +5s after)
- **Grace period default increased to 2 ticks**: Gives newcomers more time with the faster cycles
- **Settings UI allows 5s minimum**: Cutoff interval spinner now starts at 5s with 5s step

### Fixed — Autopilot CPU Saturation & Contestant Initialization
- **Contestants no longer stuck at "initializing"**: `TournamentContestant.initializeWithImage()`
  now renders an initial best image immediately after triangle initialization, so the UI
  always shows the starting state instead of a blank placeholder
- **CPU saturation fixed**: Autopilot was spawning contestants every refresh tick (~1s) without
  any cooldown. Now enforces a 10-second cooldown between spawns, tracks actual thread counts
  against a hard budget (75% of cores * CPU limit), and checks both process AND system CPU
- **Gradual resource ramp-up**: Instead of filling all available CPU immediately, the autopilot
  starts conservatively (60% thread budget initially) and only adds new contestants when both
  the cooldown has passed AND resources are genuinely available
- **Prehistoric mode era advance gating**: Auto-advance now checks thread utilization before
  advancing to the next era; defers if >=85% of thread budget is in use
- **Era 6 spawning capped**: Uses actual thread count tracking instead of naive estimate,
  limiting multi-contestant spawns to what the budget actually allows
- **PrehistoricMode.addPresetContestant()**: Now checks thread budget before adding, preventing
  over-commitment even when called by autopilot
- **SystemMonitor.canAddWork()**: Now checks system-wide CPU (not just process) and applies a
  10% safety margin below the limit to account for JMX measurement lag
- **ImageEvolver.renderTrianglesToNewImage()**: Changed from private to package-private for
  initial image rendering in TournamentContestant

### Added — Evolution Clicker (Cookie Clicker Mode)
- **Full idle/clicker game** integrated into the genetic algorithm, inspired by
  Cookie Clicker, designed channeling Carmack, Will Wright, Sid Meier, Miyamoto,
  Kojima, Gabe Newell, Sandy Petersen, Tim Cain, and Gygax
- **3 Currencies**: Evolution Points (EP, main), Mutation Crystals (MC, rare),
  Genome Fragments (GF, prestige). EP earned from evolution, clicks, and upgrades
- **52 Upgrades** across 8 categories, each with 20-200 levels:
  - Mutation Lab (8): Random Swap Power, Grid Awareness, Precision Touch,
    Targeted Strikes, Mutation Efficiency, Double Down, Chain Reaction, Mastery
  - Population Lab (6): Growth, Elite Retention, Diversity, Fresh Blood,
    Crossover Partners, Mastery
  - Crossover Lab (5): Block Crossover, Frequency, Multi-Parent, Adaptive, Mastery
  - Initialization (4): Smart Init, LAP Solver, Hybrid Init, Mastery
  - Computing (6): Processing Power, Batch Size, Memory Cache, CPU Affinity,
    Parallel Eval, Mastery
  - Tournament (6): Contestant Slots, Quick Culling, Multi-Spawn, Adaptive
    Intelligence, Grace Extension, Mastery
  - Meta-Evolution (6): Gene Crossover, Gene Mutation, Composite Weights,
    Deep Ancestry, Convergence Detection, Mastery
  - Special (8, MC): Golden Mutations, Critical Hits, Time Warp, Fitness Magnet,
    Auto-Clicker, EP Overflow, Lucky Star, Legacy Power
  - Prestige (4, GF): Eternal Speed, Eternal Fitness, Eternal Production,
    Eternal Fortune
- **120+ Achievements** procedurally generated across 10 categories:
  fitness, EP earned, clicks, play time, generations, upgrades, prestige,
  speed, contestants, and 10+ hidden discovery achievements
- **Prestige/Ascension System**: Reset progress for Genome Fragments (sqrt
  scaling), permanent multipliers, unlock prestige-tier upgrades
- **8 Random Events**: Golden Mutation, Fitness Surge, EP Rain, Population Boom,
  Algorithm Insight, Cosmic Alignment, Time Dilation, Mutation Frenzy — each
  with duration, strength multiplier, and luck-scaled spawn chance
- **Browser-based UI** (`/clicker` endpoint):
  - Dark theme with neon accents, 3-column layout
  - Giant clickable evolution orb with floating EP popups and critical hit effects
  - Tabbed upgrade store with cost scaling, max-buy (right-click), affordability
  - Achievement grid with tooltips, celebration popups on unlock
  - Live event pills with countdown timers and glow animations
  - Stats dashboard: clicks, EP/click, upgrades, achievements, play time
  - Prestige button with earned fragment preview
  - Activity log with color-coded notifications
  - Links between Dashboard and Clicker pages
- **Every upgrade maps to real evolution parameters** — this isn't a fake game,
  purchases actually change mutation rates, thread counts, population sizes, etc.

### Added — Browser Dashboard (Real-Time Leaderboard)
- **Embedded HTTP server** (`DashboardServer.java`) using Java's built-in
  `com.sun.net.httpserver.HttpServer` — zero external dependencies, auto-selects
  a free port, opens the default browser automatically
- **REST API endpoints**:
  - `GET /` — serves the dashboard HTML page from classpath resources
  - `GET /api/state` — full tournament state as JSON (system metrics, contestants,
    tournament params, evolution history, prehistoric mode status)
  - `GET /api/image/{id}` — PNG thumbnail (300px) of contestant's current best image
  - `GET /api/export/{id}` — full-resolution PNG download with proper filename
- **Modern dark-themed dashboard** (`dashboard.html`) featuring:
  - Live system metrics bar (CPU, Heap, RAM, Threads, Disk) with color-coded gauges
  - Tournament status (generation, countdown, best-ever, adaptive cutoff state)
  - Status badges: LIVE, AUTOPILOT, ERA (Prehistoric Mode)
  - Ranked leaderboard cards with image thumbnails, color dots matching chart series,
    fitness/velocity/acceleration/peak stats, config summaries, uptime, iterations
  - Eliminated contestants shown faded with skull icons and export buttons
  - One-click high-quality PNG export for any contestant (especially eliminated ones)
  - Evolution history log with syntax-highlighted narratives (newest first)
  - Summary panel: active/eliminated/total counts, average fitness
  - Auto-refreshes every 2 seconds
  - Fully responsive layout
- **Dashboard button** on Tournament Manager status bar — launches the browser dashboard

### Added — System Monitor, Autopilot & Smart Test Profiles

- **SystemMonitor.java**: Real-time CPU, RAM (heap + physical), disk, and thread
  monitoring using JMX and `java.lang.management`. All metrics exposed as percentages
  and formatted strings for both programmatic and UI use.
- **Autopilot Mode**: One-click fully automated tournament mode accessible from
  the new `Autopilot...` button on the system status bar:
  - Configurable resource limits: max CPU %, max RAM %, max JVM Heap %
  - Automatically starts Genesis/Prehistoric Mode if no contestants exist
  - Spawns contestants when resource headroom allows
  - Activates evolutionary tournament when 3+ contestants are running
  - Pauses spawning when CPU/RAM limits are approached
- **System Status Bar**: Live CPU / Heap / RAM / Thread display at the bottom
  of the Tournament Manager, color-coded by load (green/yellow/red)
- **Smart Test Profiles**: Slow tests (BenchmarkTest, ArtEvolverToolsTest,
  CrossOverTest) tagged with `@Tag("slow")` and excluded by default:
  - `mvn test` — runs only fast tests (~15s vs ~210s)
  - `mvn test -Pfull` — runs all tests including slow benchmarks
  - No tests removed, only deferred for development speed
- **Narrative Evolution Logs**: Tournament history panel now shows rich,
  human-readable multi-line narratives for each generation:
  - Timestamps, population count, leader and velocity
  - Convergence warnings, culling details with reasons
  - Breeding info with parent lineage and grace periods
  - Adaptive cutoff status (accelerated/extended)
  - Prehistoric mode eras show capability unlocks and thematic descriptions

### Added — Prehistoric Mode (Progressive Evolution Tournament)
- **New gamified tournament mode** that starts from the most primitive possible
  configuration and progressively unlocks capabilities through geological "eras"
- **8 Eras** from Primordial Soup (1 thread, random init, legacy evolve, random
  swaps only) to Age of Intelligence (full evolutionary tournament with meta-GA)
- **Era 0 — Primordial Soup**: Pure brute-force random walk (~90 iter/sec)
- **Era 1 — Single Cell**: Adds grid swaps and close mutations
- **Era 2 — Multicellular**: Smart init, population=2, crossover enabled
- **Era 3 — Cambrian Explosion**: Delta evolution + targeted swaps (~4500 iter/sec)
- **Era 4 — Age of Fish**: 2 threads, full mutation suite
- **Era 5 — Age of Reptiles**: Scaling threads, preset strategies introduced
- **Era 6 — Age of Mammals**: Full thread allocation, all 8 strategies deployed
- **Era 7 — Age of Intelligence**: Evolutionary tournament activates automatically
- **Manual or Auto-Advance**: Toggle between manual era progression (click Advance)
  and automatic progression (configurable duration per era, default 60s)
- **+1 Thread button**: Manually add a thread to the thread budget at any time
- **Add Preset Contestant**: Draws from 8 preset strategies, constrained to current
  era capabilities; shows remaining count
- **Add Evolved Contestant**: Breeds from top 2 performers via BLX-alpha crossover
  with Gaussian mutation; available once presets are exhausted or 2+ contestants exist
- **Capability badges**: Shows which features are unlocked at the current era
- **Era progress bar and countdown**: Visual feedback for auto-advance timing
- **New `EvolutionConfig` factories**: `createPrimordial()`, `createEra1()` through
  `createEra7()` for era-specific stripped-down configurations
- **New `PrehistoricMode.java` controller**: Manages era progression, contestant
  spawning, auto-advance timer, capability constraints, and preset pool tracking

### Improved — Dynamic Tournament, Performance, Draw All Sorting
- **Draw All sorted** — the "Draw All" view now renders contestants sorted by best
  score (highest first), with eliminated contestants at the bottom. No more hunting
  for the best — it's always top-left
- **Tournament table** already sorts by status then score (eliminated at bottom)
- **Multi-spawn per tick** — new `spawnsPerTick` parameter (default 1, up to 10)
  allows culling and breeding N contestants per generation tick. Each spawn gets its
  own unique name suffix (e.g., G5a, G5b). Re-ranks alive pool between each cull
- **Adaptive cutoff** — when enabled (default ON), the cutoff interval auto-adjusts:
  shortens by 10s when stalled (>=3 gens), lengthens by 5s when improving. Bounded
  by configurable min (15s) and max (300s). Lets the system find its own optimal pace
- **Performance: `syncDeltaToTriangles` moved inside improvement guard** — previously
  called unconditionally every 10 iterations (~1482 `new Color()` objects each time),
  now only called when a score actually improves. Eliminates massive GC pressure
- **Performance: Color object cache** — `ConcurrentHashMap<Integer, Color>` caches
  Color instances by RGB key, preventing duplicate allocations across all threads
- **Performance: `expectedSwaps` hoisted** — constant calculation moved outside inner
  loop to avoid redundant float multiplication per iteration
- **Performance: FitnessTracker pruning** — snapshots are automatically downsampled
  when exceeding 2000 entries, keeping recent data at full resolution and older data
  at 1/4 resolution to prevent unbounded memory growth in long tournaments
- **JVM tuning for Threadripper 2950x / 128GB** — `start.bat` now launches with:
  `-Xmx16g -Xms4g -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch
  -XX:-TieredCompilation`. ZGC provides sub-millisecond GC pauses at scale,
  generational mode improves throughput, pre-touch avoids runtime page faults,
  and skipping tiered compilation goes straight to C2 for faster steady-state
- **Evo Settings expanded** — new sections for "Adaptive Cutoff" (toggle, min, max)
  and "Spawns per Tick" control. All parameters configurable at runtime

### Added — Evolutionary Tournament v2 (Velocity-Aware Meta-GA)
Major redesign solving the "newcomer problem" where bred contestants were immediately culled
before proving their parameter quality, since they always started at fitness 0.

- **`FitnessTracker`** (new class) — per-contestant time-series fitness tracking
  - Records timestamped fitness snapshots from the ArtEvolver process timer
  - Computes velocity (fitness gain/sec) over a configurable sliding window (default 30s)
  - Computes acceleration (rate of velocity change) via half-window comparison
  - Provides projected fitness, peak fitness, peak velocity, and iterations/sec
- **`LineageNode`** (new class) — full ancestry tree with multi-generational memory
  - Each contestant gets a node linked to parent nodes, forming a pedigree tree
  - Lineage fitness: decay-weighted average of peak fitness across ancestors (configurable
    decay=0.7, depth=3), inspired by BLUP pedigree analysis in animal breeding
  - Lineage velocity: same formula applied to peak velocity values
  - Ancestral gene retrieval with decay weights for multi-generational crossover
  - Inbreeding detection: prevents crossing contestants sharing a recent common ancestor
  - Ancestry string formatter for display in the detail panel
- **Composite ranking** — replaces pure absolute-fitness ranking with weighted blend:
  - Fitness weight (default 0.35) — current absolute score still matters
  - Velocity weight (default 0.40) — improvement speed is the primary signal
  - Acceleration weight (default 0.05) — rewarding contestants that are speeding up
  - Lineage weight (default 0.20) — rewarding configurations from successful family lines
  - Min-max normalization across alive contestants for fair cross-metric comparison
- **Grace period** — new contestants receive configurable immunity ticks (default 1)
  during which they cannot be selected for culling, giving them time to build up
  velocity data before being compared to established contestants
- **Multi-generational crossover** — when breeding, gene arrays from grandparents and
  great-grandparents are blended with exponentially decaying weights, then BLX-alpha
  exploration is applied. Falls back to standard two-parent crossover when ancestry is empty
- **Configurable parameters** — all new parameters exposed in Evo Settings dialog:
  grace period ticks, velocity window, all 4 ranking weights, lineage decay, ancestry
  depth, and ancestral crossover toggle. All are future meta-optimizable
- **Enhanced table** — new "Velocity" column showing real-time fitness gain rate per
  contestant; protected contestants shown with star icon; composite scores in history log
- **Enhanced detail panel** — shows velocity, acceleration, peak fitness, peak velocity,
  lineage fitness, and a formatted ancestry tree for each contestant
- **Generation records** — enriched with composite score, velocity, grace ticks,
  ancestral crossover flag for complete audit trail

### Improved — Auto-Evolve on Start & Window Sizing
- **Auto-Evolve toggle** — new "Auto-Evolve on Start" checkbox (default ON) in the Tournament
  Manager's evolutionary bar. When checked, pressing "Start All" automatically begins the
  evolutionary culling cycle alongside the contestants, so users no longer need to separately
  discover and click "Start Evolving"
- **Manual control preserved** — the "Start Evolving" / "Stop Evolving" button remains available
  for users who want to start/stop the evolutionary cycle independently
- **Window auto-sizing** — `TournamentManagerWindow` and `FitnessChartWindow` now use `pack()`
  followed by screen-aware sizing (respecting usable screen area and taskbar insets) instead of
  hardcoded sizes. Windows open at content-fitting dimensions without requiring manual resizing
- **Increased preferred sizes** — table scroll, detail panel, and history panel use larger preferred
  dimensions so `pack()` allocates more space and scrollbars are minimized on first open

### Added — Evolutionary Tournament Mode (Meta-GA)
- **`EvolutionaryTournament`** — meta-genetic algorithm that evolves GA parameters themselves
  - At configurable cutoff intervals (default 60s), ranks all contestants by fitness score
  - Culls the worst performer (disposes threads, clears evolvers, removes chart series)
  - Breeds a replacement via BLX-alpha crossover + Gaussian mutation on 9 numeric genes
  - Tournament selection picks parents from the top half of performers
  - Spawns the new contestant with bred config, fresh evolvers, and starts it immediately
  - Configurable: cutoff interval, mutation rate (0-1), mutation strength (0-1), min contestants
- **Live countdown timer** — real-time display of seconds until next cull tick, color-coded
  (blue > 30s, orange ≤ 30s, red ≤ 10s)
- **Best-ever tracking** — records the highest fitness ever achieved and its configuration,
  surviving across generations even if that contestant is culled
- **Convergence detection** — automatically detects when the meta-GA stalls for 5 consecutive
  generations without meaningful improvement (< 0.01% gain), displays [CONVERGED] indicator
- **Rich generation history** — each record tracks: full ranking, culled contestant (with gen),
  parents, child, best/worst/average scores, timestamp, and convergence state
- **Edge case handling** — skips culling when no contestants have scores yet; breaks tied scores
  by preferring to cull the older contestant; limits parent selection retry attempts
- **Gene array system in `EvolutionConfig`** — 9 breedable parameters mapped to float arrays
  - `toGeneArray()` / `fromGeneArray()` for genetic operations
  - `getGeneMin()` / `getGeneMax()` define parameter ranges
  - Covers: gridMutationChances, gridMutationDecay, randomMutationChances, randomMutationPercent,
    closeMutationChances, closeMutationPercent, targetedSwapAttempts, population, crossoverMax
- **Evolutionary UI controls in Tournament Manager**
  - "Start Evolving" / "Stop Evolving" toggle button with state-dependent styling
  - "Evo Settings" dialog for all meta-GA parameters (can update cutoff while running)
  - Generation counter label + live countdown + best-ever label updated in real-time
  - History log panel showing all cull/breed events with full rankings and averages
  - Table includes "Gen" column; detail panel shows parentage, runtime, and iteration speed
  - Quick Setup button disabled during active evolution to prevent corruption
- **`TournamentContestant.dispose()`** — full cleanup: stop + interrupt threads + clear evolvers
- **`TournamentContestant` generation/parentage tracking** for evolutionary lineage

### Fixed — Evolutionary Tournament Not Firing
- **`EvolutionaryTournament.start()` now returns `boolean`** and logs errors when preconditions
  fail, instead of silently returning. `toggleEvolving()` now shows an error dialog on failure.
- **Ranking now filters to alive contestants only** — `getAlive()` helper excludes eliminated
  contestants from ranking, parent selection, and minimum population checks.

### Added — Eliminated Contestants Remain Visible
- **Eliminated contestants stay in the list** — instead of being removed, culled contestants
  are marked `eliminated=true` with their final score and elimination generation preserved.
- **Draw All** shows eliminated contestants with 35% opacity, red border, and a skull
  "ELIMINATED" overlay while preserving their last best image.
- **Fitness chart** keeps eliminated contestants' historical data visible (series not cleared);
  only stops adding new data points.
- **Tournament Manager table** shows eliminated contestants with skull icon, grey color swatch,
  "Eliminated (Gen N)" status, and sorts them to the bottom by default.
- **Detail panel** shows elimination info (status, final score) for eliminated contestants.
- **Contestant combo** shows eliminated entries with skull icon and "ELIMINATED" suffix.
- **Table is now sortable** — click column headers to sort by name, score, status, gen, or
  parameters. Default sort: Status ascending + Score descending (eliminated at bottom).

### Added — Tournament Display Modes
- **Display Mode** combo box in the Tournament section of the sidebar with three options:
  - **Draw Selected** — shows the contestant currently picked in the combo box (default)
  - **Draw Best** — always shows the highest-scoring contestant in real-time
  - **Draw All** — grid of all contestants rendered simultaneously in the main panel, each with
    name, chart color, score overlay, gold border on the best, and colored border on selected
- "Draw All" mode automatically lowers the rendering frame rate to reduce CPU usage
- Grid layout auto-calculates optimal rows/columns based on contestant count
- Each cell scales images with bilinear interpolation to fit; shows "initializing..." placeholder
  for contestants that haven't produced an image yet

### Fixed — Evolutionary Tournament Review
- **Parent selection now explicitly excludes the worst contestant** being culled, preventing it
  from being selected as a parent even in edge cases with tied scores
- **Tied-score tiebreaker improved** — when multiple contestants share the worst score, the
  system now prefers to cull the older one (lower generation number) across all ties, not just
  when all scores are identical
- **Stop All resets the Evolve button** — pressing "Stop All" now properly resets the "Stop
  Evolving" button back to "Start Evolving" and clears the countdown display

### Fixed — Flickering Black Triangles
- **`renderTrianglesToNewImage()` returned a shared mutable buffer**: The EDT was reading
  `bestImage` while evolver threads cleared and redrew `bestImageBuffer`, causing black/empty
  triangles. Now allocates a fresh `BufferedImage` snapshot for each improvement, so the EDT
  always reads a complete, immutable image.
- **Memory visibility**: Made `bestImage`, `bestScore`, and `isDirty` volatile in `ImageEvolver`
  for proper cross-thread visibility between evolver threads and the EDT.
- **`TournamentContestant.bestImage/bestScore`**: Made volatile for same reason.

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
