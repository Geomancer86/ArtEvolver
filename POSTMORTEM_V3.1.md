# ArtEvolver v3.1.0 — Release Postmortem

**Date**: February 18, 2026
**Version**: v2.04 → v3.1.0
**Commits**: 48 commits on develop branch
**Duration**: Single development sprint
**Status**: Released locally (not pushed)

---

## Executive Summary

ArtEvolver v3.1 represents a transformational release that evolved the project from a
single-threaded genetic algorithm for triangle art into a multi-layered, self-optimizing
meta-evolutionary system with tournament management, autopilot, real-time dashboards,
and an embedded gamification layer. The release addressed 15+ user-reported bugs during
iterative testing and introduced 20+ major features.

---

## What Was Built

### Core Engine Improvements
1. **DeltaFitnessEngine** — 50x faster evolution through incremental fitness computation
2. **Smart Initialization** — Intelligent starting configurations for faster convergence
3. **Targeted Mutation** — Precision mutation pipeline with ordered operators
4. **Permutation Validation** — Integrity checks for the color constraint system
5. **LAP Solver** — Jonker-Volgenant algorithm for optimal color assignments

### Tournament System (the heart of v3.1)
6. **Tournament Mode** — Independent parallel evolution with multiple contestants
7. **Evolutionary Tournament v1** — Meta-GA that breeds the GA parameters themselves
8. **Evolutionary Tournament v2** — Velocity-aware composite ranking, grace periods,
   ancestry trees, multi-generational breeding, inbreeding prevention
9. **Dynamic Tournament** — Multi-spawn, adaptive cutoff, convergence detection
10. **Prehistoric Mode** — Progressive 8-era Genesis mode from brute-force to full meta-GA
11. **Promoted Pool (Hall of Fame)** — Merit-based promotion with above-average threshold
12. **16 Preset Strategies** — From "Balanced" to "Chaos Engine" to "Sniper"
13. **Rich Child Naming** — Full lineage tracking: `G5·Alpha×Beta·BLX·M3`

### Lifecycle Management
14. **Stale Detection** — Kill flat-line contestants after 15s of zero improvement
15. **HOPELESS Detection** — Projected-fitness early kill for slow starters
16. **Lifespan Cap** — 60s hard cap independent of grace period
17. **Population Cap** — Thread-budget-derived max alive count

### Infrastructure
18. **System Monitor** — Real-time CPU/RAM/Disk/Thread metrics via JMX
19. **Autopilot Mode** — Fully automated tournament management with resource limits
20. **Browser Dashboard** — Real-time HTML leaderboard with REST API
21. **Fitness Chart** — Dual X-axis (iterations/time), multi-series, dynamic
22. **Evolution Clicker** — Cookie Clicker-style gamification with 52 upgrades

### UI/UX
23. **Configurable Parameters** — Full UI panels for every tuneable
24. **Quick Setup Presets** — One-click contestant configurations
25. **Image Preview Thumbnails** — File picker with image previews
26. **Screen-Aware Sizing** — Windows auto-size to screen dimensions
27. **Paint-by-Colors Export** — High-res output with color names and outlines

---

## What Went Well

### 1. Iterative User Testing Was Invaluable
Every feature went through at least one round of "deploy → user tests → bugs reported →
fix" cycle. This caught issues that no amount of static analysis would find:
- Autopilot CPU saturation (spawning every 1s with no cooldown)
- Grace period shielding lifespan enforcement (120s survivors)
- Promoted ghost bug (flag not cleared on demotion)
- Population explosion (118 active when 8 expected)

### 2. Layered Architecture Scaled Well
The separation of `TournamentContestant` → `EvolutionaryTournament` → `TournamentManagerWindow`
→ `ArtEvolver` allowed features to be added at the right abstraction layer without
breaking existing functionality. The `EvolutionConfig` gene array system made the meta-GA
elegant — the tournament breeds parameter vectors the same way the inner GA breeds triangles.

### 3. The Meta-GA Concept Works
The evolutionary tournament successfully discovers good parameter combinations automatically.
Configurations that would never be tried by a human (e.g., extreme grid mutation with
minimal random noise) emerge naturally from the breeding process. The preset injection
system ensures exploration diversity.

### 4. Documentation-First Approach
Writing `EVOLUTIONARY_TOURNAMENT_V2.md` before coding forced clear thinking about the
newcomer problem, composite ranking, and grace periods. This design document served as the
north star throughout implementation and made complex decisions traceable.

---

## What Went Wrong

### 1. Autopilot Was the Source of Most Bugs
The autopilot's interaction with the evolutionary tournament was the single biggest source
of issues. Three separate bug-fix commits addressed autopilot behavior:

| Bug | Root Cause | Impact |
|-----|-----------|--------|
| CPU saturation | No spawn cooldown | 100% CPU, system hang |
| Population explosion (118 active) | Spawns faster than culling | Memory/thread exhaustion |
| No fitness growth in genesis | `return` statement blocked evo tournament start | Flat chart |
| Lifespan not enforced | Grace ticks + adaptive cutoff = 120s immunity | Wasted compute |

**Lesson**: Autonomous systems need hard invariants, not soft hints. The lifespan cap
should have been unconditional from day one. The population should have had a hard cap
from the first autopilot commit.

### 2. State Flag Management Was Error-Prone
The promoted ghost bug (39 promoted with maxPromoted=10) was caused by a contestant having
`promoted=true` AND `eliminated=true` simultaneously. The `getPromoted()` method only
checked `isPromoted()`, not `!isEliminated()`, so demoted contestants were phantom-counted.

**Lesson**: Boolean state flags that interact (`promoted`, `eliminated`, `running`) should
use an enum (`ACTIVE`, `PROMOTED`, `ELIMINATED`) instead of independent booleans. This
would make invalid states unrepresentable.

### 3. Timer-Based Systems Need Careful Analysis
The grace period was designed to protect from competitive culling but accidentally also
protected from the lifespan hard cap. The interaction between `graceTicks` (decremented
by onCutoffTick, which fires every 60-300s) and the lifespan enforcer (fires every 5s)
was not analyzed at design time.

**Lesson**: When adding a new timer-based enforcer, enumerate ALL conditions it checks
and verify each one against ALL existing mechanisms.

### 4. Feature Scope Creep Was Real but Managed
The Evolution Clicker mode, while impressive (52 upgrades, 120+ achievements, prestige
system), was acknowledged as a detour from the core optimization goal and deprioritized.
The browser dashboard, while valuable for monitoring, added significant code surface area.

**Lesson**: Having a clear "core path" vs "extras" distinction helped prioritize bug
fixes over new features when issues emerged during testing.

---

## Bugs Found and Fixed (Chronological)

| # | Bug | Fix Commit | Severity |
|---|-----|-----------|----------|
| 1 | UI progress labels not updating | `bc683ed` | Medium |
| 2 | Window sizing requiring scrollbars | `5b039a1` | Low |
| 3 | Contestant combo reset to first on selection | `959d0d1` | Medium |
| 4 | Flickering black triangles (thread safety) | `d892086` | High |
| 5 | Evolutionary tournament not firing | `2f05404` | Critical |
| 6 | Tournament chart display issues | `5b67185` | Medium |
| 7 | History panel stuck at Gen 1 | `4a17c02` | Medium |
| 8 | FitnessTracker ConcurrentModificationException | `4a17c02` | High |
| 9 | Autopilot CPU saturation to 100% | `72f4a1a` | Critical |
| 10 | Contestants stuck at "initializing" | `72f4a1a` | High |
| 11 | Genesis mode no fitness growth | `34f8820` | Critical |
| 12 | Autopilot thread budget too conservative | `cc27d9b` | High |
| 13 | Promoted pool grows without bound | `cd28e7b` | High |
| 14 | Population explosion (118 active) | `8cbde75` | Critical |
| 15 | Settings inconsistency between start modes | `8cbde75` | Medium |
| 16 | Lifespan/grace interaction (120s survivors) | `41b058e` | High |
| 17 | Promoted ghost flag (39 promoted) | `41b058e` | Critical |

---

## Architecture Decisions

### Decision 1: Swing Timer for All Periodic Tasks
**Choice**: `javax.swing.Timer` for both `cullTimer` and `lifespanTimer`
**Rationale**: Ensures all mutations to `contestants` list happen on the EDT, avoiding
concurrent modification. Simple, zero-dependency.
**Trade-off**: EDT blocking can delay timer fires during heavy rendering.

### Decision 2: Gene Array for Meta-GA
**Choice**: `EvolutionConfig.toGeneArray()` / `fromGeneArray()` for parameter breeding
**Rationale**: Treats every tuneable as a float in [min, max] range. Same crossover/mutation
operators work on both the inner GA (triangles) and outer GA (parameters).
**Trade-off**: Some parameters are inherently discrete (population count) but are bred as
continuous floats and clamped.

### Decision 3: Composite Ranking Over Pure Fitness
**Choice**: Weighted blend of fitness, velocity, acceleration, and lineage
**Rationale**: Solves the newcomer problem — new contestants can rank high via velocity
even with low absolute fitness.
**Trade-off**: Weight tuning is subjective. The AUTO strategy mitigates this by shifting
weights over time.

### Decision 4: Merit-Based Promotion (Above Average)
**Choice**: Require above-average score of promoted pool for entry
**Rationale**: Creates a rising quality bar. The pool converges toward the best configs.
**Trade-off**: Early pool fills easily (average is low), later entries are very selective.

---

## Performance Metrics

| Metric | v2.04 | v3.1.0 | Improvement |
|--------|-------|--------|-------------|
| Fitness evaluation | Full image compare | Delta (changed triangles only) | ~50x faster |
| Initialization | Random | Smart + LAP solver | 2-5x better starting fitness |
| Mutation pipeline | Unordered | Targeted → Grid → Close → Random | Better convergence |
| Parameter tuning | Manual only | Auto (meta-GA) | Fully automated |
| Max parallelism | Single-threaded | N threads × M contestants | Full CPU utilization |
| Tournament throughput | N/A | ~1 contestant/min with 60s lifespan + stale/hopeless kill | Continuous |

---

## Files Changed (Key)

| File | Lines | Role |
|------|-------|------|
| `EvolutionaryTournament.java` | ~1200 | Meta-GA core: ranking, breeding, lifecycle |
| `TournamentManagerWindow.java` | ~1900 | Tournament UI, autopilot, presets, settings |
| `TournamentContestant.java` | ~260 | Single evolution run encapsulation |
| `ArtEvolver.java` | ~2200 | Main app, chart, draw-all, contestant display |
| `FitnessTracker.java` | ~240 | Velocity/acceleration/projected fitness |
| `EvolutionConfig.java` | ~300 | Parameter POJO with gene array support |
| `DashboardServer.java` | ~470 | HTTP server + REST API + HTML dashboard |
| `PrehistoricMode.java` | ~350 | Genesis mode with 8 progressive eras |
| `LineageNode.java` | ~200 | Ancestry tree for multi-generational breeding |
| `SystemMonitor.java` | ~100 | JMX-based resource monitoring |
| `ClickerState.java` | ~800 | Evolution Clicker game logic |

---

## Recommendations for v3.2

### Completed in v3.2.0-SNAPSHOT (already on develop)

1. ~~**Multi-stage competitors**~~: **DONE** — Contestants now have configurable "gear" stages
   (Start / Mid / Endgame) with different mutation parameters per stage. Stale detection
   auto-shifts gears. 5 multi-stage preset strategies added (21 total presets).

2. ~~**Replace boolean state flags with enum**~~: Deferred — the ghost-flag bug class was
   mitigated by clearing `promoted=false` in `eliminate()` and adding `!isEliminated()`
   guards. A full enum refactor is low priority given the fixes in place.

### Completed in v3.2.0 (see RELEASE_PLAN_V3.2.md)

3. **Persistent settings** (Feature 3): DONE — `SettingsManager` using `java.util.prefs.Preferences`
   to save/load all parameters across sessions. Auto-save on exit, auto-load on start.

4. **UI coherence pass** (Feature 1): DONE — Evo Settings rewritten as a proper resizable
   `JDialog` with `TitledBorder` sections and normalized 90px spinner widths.

5. **Help system** (Feature 2): DONE — Help menu (Quick Start, Parameter Ref, About,
   Report Issue) added to main ArtEvolver menu bar.

6. **Thumbnail cache** (Feature 4): DONE — Two-tier cache: persistent disk cache
   (`ImageDiskCache`) for resized source images across restarts (critical for 45MP+ cameras),
   plus in-memory dashboard thumbnail cache with HTTP ETag/304 support.

7. **Fitness chart culling** (Feature 5): DONE — Default top-25 series limit with "Top"
   checkbox + spinner. Active contestants always shown.

8. **Auto-open dashboard** (Feature 6): DONE — Browser dashboard opens automatically when
   the tournament starts. Configurable in Evo Settings.

9. **Code review bug fixes**: 6 bugs found and fixed — `autoOpenDashboard` persistence,
   3 missing sidebar preference loads, combo box defaults ignoring saved prefs, window
   position restoration, disk cache image type mismatch, unnecessary `setSourceImage()`
   call on file dialog cancel.

### Deferred to v3.3+

9. **Separate autopilot from tournament**: The autopilot should be a thin resource manager
   that never directly manipulates contestants. All spawning/culling should flow through
   the evolutionary tournament.

10. **Statistical benchmarking**: Automated A/B testing of parameter configurations with
    confidence intervals, not just single-run comparisons.

11. **Full tournament state persistence**: Save/load promoted pool, best-ever config,
    lineage tree, and generation history to resume long-running experiments.

---

## Acknowledgments

This release was developed through intensive pair-programming with continuous user testing.
Every major feature was validated against real evolution runs on a Threadripper 2950x
(16 cores, 128GB RAM). The iterative feedback loop was the single most important factor
in achieving a stable, high-performance system.
