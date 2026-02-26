# Evolution Clicker — Upgrade Tree Design

*Design philosophy: Miyamoto (discovery > overwhelming choice), Will Wright (meaningful gates), Sid Meier (meaningful decisions)*

## Overview

Upgrades now unlock progressively through a **tree-based visibility system**. Players discover new options as they progress, rather than being overwhelmed by 24+ choices from the start. Tabs (Click Power, Automation, Intelligence, Special, Prestige) only appear when at least one upgrade in that category is visible.

---

## EP Tree — Evolution Points (main currency)

### Click Power (starter branch)
```
click_ep ────────────────► (always visible)
retry_cycles ────────────► (always visible)
    │
    ├──► swap_reach ─────► (retry_cycles >= 1)
    │         │
    │         └──► multi_swap ──► (swap_reach >= 1)
    │                   │
    │                   └──► deep_focus ─► (multi_swap >= 5)
    │
    └──► smart_pick ────► (retry_cycles >= 3)
              │
              └──► streak_bonus ──► (smart_pick >= 1)
                        │
                        └──► patience ─► (streak_bonus >= 1)
```

### Automation (unlocks mid-early game)
```
auto_clicker ───────────► (totalEpEarned >= 400 OR retry_cycles >= 5)
    │
    └──► auto_cycles ───► (auto_clicker >= 1)
              │
              └──► auto_multi ──► (auto_cycles >= 1)
                        │
                        └──► idle_mastery ─► (auto_multi >= 3)
```

---

## MC Tree — Mutation Crystals (rare, from events)

*MC tab appears when totalEpEarned >= 2000 OR mc >= 1*

```
critical_swap ──────────► (first tier, always visible once MC gate passed)
mc_finder ──────────────► (first tier)
    │
    ├──► ep_multiplier ──► (critical_swap >= 1 OR mc_finder >= 1)
    │         │
    └──► lucky_events ──► (same)
              │
              └──► auto_boost ─► (ep_multiplier >= 1 OR lucky_events >= 1)
```

---

## GF Tree — Golden Fractals (prestige currency)

*Prestige tab appears only after first ascension (ascensionCount >= 1)*

```
eternal_cycles ─────────► (entry tier)
eternal_auto ───────────► (entry tier)
eternal_reach ──────────► (entry tier)
    │
    └──► smart_init ────► (any eternal_* >= 1)
              │
              └──► lap_init ──► (smart_init >= 1)

canvas_mastery ─────────► (completedImages >= 1 — first masterpiece done)
    │
    └──► golden_touch ──► (canvas_mastery >= 1)
```

---

## Design Principles

1. **Progressive disclosure**: Start with 2–4 choices. Unlock more as the player invests.
2. **Meaningful gates**: Each unlock teaches something (e.g. "retry cycles matter" before "swap reach").
3. **Parallel paths**: Click Power, Intelligence, and Automation can progress in parallel once automation unlocks.
4. **Prestige as aspiration**: GF tab hidden until first ascend — creates a clear "what's next" moment.
5. **Masterpiece rewards**: Canvas Mastery and Golden Touch only appear after completing at least one image — mastery begets mastery.

---

## Implemented (v3.2)

- **Chain Lightning** (MC): Critical hits 15% chance to chain. Requires auto_boost >= 3.
- **Resonance** (MC): MC finder scales with EP Overflow level. Requires both.
- **Eternal Fortune** (GF): +3% EP per ascension per level. Requires eternal_auto >= 5.
- **Genesis Boost** (GF): +5% EP from fitness gains per level. Requires lap_init >= 1.

---

## Implementation

- `ClickerState.isUpgradeVisible(UpgradeDef)` implements the tree.
- Frontend filters `u.visible` and rebuilds tabs from visible categories only.
- No data format changes — tree is logic-only in the backend.
