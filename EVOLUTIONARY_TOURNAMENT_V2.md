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

**Default weights** (configurable, future meta-optimizable):
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

### F. Configurable Parameters (all future meta-optimizable)

| Parameter | Default | Description |
|-----------|---------|-------------|
| `gracePeriodTicks` | 1 | Ticks of immunity for new contestants |
| `velocityWindowSeconds` | 30 | Window for velocity/acceleration calculation |
| `fitnessWeight` | 0.35 | Weight of absolute fitness in composite score |
| `velocityWeight` | 0.40 | Weight of improvement speed |
| `accelerationWeight` | 0.05 | Weight of improvement acceleration |
| `lineageWeight` | 0.20 | Weight of ancestry performance |
| `lineageDecay` | 0.7 | How much each generation back reduces weight |
| `ancestryDepth` | 3 | Max generations back for breeding/lineage |
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

### Adaptive Cutoff

Instead of guessing, we let the system find its own pace:
- When stalled (3+ generations without improvement), **shorten** the interval
  (faster turnover to try more parameter combinations)
- When improving, **lengthen** the interval (give good contestants more time to
  differentiate themselves)
- Bounded by configurable min/max (default 15s-300s)

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
