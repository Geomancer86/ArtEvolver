# Evolutionary Tournament v2 — Deep Analysis & Design

## Problem Statement

The v1 evolutionary tournament has a **fatal flaw**: when a new contestant is bred and
spawned, it starts with fitness = 0 (random triangle initialization). Since the ranking
system uses **absolute fitness** to decide who to cull, every newcomer is immediately
the worst performer and gets culled at the very next tick — before it ever has a chance
to demonstrate whether its *parameters* are actually superior.

This defeats the entire purpose of the meta-GA: we're supposed to be finding which
GA parameter configurations lead to the **fastest fitness improvement**, not which
contestant has been running the longest.

## Root Cause Analysis

1. **Absolute fitness is the wrong metric** — A contestant's current fitness primarily
   measures how long it has been running, not how good its parameters are. A new
   contestant with *perfect* parameters would still score 0.0% at tick zero.

2. **No grace period** — New contestants are immediately eligible for culling with no
   evaluation window to build up data.

3. **No velocity tracking** — The system has no concept of "how fast is this contestant
   improving?" which is the actual signal we want to optimize.

4. **Shallow genealogy** — Only parent names are stored as strings; there is no
   structural ancestry tree, no lineage performance tracking, and breeding cannot
   leverage multi-generational information.

## Research: Multi-Generational Genetic Algorithms

### Has anyone used grandparents/great-grandparents in GAs?

**Yes**, but it's an emerging area:

- **Phylotrack** (2024) — Full ancestry tree recording in evolutionary simulations,
  enabling analysis of when traits are gained/lost across generations.
- **Tree Sequence Recording** (Kelleher et al.) — Efficient pedigree storage using
  O(N log N) space, allowing full genealogy without prohibitive memory costs.
- **Gene Heritage Tracking** (IEEE 2021) — Tracking which genes survive across
  generations to understand selection pressure.
- **ALPS (Age-Layered Population Structure)** — Hornby (2006) — Segregates
  populations by "age" (how many generations since a random ancestor was introduced),
  preventing new blood from competing directly against established individuals.
- **Multi-Parent Crossover (DNC, 2024)** — Uses attention mechanisms to select
  genes from *multiple* parents, not just two.

### Key Insight: Lineage-Weighted Breeding

In animal breeding (BLUP — Best Linear Unbiased Prediction), the **entire pedigree**
is used to estimate breeding value. A stallion whose offspring consistently outperform
is valued higher, even if the stallion itself is retired. This maps directly to our
tournament: a parameter configuration whose descendants consistently improve fast
should be favored for parent selection, even if that specific contestant was culled.

## Design: Comprehensive Solution

### A. FitnessTracker (per contestant)

Each contestant accumulates timestamped fitness snapshots:

```
FitnessSnapshot { timestamp, fitness, totalIterations }
```

From this time series, we compute:
- **Velocity** = Δfitness / Δtime (fitness gain per second, windowed)
- **Acceleration** = Δvelocity / Δtime (is improvement speeding up or slowing down?)
- **Projected fitness** = current + velocity × projectionWindow

Windowed calculation uses a configurable window (default 30s) to measure recent
performance rather than lifetime averages, which would dilute the signal.

### B. LineageNode (ancestry tree)

```
LineageNode {
    contestantId, configSnapshot, generation,
    peakFitness, peakVelocity,
    parent (LineageNode or null),
    children (list of LineageNode)
}
```

This creates a full tree where every contestant knows its parent, grandparent,
great-grandparent, etc. The tree persists even after contestants are eliminated.

**Lineage fitness** = weighted average of peak fitness across ancestry:
```
lineageFitness = Σ (peakFitness[ancestor] × decay^depth) / Σ (decay^depth)
```
where `decay` (default 0.7) reduces weight for more distant ancestors.

**Lineage velocity** = same formula applied to peak velocity values.

### C. Grace Period (newcomer protection)

New contestants receive an **immunity window** (default = 1 tick, configurable).
During this window, the contestant cannot be selected as the worst for culling.

After the grace period, the contestant participates normally in ranking. This
ensures every newcomer gets at least `cutoffSeconds × gracePeriodTicks` seconds
of runtime to build up meaningful fitness and velocity data.

### D. Composite Ranking System

Instead of ranking purely by absolute fitness, use a weighted composite score:

```
compositeScore = wFitness × normalizedFitness
               + wVelocity × normalizedVelocity
               + wAcceleration × normalizedAcceleration
               + wLineage × normalizedLineageFitness
```

**Normalization**: Each metric is normalized to [0, 1] range relative to the
current alive population (min-max scaling) so that the weights are meaningful
regardless of the actual fitness scale.

**Ranking Strategy** determines how the weights are applied:

| Strategy | Description |
|----------|-------------|
| `BALANCED` | Uses the manually configured base weights below |
| `VELOCITY_FIRST` | 60% velocity, 15% acceleration, 10% fitness, 15% lineage |
| `FITNESS_FIRST` | 65% fitness, 15% velocity, 5% acceleration, 15% lineage |
| `AUTO` (default) | Linearly interpolates from velocity-first to fitness-first over `autoTransitionGen` generations |

**AUTO mode** is the recommended default: early generations reward fast learners (who
show the most improvement per second), while later generations shift to rewarding peak
fitness once the population has differentiated. The transition point is configurable.

**Base weights** (used by BALANCED, overridden by other strategies):
- `wFitness` = 0.35 — Current absolute fitness still matters
- `wVelocity` = 0.40 — How fast are you improving? (primary signal)
- `wAcceleration` = 0.05 — Are you speeding up or slowing down?
- `wLineage` = 0.20 — How good is your family line?

### E. Multi-Generational Breeding

When selecting parents for a new contestant:

1. **Tournament selection** uses composite score (not raw fitness)
2. **Ancestry-aware crossover**: When crossing two parents, optionally blend in
   grandparent genes with decaying weight:
   ```
   childGene[i] = α × parentA[i] + β × parentB[i]
                 + γ × grandparentA[i] + δ × grandparentB[i] + ...
   ```
   where α + β + γ + δ + ... = 1, and deeper ancestors have exponentially less weight.
3. **Inbreeding prevention**: If both parents share a recent common ancestor
   (within `ancestryDepth` generations), prefer a different partner.

### F. Contestant Lifespan Cap & Promotion

**Problem**: The leading contestant accumulates marginal gains forever, dominating the
tournament and the chart. Even with good velocity tracking, eventually the leader's
velocity drops near zero but their absolute score keeps them at the top indefinitely.

**Solution**: `maxLifespanSeconds` (default 60s). When a contestant exceeds this age:
1. It is **promoted** to the Hall of Fame — not eliminated
2. Promoted contestants stop running (free compute) but stay as breeding candidates
3. Even the top performer gets replaced — their genes live on through children
4. Forces continuous genetic turnover and exploration
5. The fitness chart looks clean: all contestants have similar time spans
6. The oldest expired contestant is promoted first (fairness)

**Promoted Pool (Hall of Fame) — Merit-Based**:
- Promotion is **earned**, not automatic. When a contestant finishes (lifespan or culled):
  - If the pool has room (`< maxPromoted`): promoted
  - If the pool is full: promoted only if score beats the worst promoted (who gets demoted)
  - Otherwise: eliminated (not good enough for the hall of fame)
- Promoted contestants retain their config and final score
- They are available as **parent candidates** when breeding new offspring
- `maxPromoted` cap (default 10) keeps only the best-ever configs in the breeding pool
- A **lifespan enforcement timer** (every 5s) hard-caps contestant age independently of
  the cull cycle, ensuring no contestant ever exceeds `maxLifespanSeconds`
- UI shows promoted with gold medal badge, between active and eliminated in sort order
- Dashboard JSON includes `"promoted": true` field

### G. Preset Strategy Injection

**Problem**: Breeding from existing contestants can converge on a local optimum. If the
initial pool only contains a subset of possible strategies, the tournament may never
explore radically different approaches.

**Solution**: `presetInjectionInterval` (default 3). Every Nth spawn cycle, instead of
breeding from two parents, the system injects a fresh preset strategy configuration
that hasn't been tried by any contestant (active, promoted, or eliminated).

Available presets (16 total): Balanced, Aggressive Explorer, Grid Refiner, Targeted
Precision, Heavy Random, Close Mutation Focus, Fast Convergence, Wide Search, Micro
Surgeon, Chaos Engine, Gradient Chaser, Population Boom, Sniper, Blitz, Deep Grid,
Hybrid Adaptive.

Once all presets have been injected at least once, breeding resumes normally. The
narrative log indicates preset injections with a dice icon and "[untried strategy]" tag.

### G2. Stale Detection (Early Kill)

**Problem**: Some contestants produce zero fitness improvement after their initial
evaluation. These flat-liners waste their entire lifespan (e.g. 60 seconds) doing
nothing useful, blocking a slot that a new, potentially better contestant could use.

**Solution**: `staleThresholdSeconds` (default 15). The lifespan enforcer (polling
every 5 seconds) checks each active contestant's velocity from the FitnessTracker.
If a contestant has been running for at least `staleThresholdSeconds` and its velocity
is near-zero (< 0.000001), it is immediately eliminated — NOT promoted, since it
produced no meaningful improvement worth preserving in the breeding pool.

This dramatically increases tournament throughput by recycling dead-weight contestants
into fresh breeding opportunities 4x faster than waiting for the lifespan cap.

### G3. Rich Child Naming & Lineage Tracking

**Problem**: Generic names like `G5a-AlpxBet` give little insight into breeding method.

**Solution**: Children now carry descriptive names encoding:
- **Generation**: `G5` = generation 5
- **Parents**: `Alpha×Beta` (abbreviated to 5 chars)
- **Crossover type**: `BLX` (standard BLX-alpha) or `ANC` (multi-generational ancestral)
- **Mutation count**: `M3` = 3 genes mutated

Example: `G5·Alpha×Beta·BLX·M3`
Presets: `G5·P·GridRef`

Each contestant also stores `breedType` (e.g., "BLX+3mut", "Preset:Grid Refiner")
and `parentage` (full parent names). Both are displayed in the Tournament Manager
table (new "Parentage" and "Breed" columns) and in the dashboard JSON/HTML.

### H. Configurable Parameters (all future meta-optimizable)

| Parameter | Default | Description |
|-----------|---------|-------------|
| `gracePeriodTicks` | 2 | Ticks of immunity for new contestants |
| `velocityWindowSeconds` | 30 | Window for velocity/acceleration calculation |
| `fitnessWeight` | 0.35 | Base weight of absolute fitness in composite score |
| `velocityWeight` | 0.40 | Base weight of improvement speed |
| `accelerationWeight` | 0.05 | Base weight of improvement acceleration |
| `lineageWeight` | 0.20 | Weight of ancestry performance |
| `lineageDecay` | 0.7 | How much each generation back reduces weight |
| `ancestryDepth` | 3 | Max generations back for breeding/lineage |
| `maxLifespanSeconds` | 60 | Max age before promotion (0 = disabled) |
| `staleThresholdSeconds` | 15 | Kill flat-line contestants after N seconds of zero improvement (0 = off) |
| `maxPromoted` | 10 | Max contestants in the hall of fame |
| `presetInjectionInterval` | 3 | Every Nth spawn inject untried preset (0 = off) |
| `rankingStrategy` | AUTO | BALANCED, VELOCITY_FIRST, FITNESS_FIRST, AUTO |
| `autoTransitionGen` | 10 | Generations for AUTO to fully shift to fitness-first |
| `useAncestralCrossover` | true | Blend in grandparent genes during breeding |

## Implementation Plan

1. **FitnessTracker** — New class with `addSnapshot()`, `getVelocity()`,
   `getAcceleration()`, `getProjectedFitness()`
2. **LineageNode** — New class representing ancestry tree nodes
3. **TournamentContestant** — Add `FitnessTracker`, `LineageNode`, `graceTicks`
4. **EvolutionaryTournament** — Replace absolute-fitness ranking with composite
   scoring; add grace period logic; use ancestry-aware breeding; store full lineage
5. **TournamentManagerWindow** — Expose new parameters in Evo Settings dialog;
   show velocity/composite score in table and detail panel; display lineage in history
6. **GenerationRecord** — Enrich with velocity data, composite scores, lineage info

## Dynamic Tournament Philosophy

The core insight is that we don't know the optimal tournament pace. Maybe 5 seconds
between iterations is better than 60, or maybe 300. The answer depends on:

- Image complexity and triangle count
- Number of contestants and threads per contestant
- Current phase of evolution (early = lots of easy gains, late = diminishing returns)
- Hardware (more cores = faster score accumulation = shorter intervals make sense)

### Adaptive Cutoff (Machine Gun Start)

The tournament uses a "machine gun start" strategy — begin cycling fast (5-10s)
for rapid initial churn through parameter combinations, then dynamically adapt:

- **Default cutoff**: 10 seconds (was 60s), giving immediate feedback
- **Autopilot fast-start**: Begins at the adaptive minimum (5s)
- When stalled (2+ generations), **shorten** the interval — aggressively at 2/3
  of current when stalled 5+ generations
- When improving, **lengthen** the interval — +10s/tick when under 30s, +5s/tick
  above 30s, giving good contestants time to differentiate
- Bounded by configurable min/max (default 5s-300s)
- **Grace period default**: 2 ticks (increased from 1 to compensate for faster cycles)

### Multi-Spawn

Rather than always culling exactly 1 and breeding exactly 1, the system supports
`spawnsPerTick` > 1 (configurable). When set to e.g. 3, each tick:
1. Rank all alive contestants by composite score
2. Cull the worst eligible, breed a replacement
3. Re-rank (the pool has changed), cull the next worst, breed another
4. Repeat up to N times, respecting `minContestants` floor

This is particularly useful on high-core systems where you can afford more
parallelism: more contestants running simultaneously, more culled per tick,
faster exploration of the parameter space.

### Java 21 Performance Features

For the Threadripper 2950x (16 cores, 128GB RAM):

1. **ZGC (Z Garbage Collector)** — Sub-millisecond GC pauses regardless of heap
   size. Critical for 16+ concurrent evolver threads that all allocate.
2. **Generational ZGC** (`-XX:+ZGenerational`, Java 21+) — Separates young/old
   generations for better throughput than non-generational ZGC.
3. **Virtual Threads** (Java 21) — Future: could replace daemon threads for
   evolvers, eliminating context-switch overhead for 100+ threads. Not yet
   implemented but the architecture supports it.
4. **`-XX:-TieredCompilation`** — Skips the C1 interpreter tier, going straight
   to C2 optimized compilation. Slower startup (~5s) but steady-state performance
   is 10-20% better for long-running evolution.
5. **`-XX:+AlwaysPreTouch`** — Pre-faults all heap pages at startup, avoiding
   OS page faults during evolution that would cause latency spikes.

## Why This Matters

With these changes, the evolutionary tournament becomes a proper meta-optimizer:
- New contestants get fair evaluation time
- Fast improvers are rewarded even before they reach peak fitness
- Lineage tracking rewards parameter configurations that consistently produce
  good offspring, creating evolutionary pressure toward robust parameter spaces
- Multi-generational breeding preserves and leverages proven genetic material
  from ancestors, preventing catastrophic forgetting
- Dynamic pacing adapts to the problem's characteristics automatically
- Multi-spawn enables faster exploration on high-core hardware
- All parameters are exposed for future meta-meta-optimization

## Prehistoric Mode — Progressive Evolution Tournament

A gamified tournament mode that starts from absolute zero and progressively unlocks
capabilities through 8 geological eras. This serves as both an educational tool
(watching the fitness curve inflect when delta evolution unlocks in Era 3 is dramatic)
and a proper ablation study.

### Implementation

- `PrehistoricMode.java` — Controller managing era progression, contestant spawning,
  auto-advance timer, capability constraints, and preset pool tracking
- `EvolutionConfig.createPrimordial()` through `createEra7()` — Era-specific factory
  methods producing stripped-down configs
- `TournamentManagerWindow` — Prehistoric Mode UI panel with era display, progress
  bar, advance button, capability badges, and manual contestant controls

### Era Progression

| Era | Name               | Key Unlocks                    | Approx Speed  |
|-----|--------------------|--------------------------------|---------------|
| 0   | Primordial Soup    | Random init, random swaps only | ~90 iter/sec  |
| 1   | Single Cell        | Grid + close mutations         | ~90 iter/sec  |
| 2   | Multicellular      | Smart init, crossover, pop=2   | ~90 iter/sec  |
| 3   | Cambrian Explosion | Delta evolution + targeted      | ~4500 iter/sec|
| 4   | Age of Fish        | 2 threads                      | ~9000 iter/sec|
| 5   | Age of Reptiles    | Scaling threads, presets        | Varies        |
| 6   | Age of Mammals     | Full threads, all strategies    | Varies        |
| 7   | Age of Intelligence| Evolutionary tournament (meta)  | Full system   |

The Era 3 inflection point (legacy -> delta evolution) is the most visually dramatic
moment: iteration throughput jumps ~50x and the fitness curve visibly bends upward.

---

## System Monitor & Autopilot Mode

### SystemMonitor.java

A dedicated JMX-based system resource monitor providing:
- **CPU**: Process + system-wide load via `com.sun.management.OperatingSystemMXBean`
- **Memory**: JVM heap usage + physical RAM (total/free) on supported JVMs
- **Disk**: Workspace drive total/free via `java.io.File`
- **Threads**: Active JVM thread count and peak, plus available processors

All metrics are polled periodically (every refresh cycle, ~2-5s) and available as
both raw values and formatted strings for UI display.

### Autopilot Mode

One-click autonomous tournament management with gradual resource ramp-up:

1. **Resource Limits**: User configures max CPU %, max RAM %, max Heap %
2. **Thread Budget**: Hard ceiling at 75% of cores * CPU limit, tracked per-contestant
3. **Spawn Cooldown**: Minimum 10 seconds between spawns to let existing contestants
   stabilize and produce initial images before adding more load
4. **canAddWork()**: Before spawning, checks both process AND system CPU against
   90% of the configured limit (safety margin for JMX lag), plus RAM and Heap
5. **Auto-progression**: If no contestants exist, starts Prehistoric Mode with
   conservative 60% initial thread budget. If contestants exist, ensures running.
6. **Era Advance Gating**: Prehistoric mode defers era advancement when thread
   utilization exceeds 85% of budget, retrying every 15 seconds
7. **Tournament activation**: When 3+ contestants are active AND at least one has
   produced a score, automatically creates the Evolutionary Tournament

### Smart Test Profiles

- `@Tag("slow")` on BenchmarkTest, ArtEvolverToolsTest, CrossOverTest
- Surefire default: `<excludedGroups>slow</excludedGroups>`
- `mvn test -Pfull` profile overrides to run everything
- Development cycle: ~15s (9 fast tests) vs ~210s (27 all tests)

### Smart Announcer — Live Status & Narrative Logs

The evolution history panel acts as a "smart announcer" with two components:

**Live Status Block** (top of panel, updates every refresh cycle):
- Mini-leaderboard with rank, score, velocity arrows, and promise indicators
- Promise tags per contestant: FAST (high velocity), rising (positive acceleration),
  fading (negative velocity + acceleration), at risk (low score + stalled)
- Grace period markers for protected newcomers
- Uptime per contestant, average fitness, score spread (leader - trailer)
- Countdown to next cycle with interval info

**Generation Narratives** (appended after each cycle):
- Score deltas between generations (average fitness change)
- Stall/convergence warnings with generation count
- Culling stories: contestant's fitness, velocity trend, birth generation
- Cycle speed annotations: machine gun (≤10s), fast (<30s), extended (>120s)
- Timestamped generation events with population counts
- Leader identification with fitness and velocity
- Breeding lineage (parents, ancestral crossover, grace period)
- Adaptive cutoff state
- Prehistoric mode era descriptions with capability unlocks

---

## Browser Dashboard — Real-Time Leaderboard

### Architecture

```
Java App (Swing)
  └── DashboardServer.java
        ├── com.sun.net.httpserver.HttpServer (port auto-selected)
        ├── GET /            → dashboard.html (classpath resource)
        ├── GET /api/state   → JSON (full tournament state, polled every 2s)
        ├── GET /api/image/{id}  → 300px PNG thumbnail
        └── GET /api/export/{id} → Full-res PNG download
```

### JSON State Schema (`/api/state`)

- `system` — CPU (process + system), heap, RAM, disk, threads, cores
- `tournament` — generation, countdown, cutoff, adaptive state, best-ever
- `prehistoric` — era number, name, auto-advance status
- `autopilot` — enabled flag
- `contestants[]` — id, name, rank, score, velocity, acceleration, peak,
  iterations, status flags, generation, parentage, config, color, uptime
- `history[]` — narrative log strings (pre-formatted)

### Dashboard Features

- Dark theme with GitHub-inspired design
- System metrics bar with animated gauge fills (color: green/yellow/red)
- Leaderboard cards: rank, image thumbnail, fitness stats, config summary
- Eliminated contestants: faded, skull rank icon, export download button
- Evolution history: syntax-highlighted log with newest entries first
- Tournament summary sidebar: active/eliminated/total counts, average fitness
- Status badges: LIVE (pulsing), AUTOPILOT, ERA (prehistoric mode)
- Auto-refresh every 2 seconds, graceful reconnection on network errors
- Fully responsive: collapses sidebar below leaderboard on narrow screens

---

## Evolution Clicker — Cookie Clicker Mode

### Design Philosophy

Channeling the greatest game designers:
- **Carmack**: Every upgrade maps to a real evolution parameter — deep technical layers
- **Will Wright**: Emergent complexity from simple multiplicative rules
- **Sid Meier**: "One more upgrade" — every decision is interesting
- **Miyamoto**: Immediate tactile feedback — floating EP numbers, achievement popups
- **Kojima**: Meta-narrative — the algorithm "becoming aware" through prestige
- **Gabe Newell**: Platform economy — currencies cascade into each other
- **Sandy Petersen**: Random events create emergent gameplay moments
- **Tim Cain**: Deep upgrade trees with meaningful build diversity
- **Gygax**: Critical hits, luck mechanics, dice rolls on events

### Currency System

| Currency | Symbol | Earned From | Spent On |
|----------|--------|-------------|----------|
| Evolution Points (EP) | ⚡ | Clicks, passive production, fitness gains | Upgrades (most) |
| Mutation Crystals (MC) | 💎 | Achievements, rare events | Special upgrades |
| Genome Fragments (GF) | 🌟 | Prestige/Ascension (sqrt scaling) | Prestige upgrades |

### Upgrade Categories (52 total)

| Category | Count | Currency | Theme |
|----------|-------|----------|-------|
| Mutation Lab | 8 | EP | All mutation types + mastery |
| Population Lab | 6 | EP | Population dynamics |
| Crossover Lab | 5 | EP | Crossover mechanics |
| Initialization | 4 | EP | Starting conditions |
| Computing | 6 | EP | Thread/speed optimization |
| Tournament | 6 | EP | Tournament parameters |
| Meta-Evolution | 6 | EP | Meta-GA tuning |
| Special | 8 | MC | Rare powerful effects |
| Prestige | 4 | GF | Permanent bonuses |

Each upgrade has: base cost, exponential cost growth (1.14x-1.70x per level),
max level (20-200), and a real effect target mapping to EvolutionConfig parameters.

### Achievement System (120+)

Procedurally generated across 10 milestone categories:
- Fitness (20 tiers: 0.1% to 99.9%)
- Total EP (15 tiers: 10 to 1T)
- Clicks (12 tiers: 1 to 1M)
- Play Time (10 tiers: 1min to 1 week)
- Generations (10 tiers: 1 to 10K)
- Upgrades (8 tiers: 1 to 500)
- Prestige (7 tiers: 1 to 100)
- Speed (10 tiers: 100 to 10M iter/s)
- 10+ hidden discovery achievements

Each achievement grants EP bonus + permanent multiplier (1.001x to 1.25x).

### Prestige / Ascension

- Available when enough EP has been earned lifetime
- Resets: EP, MC, non-prestige upgrades, fitness progress
- Keeps: Achievements, prestige upgrades, Genome Fragments
- Reward: GF = floor(sqrt(totalEpEarned / 1000) * prestigeMultiplier)
- Each GF provides +1% to all production (compounding)
- After first ascension: Prestige upgrade tier becomes visible

### Random Events (8 types)

| Event | Duration | Effect | Base Chance |
|-------|----------|--------|-------------|
| Golden Mutation | 30s | 2x mutations | 0.3%/5s |
| Fitness Surge | 45s | 3x fitness gains | 0.2%/5s |
| EP Rain | 30s | 5x EP production | 0.4%/5s |
| Population Boom | 60s | +1 population | 0.2%/5s |
| Algorithm Insight | 45s | 2.5x algorithms | 0.15%/5s |
| Cosmic Alignment | 120s | 2x everything | 0.1%/5s |
| Time Dilation | 60s | 2x speed | 0.2%/5s |
| Mutation Frenzy | 30s | 3x mutation rate | 0.3%/5s |

Event spawn chance scales with Lucky Star upgrade + Eternal Fortune prestige.

### Combinatorial Option Space

With 52 upgrades × avg 100 levels × prestige multipliers × 120 achievements ×
8 event types × synergy combinations, the total build space is effectively
**billions of distinct configurations** — each playthrough is unique.

### Endgame Progression

1. **Early Game**: Manual clicking, first upgrades, unlocking mutation types
2. **Mid Game**: Auto-clicker, tournament upgrades, first events
3. **Late Game**: Meta-evolution, special upgrades, approaching first prestige
4. **Prestige 1**: Unlock GF currency and eternal upgrades, restart with bonuses
5. **Prestige 5+**: Compound growth, faster cycles, deeper upgrades
6. **Prestige 25+**: All achievement tiers, event optimization
7. **Prestige 100+**: Effectively infinite scaling — numbers in the trillions
