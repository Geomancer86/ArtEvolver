# Multi-Stage (Geared) Competitors — Deep Analysis & Design

## Problem Statement

Current competitors use a **single static configuration** for their entire lifetime.
Whether they run for 15 seconds or 60 seconds, the mutation rates, grid sweep
intensity, population size, and crossover settings remain fixed.

This is suboptimal because evolution naturally has distinct phases:

1. **Early game (exploration)**: Need aggressive, wide-range mutations to escape local
   optima and establish a rough approximation quickly
2. **Mid game (exploitation)**: Need balanced operators — grid refinement, targeted swaps,
   moderate random mutations to converge on good regions
3. **End game (polishing)**: Need precise, small mutations — close mutation focus,
   high targeted swaps, minimal random noise to squeeze out the last few % of fitness

A "Chaos Engine" preset is great for early game but terrible for polishing. A "Sniper"
preset is great for endgame but wastes time in early game doing nothing useful on a
random canvas. The ideal competitor would **shift gears** as it evolves.

## Design Goals

1. **Backward compatible**: Existing single-stage competitors ("static") work unchanged.
   A single-stage competitor is just a multi-stage competitor with 1 stage.
2. **Stage transitions are part of the DNA**: The number of stages, the config for each
   stage, and the transition triggers are all evolvable parameters. The meta-GA breeds
   not just parameters but entire evolution *strategies* (sequences of parameter sets).
3. **Multiple transition trigger types**:
   - **Time-based**: Switch after N seconds (e.g., aggressive for 20s, then precision)
   - **Stale-based**: Switch when velocity drops below threshold (automatic gear shift)
   - **Fitness-based**: Switch when reaching a fitness milestone (e.g., 50%, 80%)
4. **Live gear shifting**: The stage transition happens mid-evolution without restarting.
   The `ImageEvolver` simply starts reading different config values.
5. **Visible in UI**: The current stage/gear is shown in the tournament table, chart,
   and dashboard. Stage transitions are logged in the evolution narrative.

## Architecture

### Core Data Model

```
EvolutionStage {
    String name;              // "Explore", "Converge", "Polish", etc.
    EvolutionConfig config;   // The full parameter set for this stage
    StageTrigger trigger;     // When to transition TO the next stage
}

StageTrigger {
    TriggerType type;         // TIME, STALE, FITNESS
    double value;             // seconds, velocity threshold, or fitness %
}

enum TriggerType { TIME, STALE, FITNESS }

MultiStageConfig {
    List<EvolutionStage> stages;   // Ordered list: stage 0 = start
    int currentStage = 0;
}
```

### Where It Lives

The `EvolutionConfig` class gains an optional `List<EvolutionStage>` field.
If null or empty (the default), the competitor behaves exactly as today — one
stage, the config IS the stage. If populated, the contestant cycles through stages.

```java
// In EvolutionConfig:
public List<EvolutionStage> stages = null;  // null = legacy single-stage

// Helper:
public boolean isMultiStage() { return stages != null && stages.size() > 1; }
```

A single-stage competitor's gene array is identical to today (9 genes). A multi-stage
competitor's gene array is: `[stage0_genes... stage0_trigger_value, stage1_genes... 
stage1_trigger_value, ...]`. The gene count scales with stages.

### Stage Transition Engine

In `TournamentContestant`, a new lightweight timer checks stage transitions:

```java
// Called periodically (e.g., every 2 seconds) or on every fitness update
public void checkStageTransition() {
    if (config.stages == null || currentStageIndex >= config.stages.size() - 1) return;
    
    EvolutionStage current = config.stages.get(currentStageIndex);
    StageTrigger trigger = current.trigger;
    
    boolean shouldAdvance = switch (trigger.type) {
        case TIME -> getAgeSeconds() >= trigger.value;
        case STALE -> fitnessTracker.getVelocity() < trigger.value 
                      && fitnessTracker.getElapsedSeconds() > 5;
        case FITNESS -> getBestScore() >= trigger.value;
    };
    
    if (shouldAdvance) {
        currentStageIndex++;
        EvolutionStage next = config.stages.get(currentStageIndex);
        applyStage(next);  // Hot-swap config on all evolvers
        System.out.println("[Stage] " + getName() + " shifted to gear " 
            + (currentStageIndex + 1) + "/" + config.stages.size() 
            + ": " + next.name);
    }
}

private void applyStage(EvolutionStage stage) {
    // Copy stage config values into the live config (which evolvers reference)
    EvolutionConfig sc = stage.config;
    config.gridMutationChances = sc.gridMutationChances;
    config.gridMutationDecay = sc.gridMutationDecay;
    config.randomMutationChances = sc.randomMutationChances;
    config.randomMutationPercent = sc.randomMutationPercent;
    config.closeMutationChances = sc.closeMutationChances;
    config.closeMutationPercent = sc.closeMutationPercent;
    config.targetedSwapAttempts = sc.targetedSwapAttempts;
    // Note: threads, population, crossoverMax are NOT changed mid-run
    // (would require restarting evolvers)
}
```

**Critical insight**: The `ImageEvolver` reads `config.gridMutationChances` etc. at the
start of each evolve batch (every few milliseconds). Since `config` is a reference, mutating
its fields takes effect on the very next batch with zero restart cost. The evolvers keep
their threads, their population, their triangle state — only the mutation parameters change.

### Gene Array for Meta-GA Breeding

For multi-stage competitors to participate in the evolutionary tournament, their entire
configuration (all stages + trigger values) must be encodable as a gene array:

```
Single-stage (legacy): [gene0..gene8] = 9 genes

Two-stage:   [stage0_gene0..gene8, trigger0_value,
              stage1_gene0..gene8, trigger1_value] = 21 genes

Three-stage: [stage0_gene0..gene8, trigger0_value,
              stage1_gene0..gene8, trigger1_value,
              stage2_gene0..gene8, trigger2_value] = 31 genes

General: stages * (9 config genes + 1 trigger gene) + 1 (trigger type per stage)
```

The trigger type (TIME/STALE/FITNESS) can be encoded as a float in [0, 3) and
discretized. Or it can be fixed for simplicity (e.g., always TIME-based initially,
and the meta-GA evolves toward optimal trigger values).

### Crossover Between Different Stage Counts

When breeding two parents with different stage counts:

1. **Same count**: Standard BLX-alpha crossover on all gene positions. Straightforward.
2. **Different count**: The child inherits the stage count from one parent (randomly or
   from the better parent). Genes are aligned from stage 0 upward. Extra stages from the
   longer parent are included with reduced weight. Missing stages are interpolated from
   the parent that has them.

Alternatively (simpler and recommended for v1):
- **Fixed max stages**: All multi-stage competitors have the same number of stages
  (e.g., 3: start/mid/end). The meta-GA only evolves the config values and trigger
  thresholds, not the number of stages. A "single-stage" competitor is just one where
  all 3 stages have identical configs.

### Preset Multi-Stage Strategies

```
"3-Gear Classic":
  Stage 1: Chaos Engine     (trigger: TIME 15s)
  Stage 2: Balanced         (trigger: TIME 30s)  
  Stage 3: Sniper           (trigger: none — runs to end)

"Stale Shifter":
  Stage 1: Aggressive Explorer  (trigger: STALE velocity < 0.0001)
  Stage 2: Grid Refiner         (trigger: STALE velocity < 0.00001)
  Stage 3: Micro Surgeon        (trigger: none)

"Fitness Ladder":
  Stage 1: Wide Search          (trigger: FITNESS 0.30)
  Stage 2: Gradient Chaser      (trigger: FITNESS 0.60)
  Stage 3: Close Mutation Focus  (trigger: none)

"Sprint to Precision":
  Stage 1: Blitz                (trigger: TIME 10s)
  Stage 2: Targeted Precision   (trigger: TIME 20s)
  Stage 3: Micro Surgeon        (trigger: none)

"Adaptive 5-Gear":
  Stage 1: Chaos Engine         (trigger: STALE velocity < 0.001)
  Stage 2: Wide Search          (trigger: STALE velocity < 0.0005)
  Stage 3: Balanced             (trigger: STALE velocity < 0.0001)
  Stage 4: Gradient Chaser      (trigger: STALE velocity < 0.00005)
  Stage 5: Micro Surgeon        (trigger: none)
```

### Integration with Existing Systems

| System | Impact | Change |
|--------|--------|--------|
| `EvolutionConfig` | Add `stages` field, `EvolutionStage` inner class | Additive |
| `TournamentContestant` | Add `currentStageIndex`, `checkStageTransition()` | Additive |
| `EvolutionaryTournament` | Breed multi-stage gene arrays | Extend `breedConfigs()` |
| `TournamentManagerWindow` | Show current gear in table, add multi-stage presets | UI only |
| `FitnessTracker` | No change — already tracks velocity for STALE triggers | None |
| `ImageEvolver` | No change — already reads config by reference | None |
| `DashboardServer` | Export `currentStage` in JSON | Additive |
| `FitnessChartWindow` | Optionally mark stage transitions on chart | Nice-to-have |
| `PrehistoricMode` | No change — it creates static configs for eras | None |

### What Does NOT Change

- Static (single-stage) competitors work identically — no regressions
- The promoted pool, hall of fame, and lifespan/stale/hopeless detection work unchanged
- The inner GA (triangle evolution) is untouched
- Threads, population, crossover settings remain fixed per-contestant (set at creation)
- The `isFinished()`, `isProtected()`, and all lifecycle methods are unchanged

## Implementation Plan

### Phase 1: Data Model (minimal, foundational)
1. Create `EvolutionStage` class with `name`, `config` (EvolutionConfig), `trigger`
2. Create `StageTrigger` class with `type` (enum) and `value` (double)
3. Add `List<EvolutionStage> stages` to `EvolutionConfig`
4. Add `currentStageIndex`, `checkStageTransition()`, `applyStage()` to `TournamentContestant`

### Phase 2: Gene Array Extension
5. Extend `EvolutionConfig.toGeneArray()` / `fromGeneArray()` for multi-stage
6. Extend `breedConfigs()` and `breedWithAncestry()` to handle multi-stage arrays
7. Update `GENE_COUNT`, `GENE_MIN`, `GENE_MAX` to be dynamic based on stage count

### Phase 3: Preset Strategies
8. Add 4-5 multi-stage preset strategies to `TournamentManagerWindow`
9. Add "Multi-Stage" section in Quick Setup dialog

### Phase 4: UI & Monitoring
10. Show current gear in tournament table (new column or in status)
11. Log stage transitions in console and evolution narrative
12. Export stage info in dashboard JSON

### Phase 5: Testing & Polish
13. Verify single-stage backward compatibility
14. Test multi-stage breeding across stage counts
15. Verify stale/hopeless detection works correctly with stage transitions
    (a contestant might appear "stale" right before a gear shift saves it)

## Open Questions

1. **Should stage transitions reset stale detection?** Yes — shifting gears should give
   the contestant a fresh window to prove the new config. Otherwise a contestant about
   to shift gears might be killed for being "stale" in its current gear.

2. **Should HOPELESS detection account for upcoming gear shifts?** Partially — if a
   contestant has more gears to go, the projected-fitness calculation should use a
   more generous multiplier since the next gear might accelerate it.

3. **Fixed vs variable stage count?** Start with fixed 3 stages (start/mid/end) for
   simplicity. The meta-GA evolves all 3 configs + 2 trigger values. A "single-stage"
   competitor is one where stage 0's trigger is set to TIME=999 (never triggers).
   Variable stage count can be added later.

4. **What triggers for the last stage?** None — the last stage runs until the contestant
   finishes (lifespan, promotion, elimination). Its trigger field is ignored.
