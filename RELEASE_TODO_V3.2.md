# ArtEvolver v3.2.0 — Pre-Release Audit Report

**Date**: 2026-02-23  
**Purpose**: Deep audit before push to remote and release/tag. Use this to decide: fix before release vs. release with documented TODOs.  
**Status**: All TODOs documented; release pending your explicit authorization.

---

## Executive Summary

| Category | Count | Release-blocking? |
|----------|-------|--------------------|
| **Critical / Blockers** | 1 | Yes (version strings only) |
| **Should fix before release** | 4 | Recommended |
| **Polish / UX** | 6 | No |
| **Technical debt / In-code TODOs** | 15+ | No (documented) |
| **Environment / platform notes** | 2 | No |
| **Deferred (v3.3+)** | 3 | No |

**Completed**: JaCoCo upgraded to 0.8.14 — test suite passes on Java 21 (see §0). Evolution Clicker v4: content & polish pass with masterpiece completion system, 21 upgrades, 8 events, 75+ achievements (see §6).  
**Recommendation**: Fix remaining **Critical** (version strings). Optionally fix **Should fix** and polish items. Then release with this document as the known-TODO appendix.

---

## 0. Completed (No Action Needed)

### 0.1 Test suite on Java 21 (JaCoCo) — DONE

- **What was done**: Upgraded JaCoCo from 0.8.8 to **0.8.14** in root `pom.xml`. Result: `mvn test -pl artevolver-core` passes with coverage.

---

## 1. Critical / Blockers

### 1.1 ~~Test suite (JaCoCo)~~ — see §0.1 (DONE)

- **What** (was): `mvn test -pl artevolver-core` failed with:
  - `Unsupported class file major version 65` (Java 21 bytecode)
  - JaCoCo 0.8.8 does not support Java 21 instrumentation
- **Where**: `artevolver-core/pom.xml` — `jacoco.version` 0.8.8 (parent), agent + report goals
- **Options**:
  - **A)** Upgrade JaCoCo to 0.8.12+ (supports Java 21) in root `pom.xml` and re-run tests.
  - **B)** Run tests without coverage for release: `mvn test -pl artevolver-core -Djacoco.skip=true` (or exclude jacoco in release profile).
- **Recommendation**: Upgrade JaCoCo so the release checklist “Full test suite passes” is satisfied.

### 1.2 Version strings still “v3.1” in launchers and HTML

- **What**: User-facing text still says “v3.1” in several places.
- **Where**:
  - `start.sh` line 6: `ArtEvolver v3.1 - Launcher`
  - `start.bat` line 6: `ArtEvolver v3.1 - Launcher`
  - `benchmark.sh` / `benchmark.bat`: `ArtEvolver v3.1 - Benchmark Runner`
  - `artevolver-core/src/main/resources/dashboard.html` ~579: `ArtEvolver v3.1 — Genetic Art Tournament`
  - `artevolver-core/src/main/resources/clicker.html` ~193: `ArtEvolver v3.1 | ...`
- **Fix**: Global replace “v3.1” → “v3.2” (or “v3.2.0”) in these files. Release checklist already has “Version bumped to 3.2.0 in all POMs” — POMs are 3.2.0-SNAPSHOT; launchers/HTML should match.

---

## 2. Should Fix Before Release

### 2.1 Silent export failure in Renderer

- **What**: Frame export can fail (e.g. disk full, permission) with no user feedback.
- **Where**: `artevolver-core/.../render/Renderer.java` ~56–59:
  - `try { ImageIO.write(...); } catch (IOException e) { }`
- **Fix**: At least log: `System.err.println("[Renderer] Frame export failed: " + e.getMessage());` or show a non-modal toast/label so the user knows exports stopped.

### 2.2 Inconsistent error dialog parent and constant

- **What**: One dialog uses `null` as parent and magic constant `2` for message type.
- **Where**: `ArtEvolver.java` ~1323:
  - `JOptionPane.showMessageDialog(null, "Unable to Load Image", "Fail", 2);`
- **Fix**: Use `this` (or `mainFrame`) as parent and `JOptionPane.ERROR_MESSAGE` instead of `2`. Use title e.g. `"Error"` instead of `"Fail"` for consistency with the rest of the app.

### 2.3 “Start pressed twice” behavior (documented TODO)

- **What**: Comment in code: “this breaks processing if start is pressed twice (or after stopping)”.
- **Where**: `ArtEvolver.java` ~1440–1443, `start()` method.
- **Impact**: Double-clicking Start or Start after Stop may leave evolution in a bad state.
- **Fix**: Guard at top of `start()`: if `isRunning` (or equivalent) already true, return or show a short message and return. Optionally disable Start button while running (if not already).

### 2.4 RELEASE_PLAN checklist and commit count

- **What**: Release plan says “61 commits ahead of origin/develop”; commit count is now higher. Checklist has unchecked items (version bump, release branch, etc.).
- **Fix**: Update “61 commits” to current count (or remove the number). Leave checklist as-is and tick items when done during release.

---

## 3. Polish / UX (Optional Before Release)

### 3.1 Help menu: “No keyboard shortcuts defined”

- **Where**: Help → Keyboard Shortcuts shows “No keyboard shortcuts are currently defined.”
- **Idea**: Either add a few (e.g. Load Image, Start/Stop) or reword to “Keyboard shortcuts are not yet implemented; all actions are available from the sidebar and Tournament Manager.”

### 3.2 Draw Original image: fixed offset 32

- **Where**: `ArtEvolver.java` ~1522: “TODO: make both offsets dynamic to center in JPanel”
- **Impact**: “Draw Original” may not be centered on all window sizes. Low impact.

### 3.3 Dashboard / Clicker HTML footer version

- Covered in 1.2 (version strings).

### 3.4 Main window position on multi-monitor

- **What**: Restored position is applied without checking if the saved coordinates are still on a valid display (e.g. monitor unplugged).
- **Where**: `ArtEvolver.java` initComponents — `setBounds(savedX, savedY, ...)`.
- **Impact**: Window could open off-screen. Optional: use `GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()` to clamp or fall back to center.

### 3.5 Fitness chart: culling label when disabled

- **What**: When culling is off, label is cleared; when on and all visible, “N/M” might still be useful.
- **Current**: Already reasonable; no change required unless you want “N/N” when culling on and all shown.

### 3.6 Evo Settings dialog: no “Reset to defaults”

- **What**: No single action to restore all Evo Settings to built-in defaults.
- **Idea**: Add “Reset to defaults” button that reloads defaults and refreshes the form (v3.3+ is fine).

---

## 4. Technical Debt / In-Code TODOs (Document Only)

These are safe to leave for a post-3.2 cleanup; no need to fix before release.

| Location | TODO / Note |
|----------|--------------|
| `ArtEvolver.java` | Save parameters for “dropdown size select”; document & benchmark; fix random removal of drawings; document & optimize; benchmark threshold export; v1.0 TODOs; “app keeps running on standalone mode”; “move draw original to different button”; “move up with other rendering parameters” |
| `CrossOver.java` | Validate max distance before raising targeted-swap number |
| `TournamentManagerWindow.java` | `IllegalArgumentException` ignored when loading ranking strategy / adaptive lifetime mode (enum from string) — intentional fallback to default |
| `ImageEvolver.java` | Resilient retry in one path (intended) |
| `ArtEvolverManager.java` | “maybe timeout * activeCount”; “Auto-generated catch block” |
| `MainScreen.java` | “Allow all files but show file type not supported”; “error loading palette/exit”; Auto-generated stubs |
| `ProcessRunner.java` | WIP command-line Art Evolver; needs all parameters |
| `ImageComparator.java` | “Auto-generated method stub” |
| `Renderer.java` | Empty catch (see 2.1 — recommend at least logging) |
| `GreedyEvolver.java` | Commented parametrize background color |

None of these are release-blocking; they are product/design follow-ups.

---

## 5. Error Handling and Logging (Summary)

- **Good**: Most failures log to `System.err` or show a dialog (Settings, Image load, Tournament start, Dashboard browser, etc.).
- **Improve**: Renderer frame export (see 2.1) and replacing `e.printStackTrace()` with structured logging would be a nice later pass (v3.3+).
- **Swallowed exceptions**: Only the Renderer empty catch and the intentional enum `IllegalArgumentException` in TournamentManagerWindow; the rest either log or rethrow.

---

## 6. Completed — Evolution Clicker Redesign (v4 — Content & Polish Pass)

- **What was done**: Fourth iteration — massive content and balance pass inspired by the
  game design philosophies of Miyamoto, Meier, Wright, Kojima, Miyazaki, et al.
- **Core mechanic (unchanged)**: 1 click = 1 random two-triangle swap with LIMITED starting
  distance. May succeed (apply, earn EP) or miss. No iterating until success.
- **Design rule: always start from chaos**: Default init is ALWAYS INIT_RANDOM (shuffled
  palette, unordered triangles). Prestige unlocks (Smart/LAP Genesis) are earned rewards
  that improve starting position. ALL progress is measured as fitness GAIN from the
  starting point — never absolute fitness. Prevents ~50K EP windfall from natural palette
  affinity (images start at 50-70% absolute fitness). Code enforced in `getInitMethod()`,
  `checkAchievements()`, and frontend `gainMilestones`.

### 6a. Masterpiece Completion System (new)

- **The big loop**: When fitness reaches 85%+, the player can "Complete Masterpiece" —
  earning GF based on final fitness (`floor(fitness * 50 + completedImages * 5)`),
  incrementing the completed images counter, and performing a full reset. The player
  then loads a new image in ArtEvolver for a fresh canvas.
- **Three-tier progression**: Click → Upgrade → Ascend (medium loop) → Complete Image (big loop).
- **Golden frame effect**: At 80%+ fitness, the evolving image gains a golden glow.
- **Completion celebration overlay**: Stats summary, GF reward, "New Canvas" button.
- **API endpoint**: `POST /api/clicker/complete`.

### 6b. New Upgrades (21 total, was 18)

- **Patience** (Intelligence): After 5+ consecutive misses, next success earns bonus EP.
  Converts frustration into reward — Mikami-style tension/release.
- **Canvas Mastery** (Prestige): +10% EP per completed masterpiece. Cross-image power
  accumulation — the Wright emergent long-term loop.
- **Eternal Reach** (Prestige): Permanent swap reach bonus that persists through ascensions.

### 6c. New Events (8 total, was 6)

- **Focus Mode**: Swap distance halved, 2x retry cycles (20s).
- **Inspiration**: 5x EP from all sources (15s).

### 6d. Gain-Based Fitness Achievements (critical balance fix)

- **Problem**: Images start at 60-70% absolute fitness due to natural palette affinity.
  All fitness achievements based on absolute % triggered immediately, granting ~50K EP
  before the player clicked once.
- **Fix**: All fitness milestones now measure **fitness GAIN from start** (0.1%, 0.5%,
  1%, 2%, 3%, 5%, 7%, 10%, 15%, 20%, 25%, 30%, 40%, 50%, 60%). A player starting at
  65% fitness has 0 achievements and earns everything through swaps.
- **Frontend milestones** also updated to gain-based with gain-specific celebration names.

### 6e. New Achievements (75+)

- **Masterpiece milestones**: First Canvas, Gallery Owner (3), Museum Curator (5), Grand Master (10).
- **GF milestones**: Golden Start (10), Golden Hoard (50), Golden Age (200), Gilded Legend (1000).
- **Hidden discoveries**: Completionist (first masterpiece), Perfectionist (99%+ completion).

### 6f. Balance Pass

- Masterpiece reward halved from `fitness * 100` to `fitness * 50 + bonus` (85% → ~42 GF).
- Ascension count NOT reset on masterpiece (prestige tab stays visible).
- Achievement count properly reset on masterpiece for clean new-image experience.
- Inspiration event (5x EP) is rare (0.05% per check) and short (15s) — impactful but not broken.

- **Files modified**: `ClickerEngine.java`, `ClickerState.java`, `DashboardServer.java`,
  `clicker.html`, `CHANGELOG.md`.
- **Test suite**: Passes (`mvn test -pl artevolver-core`).

---

## 6b. Deferred to v3.3+ (From POSTMORTEM_V3.1)

- Separate autopilot from tournament (thin resource manager).
- Statistical benchmarking (A/B with confidence intervals).
- Full tournament state persistence (save/load promoted pool, lineage, history).

No need to do these for v3.2.

---

## 7. Release Checklist (Current State)

- [x] All 6 features implemented and merged to develop
- [x] Code review pass — 6 bugs fixed
- [x] **Full test suite passes** — JaCoCo 0.8.14 (see §0.1)
- [x] Evolution Clicker v4 (masterpiece system, 21 upgrades, 8 events, 75+ achievements) — see §6
- [ ] Manual testing on primary machine
- [ ] All documentation updated (README, CHANGELOG, ARCHITECTURE)
- [ ] **Version bumped to 3.2.0** in POMs (currently 3.2.0-SNAPSHOT) and **version strings in launchers/HTML** (see 1.2)
- [ ] `release/v3.2.0` branch created from develop
- [ ] Release tag `v3.2.0` created
- [ ] Merged back to develop and master
- [ ] POSTMORTEM_V3.2.md written

---

## 8. Suggested Order of Work

1. **Must-do for release**
   - Fix tests: upgrade JaCoCo (or skip for release) (1.1).
   - Update all “v3.1” → “v3.2” in launchers and HTML (1.2).
2. **Quick wins**
   - Renderer: log on export failure (2.1).
   - ArtEvolver: fix “Fail”, 2 dialog (2.2).
   - Start guard for double-start (2.3).
3. **Then**
   - Version bump to 3.2.0 in POMs when cutting release.
   - Create `release/v3.2.0`, tag, merge, write POSTMORTEM_V3.2.md.

You can **release with this TODO report** as-is after (1); (2) and (3) can be part of the release branch or the first 3.2.x patch.

---

## 9. Environment / Platform Notes (Document Only)

- **Default image directory** (`ArtEvolver.java` ~1148): When no saved “last image dir” exists, the file chooser uses `C:\Media\Art Evolver Stream` if that folder exists, else `user.dir`. This is Windows-specific; on Linux/macOS the path won’t exist and fallback is `user.dir`. Optional improvement: use `user.home` or a configurable default (v3.3+).
- **SystemMonitor** uses `user.dir` for working directory; acceptable for current use.

---

## 10. Documentation Version Labels (No Change Needed)

- **README.md**, **ARCHITECTURE.md**, **BENCHMARKING.md**, **GA_OPTIMIZATION_DESIGN.md**: Feature names like “Delta Fitness Engine (v3.1)” or “Evolutionary Tournament System (v3.1)” mean “introduced in v3.1”. This is intentional versioning; no change required for v3.2 release. Only user-facing launcher/HTML version text (see 1.2) should be updated to v3.2.

---

## 11. Index of All Documented TODOs

| ID | Item | Section |
|----|------|---------|
| 0.1 | JaCoCo / test suite Java 21 | §0 (DONE) |
| 1.2 | Version strings v3.1 → v3.2 in launchers + HTML | §1 |
| 2.1 | Renderer silent export failure | §2 |
| 2.2 | ArtEvolver “Fail”, 2 dialog | §2 |
| 2.3 | Start pressed twice guard | §2 |
| 2.4 | RELEASE_PLAN commit count + checklist | §2 |
| 3.1 | Help keyboard shortcuts text | §3 |
| 3.2 | Draw Original offset 32 | §3 |
| 3.4 | Window position multi-monitor | §3 |
| 3.6 | Evo Settings Reset to defaults | §3 |
| 4 | In-code TODOs (ArtEvolver, CrossOver, MainScreen, etc.) | §4 |
| 6 | Deferred v3.3+ (autopilot, benchmarking, state persistence) | §6 |
| 9 | Default image dir Windows path | §9 |
| 10 | Doc version labels (no change) | §10 |

**When all items above are either DONE or accepted as deferred, the project is ready for release upon your explicit authorization.**
