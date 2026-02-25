# ArtEvolver v3.2.0 — Release Report

**Date**: 2026-02-23 (final update 2026-02-25)
**Status**: RELEASE APPROVED by testers. All critical and recommended items resolved.

---

## Executive Summary

| Category | Count | Status |
|----------|-------|--------|
| **Critical / Blockers** | 2 | ALL DONE |
| **Should fix before release** | 4 | ALL DONE |
| **Evolution Clicker** | v4 | DONE — testers approved |
| **Polish / UX** | 6 | Deferred to v3.3+ (non-blocking) |
| **Technical debt / In-code TODOs** | 15+ | Documented (non-blocking) |
| **Environment / platform notes** | 2 | Documented (non-blocking) |
| **Deferred (v3.3+)** | 3 | Documented |

**All critical and recommended items are resolved.** Testers have approved the release.
Remaining items are polish, tech debt, and deferred features — all documented for v3.3+.

---

## 0. Completed — Critical / Blockers

### 0.1 Test suite on Java 21 (JaCoCo) — DONE

- Upgraded JaCoCo from 0.8.8 to **0.8.14** in root `pom.xml`.
- `mvn test -pl artevolver-core` passes with coverage on Java 21.

### 0.2 Version strings v3.1 to v3.2 — DONE

- All "v3.1" replaced with "v3.2" in launchers, HTML, benchmarks, and `ArtEvolver.java` window title.
- Start scripts (`start.sh`, `start.bat`) now use dynamic version from Maven/POM with fallback.

---

## 1. Completed — Should Fix Items

### 1.1 Renderer silent export failure — DONE

- Added `System.err.println("[Renderer] Frame export failed: " + e.getMessage());` in `Renderer.java`.

### 1.2 Inconsistent error dialog parent and constant — DONE

- Changed `JOptionPane.showMessageDialog(null, ..., "Fail", 2)` to `JOptionPane.showMessageDialog(mainFrame, ..., "Error", JOptionPane.ERROR_MESSAGE)` in `ArtEvolver.java`.

### 1.3 "Start pressed twice" guard — DONE

- Added `if (isRunning) { return; }` guard at top of `start()` method in `ArtEvolver.java`.

### 1.4 RELEASE_PLAN checklist and commit count

- Commit count updated. Checklist items ticked as completed during development.

---

## 2. Completed — Evolution Clicker Redesign (v4 — Content & Polish Pass)

Fourth iteration — massive content and balance pass inspired by the game design
philosophies of Miyamoto, Meier, Wright, Kojima, Miyazaki, et al.

### Core Mechanic

- 1 click = 1 random two-triangle swap with LIMITED starting distance.
- May succeed (apply, earn EP) or miss. No iterating until success.

### Design Rule: Always Start From Chaos

- Default init is ALWAYS INIT_RANDOM (shuffled palette, unordered triangles).
- Prestige unlocks (Smart/LAP Genesis) are earned rewards that improve starting position.
- ALL progress is measured as **fitness GAIN from the starting point** — never absolute fitness.
- Prevents ~50K EP windfall from natural palette affinity (images start at 50-70% absolute).
- Code enforced in `getInitMethod()`, `checkAchievements()`, and frontend `gainMilestones`.

### Masterpiece Completion System

- When fitness reaches 85%+, the player can "Complete Masterpiece".
- GF reward: `floor(fitness * 50 + completedImages * 5)`.
- Full reset, then player loads a new image for a fresh canvas.
- Three-tier progression: Click -> Upgrade -> Ascend -> Complete Image.
- Golden frame effect at 80%+ fitness.
- Completion celebration overlay with stats, GF reward, "New Canvas" button.
- API endpoint: `POST /api/clicker/complete`.

### Upgrades (21 total)

- **Click Power**: Retry Cycles, Multi-Swap, Swap Reach, Click Reward.
- **Automation**: Auto-Clicker, Auto Precision, Auto Volume.
- **Intelligence**: Smart Pick, Hot Hand, **Patience** (bonus EP after 5+ misses).
- **Special (MC)**: Critical Swap, Crystal Finder, EP Overflow, Lucky Star, Turbo Auto.
- **Prestige (GF)**: Eternal Cycles, Eternal Speed, Smart Genesis, LAP Genesis,
  **Canvas Mastery** (+10% EP per completed image), **Eternal Reach** (permanent swap reach).

### Events (8 total)

- Swap Storm, Golden Hour, Crystal Rain, Auto Frenzy, Precision Wave, Lucky Streak,
  **Focus Mode** (halved distance, 2x retries), **Inspiration** (5x EP).

### Achievements (75+)

- Gain-based fitness milestones (+0.1% through +60% — never absolute fitness).
- Click, swap, EP, time, upgrade, streak, prestige milestones.
- Masterpiece completion milestones (First Canvas -> Grand Master).
- GF accumulation milestones (Golden Start -> Gilded Legend).
- 8 hidden discoveries (Drought Breaker, Completionist, Perfectionist, etc.).

### Balance

- Masterpiece reward: `fitness * 50 + bonus` (not `* 100`).
- Ascension count preserved through masterpiece (prestige tab stays visible).
- Achievement count properly reset on masterpiece for clean new-image experience.
- Inspiration event (5x EP) rare (0.05% per check) and short (15s).

---

## 3. Deferred to v3.3+ (Non-blocking)

### Polish / UX

| Item | Description |
|------|-------------|
| Help keyboard shortcuts | Shows "No keyboard shortcuts defined" — add shortcuts or reword |
| Draw Original offset | Fixed offset 32 — should be dynamic to center in JPanel |
| Multi-monitor position | Restored window position not validated against active displays |
| Fitness chart culling label | Minor label improvement when all contestants visible |
| Evo Settings reset | No "Reset to defaults" button |

### Technical Debt / In-Code TODOs

| Location | TODO / Note |
|----------|-------------|
| `ArtEvolver.java` | Save parameters for dropdown size select; document & benchmark; fix random removal; v1.0 TODOs |
| `CrossOver.java` | Validate max distance before raising targeted-swap number |
| `TournamentManagerWindow.java` | `IllegalArgumentException` ignored on enum from string (intentional fallback) |
| `ImageEvolver.java` | Resilient retry in one path (intended) |
| `ArtEvolverManager.java` | "maybe timeout * activeCount"; auto-generated catch block |
| `MainScreen.java` | File type filter; error loading palette; auto-generated stubs |
| `ProcessRunner.java` | WIP command-line Art Evolver |
| `ImageComparator.java` | Auto-generated method stub |
| `GreedyEvolver.java` | Commented parametrize background color |

### Deferred Features (from POSTMORTEM_V3.1)

- Separate autopilot from tournament (thin resource manager).
- Statistical benchmarking (A/B with confidence intervals).
- Full tournament state persistence (save/load promoted pool, lineage, history).

### Environment / Platform Notes

- Default image directory uses Windows-specific path `C:\Media\Art Evolver Stream` with fallback to `user.dir`. On Linux/macOS the fallback kicks in. Consider `user.home` in v3.3+.
- `SystemMonitor` uses `user.dir` for working directory; acceptable.

---

## 4. Release Checklist

- [x] All 6 features implemented and merged to develop
- [x] Code review pass — 6 bugs fixed
- [x] **Full test suite passes** — JaCoCo 0.8.14 on Java 21
- [x] Evolution Clicker v4 (masterpiece system, 21 upgrades, 8 events, 75+ achievements)
- [x] Gain-based fitness achievements (prevents 50K EP windfall)
- [x] Version strings updated (dynamic version in start scripts)
- [x] Renderer export logging, dialog fix, start guard
- [x] **Manual testing — testers approved**
- [x] All documentation updated (README, CHANGELOG, RELEASE_TODO)
- [ ] Version bumped to 3.2.0 in POMs (currently 3.2.0-SNAPSHOT)
- [ ] `release/v3.2.0` branch created from develop
- [ ] Release tag `v3.2.0` created
- [ ] Merged back to develop and master
- [ ] POSTMORTEM_V3.2.md written

---

## 5. Documentation Version Labels (No Change Needed)

Feature names like "Delta Fitness Engine (v3.1)" in README, ARCHITECTURE, BENCHMARKING,
and GA_OPTIMIZATION_DESIGN mean "introduced in v3.1". This is intentional versioning;
no change required. Only user-facing launcher/HTML version text was updated to v3.2.

---

**Release is approved. Proceed with version bump, release branch, tag, and merge.**
