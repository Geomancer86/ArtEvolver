# ArtEvolver v3.2.0 — Release Plan

**Planned Release**: March 2026
**Branch**: `develop` → `release/v3.2.0` (when feature-complete)
**Current Base**: v3.2.0-SNAPSHOT (61 commits ahead of origin/develop)
**Priority**: User-facing polish, persistence, and performance

---

## Executive Summary

v3.2.0 is a **polish and usability release** focused on making ArtEvolver feel professional
and user-friendly. Where v3.1.0 was a transformational feature release (tournament system,
meta-GA, dashboard, clicker), v3.2.0 addresses the rough edges left behind: inconsistent UI
quality, lack of persistent settings, missing help documentation, and performance bottlenecks
in thumbnail generation. Six features are planned, all driven by direct user feedback.

---

## Feature Roadmap

### Feature 1: UI Coherence Pass

**Priority**: HIGH
**ETA**: 3–4 days
**Status**: COMPLETE

#### Problem

Several screens in ArtEvolver have inconsistent quality. The main sidebar and browser
dashboard are polished, but internal dialogs — especially the "Evolutionary Tournament
Settings" (Evo Settings) dialog — are difficult to use:

- Every field uses oversized `JSpinner` controls with no size constraints
- The dialog cannot be resized (uses `JOptionPane.showConfirmDialog()`)
- Sections are cramped vertically with poor visual hierarchy
- No visual grouping or spacing between logical sections
- The 500×660 fixed preferred size doesn't scale to different monitors

#### Solution

1. **Replace `JOptionPane` with a proper `JDialog`** for Evo Settings
   - Resizable, with minimum size constraints
   - Remembers position and size across opens (in-session)
   - Apply / OK / Cancel buttons instead of JOptionPane's generic buttons
2. **Normalize control sizes** across all settings dialogs
   - Spinners: max width 80px for numeric fields, 120px for floats
   - Checkboxes: natural size (no forced stretching)
   - ComboBoxes: max width 200px
   - Use `GridBagLayout` or `MigLayout`-style constraints consistently
3. **Add titled borders with padding** to each section
   - TIMING & POPULATION, ADAPTIVE CUTOFF, CONTESTANT LIFESPAN, etc.
   - 8px internal padding, 12px between sections
4. **Audit all other dialogs** for consistent sizing
   - Quick Setup dialog
   - Autopilot settings dialog
   - Parameter editor (double-click contestant)
5. **Consistent color theme** across all Swing components
   - Match the dark sidebar theme to dialogs where feasible
   - Uniform font sizes for labels vs. values

#### Files Affected

- `TournamentManagerWindow.java` — `showEvoSettings()` method (lines 1326–1547)
- `ArtEvolver.java` — sidebar panel construction
- Potentially new `EvoSettingsDialog.java` extracted class

#### Acceptance Criteria

- [ ] Evo Settings dialog is resizable with sensible min/max bounds
- [ ] All spinners have constrained widths (no field stretching across entire dialog)
- [ ] Sections are visually separated with titled borders
- [ ] Dialog works well on 1080p, 1440p, and 4K displays
- [ ] All other settings dialogs audited and normalized

---

### Feature 2: Help System

**Priority**: MEDIUM
**ETA**: 2–3 days
**Status**: COMPLETE

#### Problem

New users have no guidance on what parameters do, how the tournament system works, or
what the various modes mean. Tooltips exist on some controls (Evo Settings has them) but
the main sidebar has minimal help, and there's no centralized help resource.

#### Solution

1. **Help Menu** in the main `ArtEvolver` JFrame menu bar
   - "About ArtEvolver" — version, license, link to GitHub
   - "Quick Start Guide" — opens a brief HTML help page in the browser
   - "Parameter Reference" — opens parameter docs in the browser
   - "Keyboard Shortcuts" — lists all shortcuts (if any)
   - "Report Issue" — opens GitHub issues page
2. **Context-sensitive tooltips** on all sidebar controls
   - Every spinner, checkbox, combo, and button gets a tooltip
   - Tooltips explain what the parameter does AND suggest good starting values
   - Format: `"Grid Width — Number of triangle columns. Default: 80. Higher = finer detail but slower."`
3. **Help icons (?)** next to complex parameter sections
   - Clicking opens a small popup with 2–3 sentence explanation
   - Sections: Genetic Algorithm, Tournament Mode, Fitness Chart, Dashboard
4. **Evo Settings dialog tooltip audit**
   - Verify all 25+ controls have tooltips (most already do)
   - Add suggested value ranges to each tooltip
5. **Dashboard help panel**
   - Add a "?" icon in the dashboard header that toggles a brief help overlay
   - Explains leaderboard metrics, chart, and status indicators

#### Files Affected

- `ArtEvolver.java` — menu bar creation, tooltip additions
- `TournamentManagerWindow.java` — tooltip audit
- New: `help/quick-start.html` resource file (served by DashboardServer)
- `dashboard.html` — help overlay

#### Acceptance Criteria

- [ ] Help menu exists with at least 4 items
- [ ] Every sidebar control has a descriptive tooltip
- [ ] Evo Settings tooltips include suggested value ranges
- [ ] Dashboard has a toggleable help overlay
- [ ] About dialog shows version, license, and GitHub link

---

### Feature 3: Persistent Settings (Remember Last Used)

**Priority**: HIGH
**ETA**: 2–3 days
**Status**: COMPLETE

#### Problem

Every time ArtEvolver starts, all parameters reset to hardcoded defaults. Users must
reconfigure their preferred grid size, thread count, mutation rates, tournament settings,
etc. on every launch. This is especially painful for the Evo Settings dialog which has
25+ parameters.

#### Solution

1. **`SettingsManager` class** using `java.util.prefs.Preferences`
   - Stores all user-configurable parameters under the `com.rndmodgames.evolver` node
   - `save()` writes all current values; `load()` restores them
   - Fallback to hardcoded defaults when no saved prefs exist
   - No external files needed — Java Preferences API uses the OS registry (Windows)
     or `~/.java/.prefs/` (Linux/Mac)
2. **Auto-save on exit** — `WindowListener.windowClosing()` calls `SettingsManager.save()`
3. **Auto-load on start** — Constructor calls `SettingsManager.load()` before UI init
4. **Parameters to persist** (complete list):
   - **Sidebar**: Grid W/H, palette reps, init method, evolution method, population,
     crossover, iterations, mutation rates (all 6), threads, FPS, toggles
   - **Evo Settings**: All 25+ tournament parameters (cutoff, grace, lifespan,
     adaptive settings, ranking strategy, breeding params)
   - **Window positions**: Main window, Tournament Manager, Fitness Chart
   - **Last loaded image path**: Remember the last file picker directory
   - **Display mode**: Draw Selected / Draw Best / Draw All
   - **Autopilot settings**: CPU/RAM/Heap limits
5. **Reset to Defaults** button in the Help menu or Settings
   - Clears all saved preferences and restores hardcoded defaults
   - Confirms before clearing

#### Files Affected

- New: `SettingsManager.java`
- `ArtEvolver.java` — load on start, save on close
- `TournamentManagerWindow.java` — save/load evo settings
- `EvolutionaryTournament.java` — read settings from SettingsManager

#### Acceptance Criteria

- [ ] All sidebar parameters persist across restarts
- [ ] All Evo Settings parameters persist across restarts
- [ ] Window positions and sizes are remembered
- [ ] Last image directory is remembered
- [ ] "Reset to Defaults" works
- [ ] First launch uses sensible defaults (identical to current behavior)

---

### Feature 4: Thumbnail Cache

**Priority**: MEDIUM-HIGH
**ETA**: 1–2 days
**Status**: COMPLETE

#### Problem

The dashboard generates 300px PNG thumbnails on every `/api/image/{id}` request by
re-rendering the contestant's best image and scaling it. This happens every 2 seconds
per visible contestant (auto-refresh). With 8+ contestants, that's 8+ full image renders
every 2 seconds, consuming significant CPU that could be used for evolution.

#### Solution (Implemented)

**Two-tier cache architecture:**

1. **Persistent disk cache for source images** (`ImageDiskCache`)
   - Resized source images cached as PNGs in `~/.artevolver/cache/`
   - Cache key = SHA-256 of (absolute path + file size + last modified + target dimensions)
   - High-res camera images (Canon EOS R5 45MP, R1 50MP) load near-instantly on repeat
   - Max 200 entries / 500MB with LRU eviction on oldest access time
   - Survives app restarts — once an image is processed, it's always fast

2. **In-memory dashboard thumbnail cache** (`CachedThumbnail` in `DashboardServer`)
   - `ConcurrentHashMap<String, CachedThumbnail>` keyed by contestant ID
   - Invalidated when `bestScore` changes; eliminated contestants cached permanently
   - HTTP `ETag` headers with `304 Not Modified` for browser-side caching
   - Max 100 entries (~5MB)

#### Files Affected

- `ImageDiskCache.java` — new: persistent disk cache for resized source images
- `DashboardServer.java` — in-memory thumbnail cache + ETag support
- `ArtEvolver.java` — disk cache integration in `setSourceImage()` and `loadImage()`

#### Acceptance Criteria

- [x] Resized source images persist across app restarts via disk cache
- [x] Second load of same image with same grid is near-instant (cache hit)
- [x] Dashboard thumbnails only regenerated when contestant's score changes
- [x] HTTP ETag headers prevent redundant network transfers
- [x] Both caches have bounded memory/disk usage with automatic eviction

---

### Feature 5: Fitness Chart Culling

**Priority**: MEDIUM
**ETA**: 1 day
**Status**: COMPLETE

#### Problem

The Fitness Chart renders a series for every contestant that has ever existed in the
tournament. In long-running tournaments with 50+ contestants (most eliminated), the
chart becomes unreadable with overlapping series, a massive legend, and slow rendering.

#### Solution

1. **Default culling to top 25 opponents**
   - New `maxChartSeries` parameter (default: 25)
   - Chart only renders the top N contestants by current fitness score
   - Plus any contestant currently running (active, not eliminated)
   - This means: top 25 by score UNION all currently active = displayed series
2. **Checkbox in Fitness Chart window**: "Limit chart to top N"
   - Default: checked, N = 25
   - Spinner for N (range: 5–100)
   - When unchecked, show all series (current behavior)
3. **Visual indicator** when culling is active
   - Chart title or subtitle shows "Showing top 25 of 87 contestants"
   - Faded note at bottom of legend
4. **Series priority order**
   - Active contestants always shown (regardless of rank)
   - Top N promoted/eliminated by peak score
   - Eliminated contestants below the cutoff are hidden entirely
5. **Dynamic re-evaluation**
   - As scores change, the visible set updates on each repaint
   - Smooth transitions — no flickering when series enter/leave the visible set

#### Files Affected

- `FitnessChartWindow.java` — culling logic, checkbox UI, series filtering
- `ArtEvolver.java` — pass culling preference (or FitnessChartWindow manages internally)

#### Acceptance Criteria

- [ ] Default behavior shows max 25 series plus all active contestants
- [ ] Checkbox and spinner allow user to configure the limit
- [ ] Chart is readable with 50+ total contestants
- [ ] Active (running) contestants are always shown regardless of rank
- [ ] "Showing top N of M" indicator is visible

---

### Feature 6: Auto-Open Dashboard

**Priority**: LOW
**ETA**: 0.5 days
**Status**: COMPLETE

#### Problem

Users must manually click "Dashboard" in the Tournament Manager to open the browser
dashboard. Since the dashboard is the primary monitoring tool, it should open
automatically when the tournament starts.

#### Solution

1. **Auto-open on tournament start**
   - When `DashboardServer` starts (or when the evolutionary tournament begins),
     automatically open the default browser to the dashboard URL
   - Only auto-open once per session (don't re-open if user closes the tab)
2. **Settings toggle**: "Auto-open dashboard on start"
   - Checkbox in Tournament Manager or Evo Settings
   - Default: enabled
   - Persisted via SettingsManager (Feature 3)
3. **Smart detection**
   - If the dashboard is already open (server already running), don't open a new tab
   - Track `dashboardOpened` boolean per session

#### Files Affected

- `DashboardServer.java` — auto-open logic (partially exists: `openBrowser()` method)
- `TournamentManagerWindow.java` — trigger auto-open, settings checkbox
- `SettingsManager.java` — persist the preference

#### Acceptance Criteria

- [x] Dashboard opens in default browser when tournament starts
- [x] Only opens once per session (not on every restart of evolving)
- [x] Checkbox allows disabling the auto-open behavior
- [x] Preference is remembered across sessions (via Feature 3) — *fixed in code review*

---

## Timeline & ETA Summary

| # | Feature | Priority | ETA | Status |
|---|---------|----------|-----|--------|
| 1 | UI Coherence Pass | HIGH | 3–4 days | COMPLETE |
| 2 | Help System | MEDIUM | 2–3 days | COMPLETE |
| 3 | Persistent Settings | HIGH | 2–3 days | COMPLETE |
| 4 | Thumbnail Cache | MEDIUM-HIGH | 1–2 days | COMPLETE |
| 5 | Fitness Chart Culling | MEDIUM | 1 day | COMPLETE |
| 6 | Auto-Open Dashboard | LOW | 0.5 days | COMPLETE |

**All 6 features implemented and merged to `develop`.**
**Code review pass completed**: 6 bugs found and fixed (see below).
**Next step**: Human testing, stabilization, then `release/v3.2.0` branch.  
**Pre-release audit**: See [RELEASE_TODO_V3.2.md](RELEASE_TODO_V3.2.md) for a full list of open TODOs, polish items, and release-blocking fixes (JaCoCo/Java 21, version strings).

---

## Development Workflow (Gitflow)

### Branch Strategy

```
develop (all 6 features merged)
  ├── feature/ui-coherence        ← Feature 1 ✓ merged
  ├── feature/help-system         ← Feature 2 ✓ merged
  ├── feature/persistent-settings ← Feature 3 ✓ merged
  ├── feature/thumbnail-cache     ← Feature 4 ✓ merged
  ├── feature/chart-culling       ← Feature 5 ✓ merged
  └── feature/auto-dashboard      ← Feature 6 ✓ merged
      │
      └──→ develop (all features integrated)
              │
              └──→ release/v3.2.0 (stabilization — next step)
                      │
                      ├──→ master (release tag v3.2.0)
                      └──→ develop (merge back)
```

### Code Review & Bug Fixes (Post-Implementation)

A systematic review of all v3.2.0 code identified **6 bugs**, all fixed on `develop`:

| # | Bug | Severity | File(s) | Fix |
|---|-----|----------|---------|-----|
| 1 | `autoOpenDashboard` not persisted | HIGH | `TournamentManagerWindow.java` | Added save/load in `saveEvoSettings()`/`loadEvoSettings()` |
| 2 | 3 sidebar prefs (evolve method, block crossover, draw mode) saved but never loaded | HIGH | `ArtEvolver.java` | Added loads in `loadSettings()` |
| 3 | Combo boxes used hardcoded defaults, ignoring loaded prefs | HIGH | `ArtEvolver.java` | Use `INITIALIZATION_METHOD` / `savedEvolveMethodIdx` / `tournamentDrawMode` |
| 4 | Window position saved but never restored | MEDIUM | `ArtEvolver.java` | Restore `setBounds()` from saved values in `initComponents()` |
| 5 | Disk cache returns wrong `BufferedImage` type (`TYPE_3BYTE_BGR` vs `TYPE_INT_ARGB`) | MEDIUM | `ArtEvolver.java` | Post-load conversion if type mismatch |
| 6 | `loadImage()` calls `setSourceImage()` when user cancels dialog | LOW | `ArtEvolver.java` | Moved call inside `APPROVE_OPTION` block |

### Release Checklist

- [x] All 6 features implemented and merged to develop
- [x] Code review pass completed — 6 bugs fixed
- [ ] Full test suite passes (`mvn test -pl artevolver-core`)
- [ ] Manual testing on Threadripper 2950x / 128GB (primary test machine)
- [ ] All documentation updated (README, CHANGELOG, ARCHITECTURE)
- [ ] Version bumped to `3.2.0` in all POMs
- [ ] `release/v3.2.0` branch created from develop
- [ ] Release tag `v3.2.0` created
- [ ] Merged back to develop and master
- [ ] POSTMORTEM_V3.2.md written

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| UI changes break existing layouts | Medium | Medium | Test on multiple resolutions |
| Settings persistence introduces regressions | Low | High | Fallback to defaults on any error |
| Thumbnail cache memory issues | Low | Low | Hard cap at 100 entries (~5MB) |
| Help content becomes outdated | Medium | Low | Keep help minimal and link to GitHub docs |
| Feature 1 scope creep (full theme overhaul) | Medium | Medium | Timebox to specific dialogs listed |

---

## Success Criteria

v3.2.0 is successful when:

1. A new user can understand the basic workflow without external documentation
2. Returning users don't need to reconfigure their preferred settings on each launch
3. The Evo Settings dialog is usable without frustration
4. The dashboard runs smoothly with 10+ contestants without CPU spike
5. The Fitness Chart remains readable during long tournament runs
6. The dashboard appears automatically, reducing the "where do I look?" friction

---

## References

- [CHANGELOG.md](CHANGELOG.md) — Version history
- [ARCHITECTURE.md](ARCHITECTURE.md) — System architecture
- [POSTMORTEM_V3.1.md](POSTMORTEM_V3.1.md) — v3.1.0 release retrospective
- [EVOLUTIONARY_TOURNAMENT_V2.md](EVOLUTIONARY_TOURNAMENT_V2.md) — Tournament system design
