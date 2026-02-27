# ArtEvolver v3.2.0 — Release Postmortem

**Date**: February 23, 2026
**Version**: v3.1.0 → v3.2.0
**Branch**: `release/v3.2.0` (merged to `master`)
**Tag**: `v3.2.0`
**Status**: Released locally (push when ready)

---

## Executive Summary

ArtEvolver v3.2 is a **polish and usability release** that builds on v3.1's tournament and
Evolution Clicker foundations. It delivers persistent settings, a coherent UI, a help system,
thumbnail and disk caches, and a fully redesigned Evolution Clicker (v4) with 21 upgrades,
8 events, 75+ achievements, Sample Atlas, masterpiece completion, and ascension flows.
All critical and recommended items from the release plan were completed; remaining items
are deferred to v3.3+.

---

## What Was Built

### Infrastructure & Polish (from RELEASE_PLAN_V3.2)
1. **Persistent Settings** — `SettingsManager` via `java.util.prefs.Preferences`; auto-save
   on exit, auto-load on start. Sidebar, Evo Settings, window position, last image dir.
2. **UI Coherence Pass** — Evo Settings as resizable `JDialog` with `TitledBorder` sections,
   normalized spinner/ComboBox widths, consistent visual hierarchy.
3. **Help System** — Help menu (Quick Start, Parameter Reference, Keyboard Shortcuts,
   Report Issue, About). Context-sensitive tooltips on every control.
4. **Thumbnail & Disk Cache** — `ImageDiskCache` for resized source images in
   `~/.artevolver/cache/`; in-memory dashboard cache with HTTP ETag/304.
5. **Fitness Chart Culling** — Default top-25 series limit; active contestants always shown.
6. **Auto-Open Dashboard** — Browser dashboard opens on tournament start; toggle in Evo Settings.

### Evolution Clicker v4 (Content & Polish Pass)
7. **Random Swap Core** — 1 click = 1 random two-triangle swap; no iterating until success.
8. **Always Start From Chaos** — INIT_RANDOM default; gain-based achievements only.
9. **21 Upgrades** — Click Power, Automation, Intelligence, MC, Prestige (GF).
10. **8 Events** — Swap Storm, Golden Hour, Crystal Rain, Focus Mode, Inspiration, etc.
11. **75+ Achievements** — Gain milestones, click/swap/EP/time/streak, masterpiece, GF.
12. **Sample Atlas** — Gamedev-designed tree: Starter → Apprentice → Veteran → Master →
    Legendary (Mona Lisa, Starry Night, etc.) → Secret. OR-logic unlock paths.
13. **Real Sample Images** — `DownloadSampleImages` fetches CC0 from Picsum/Wikimedia.
14. **Per-Sample Stats** — ascensionsCount, totalPlayTimeMs, totalClicks in init picker.
15. **My Images** — Custom uploads saved to `~/.artevolver/clicker-uploads/`; easy re-pick.
16. **Gallery Play Again** — Custom-image cards have "Play again" in standalone mode.
17. **Masterpiece Completion** — 85%+ fitness → Complete → GF reward → New Canvas flow.
18. **Ascension Flow** — Prestige → Gallery snapshot → New image required.
19. **Procedural Sound V2** — Noise-layered, compressed; 13 distinct sounds.

### Bug Fixes (Post-Plan)
20. **Masterpiece Completion Flow** — Completion overlay no longer covered by init overlay;
    polling stops when uninitialized; POST body consumed; standalone uploads cleared.
21. **Ascension Flow** — Same fixes: stop polling, reset init UI, clear uploads.
22. **Reference Image Refresh** — Source preview cache-busts and clears on reset.

---

## What Went Well

### 1. Release Plan Drove Scope
`RELEASE_PLAN_V3.2.md` defined six features with clear acceptance criteria. All were
completed. The "deferred to v3.3+" section kept scope creep under control.

### 2. Evolution Clicker v4 Was User-Validated
Iterative testing caught masterpiece/ascension flow bugs (overlay covering stats, stuck
spinner, 404 image spam, reference image not refreshing). Each fix was documented and
committed; testers approved the release.

### 3. Sample Atlas Design Document
`SAMPLE_ATLAS_DESIGN.md` and `samples/README.md` provided a clear unlock tree and
implementation notes. Real images from Picsum replaced gradient placeholders.

### 4. Documentation Stayed Current
CHANGELOG, RELEASE_TODO, and README were updated throughout. The Evolution Clicker
section was added to README for the release.

---

## What Went Wrong

### 1. Masterpiece/Ascension Flow Had Multiple Failure Modes
The completion overlay was hidden by the init overlay because `refresh()` showed init
when `state.initialized` became false, and init had higher z-index. Image polling continued
after engine clear, causing 404 spam. Reference image cached the previous source.

**Lesson**: When adding "reset and pick new image" flows, enumerate all polling, overlays,
and cached UI state. Stop polling and clear caches before showing the picker.

### 2. POST Body Not Consumed
`handleClickerComplete` and `handleClickerPrestige` did not consume the POST request body.
Some HTTP implementations can hang or misbehave if the body is not read.

**Lesson**: All POST handlers should consume the body (e.g. `ex.getRequestBody().readAllBytes()`)
even when the body is empty or unused.

### 3. Standalone Upload State Persisted Across Resets
After masterpiece/ascension, `uploadedImage` and `uploadedPalette` were not cleared in
standalone mode. Users could accidentally re-init with the same image.

**Lesson**: Explicitly clear server-side state when transitioning to "pick new image" flows.

---

## Bugs Found and Fixed (v3.2 Cycle)

| # | Bug | Fix | Severity |
|---|-----|-----|----------|
| 1 | Completion overlay hidden by init overlay | refresh() skips init when complete shown; z-index 120 | High |
| 2 | Image polling 404 spam after reset | stopPolling() when uninitialized; refreshImage guard | High |
| 3 | Ascension same flow issues | stopPolling, reset init UI, consume POST, clear uploads | High |
| 4 | Reference image shows previous after reset | resetReference(); cache-bust on loadReference | Medium |
| 5 | POST body not consumed (complete/prestige) | consumeRequestBody(ex) | Low |

---

## Architecture Decisions

### Decision 1: Polling Lifecycle
**Choice**: Track `refreshTimer`, `imageTimer`, `achTimer`; call `stopPolling()` when
`state.initialized` is false or on explicit reset (ascend/masterpiece).
**Rationale**: Prevents 404s and stale UI when engine is cleared.
**Trade-off**: Must call `stopPolling()` in every code path that transitions to uninitialized.

### Decision 2: Completion Overlay Above Init
**Choice**: Completion overlay z-index 120; init overlay 100. `refresh()` does not show
init when completion overlay has `.show` class.
**Rationale**: User must see masterpiece stats before picking next image.
**Trade-off**: Slight coupling between refresh logic and overlay visibility.

### Decision 3: My Images in ~/.artevolver
**Choice**: Custom uploads saved to `~/.artevolver/clicker-uploads/{fingerprint}.png` on
first use. Served via `GET /api/clicker/my-image/{fp}`.
**Rationale**: No folder browsing; easy re-pick. Fingerprint ensures deduplication.
**Trade-off**: Disk usage grows with unique uploads; no eviction policy yet.

---

## Release Mechanics

| Step | Status |
|------|--------|
| Version bump 3.2.0-SNAPSHOT → 3.2.0 | Done |
| README badges, Evolution Clicker section | Done |
| CHANGELOG [3.2.0] with date | Done |
| Create release/v3.2.0 from develop | Done |
| Tag v3.2.0 | Done |
| Merge release/v3.2.0 into master | Done |
| POSTMORTEM_V3.2.md | Done |
| Push to remote | Manual (user) |

---

## Recommendations for v3.3+

1. **Evolution Clicker**: Add eviction for My Images (e.g. LRU, max 50 entries).
2. **Help**: Add keyboard shortcuts or reword "No keyboard shortcuts defined".
3. **Tournament**: Separate autopilot from tournament; full state persistence.
4. **Benchmarking**: A/B testing with confidence intervals.
5. **Platform**: Default image dir uses `user.home` on Linux/macOS.

---

## Acknowledgments

Release prepared with iterative testing and documentation updates. Evolution Clicker v4
and the masterpiece/ascension flows were validated by testers before release approval.
