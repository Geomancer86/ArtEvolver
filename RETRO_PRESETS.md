# Retro Console Presets — Resolution × Palette Matrix

Test combinations for retro mode. Each row is a valid preset to implement.

---

## Game Boy Family

| Preset ID | Resolution | Palette | Colors | Reference Games |
|-----------|------------|---------|--------|-----------------|
| `gb_dmg` | 160×144 | DMG green | 4 | Tetris, Link's Awakening |
| `gb_gray` | 160×144 | Grayscale | 4 | Classic monochrome |
| `gbc` | 160×144 | GBC default | 32 | Pokémon G/S, Zelda Oracles |
| `gbc_high` | 160×144 | GBC extended | 56 | Donkey Kong Country GBC |

---

## Game Boy Advance

| Preset ID | Resolution | Palette | Colors | Reference Games |
|-----------|------------|---------|--------|-----------------|
| `gba` | 240×160 | 15-bit full | 32,768 | Metroid Fusion, Advance Wars |
| `gba_8` | 240×160 | 8-color subset | 8 | Stylized / limited |
| `gba_256` | 240×160 | 256-color subset | 256 | Many GBA games |

---

## Sega Genesis / Mega Drive

| Preset ID | Resolution | Palette | Colors | Reference Games |
|-----------|------------|---------|--------|-----------------|
| `genesis` | 320×224 | Genesis 9-bit | 512 | Sonic, Streets of Rage |
| `genesis_256` | 256×224 | Genesis 9-bit | 512 | Some EU/PAL games |
| `genesis_64` | 320×224 | 64-color subset | 64 | Typical on-screen |

---

## Super Nintendo

| Preset ID | Resolution | Palette | Colors | Reference Games |
|-----------|------------|---------|--------|-----------------|
| `snes` | 256×224 | 15-bit full | 32,768 | DKC, Chrono Trigger |
| `snes_256` | 256×224 | 256-color subset | 256 | Mode 1 typical |
| `snes_512` | 512×448 | 15-bit | 32,768 | Hi-res mode |

---

## High-End References (User-Requested)

| Game | Closest Preset | Notes |
|------|----------------|-------|
| Alone in the Dark | `genesis` or custom 256 | PC 320×200, 256 colors |
| Donkey Kong Country GBC | `gbc_high` | GBC port, 56 colors |
| Cannon Fodder | `genesis` | Amiga/Genesis palette |
| Donkey Kong Country (SNES) | `snes` | 256×224, pre-rendered 3D |

---

## Implementation Order

1. **gb_dmg** — Simplest (4 colors, 160×144). Validates pixel mode.
2. **gbc** — 32 colors. Same resolution.
3. **genesis** — 512 colors, 320×224. Mid-complexity.
4. **gba** — 240×160, full color.
5. **snes** — 256×224, full color.

---

## Palette File Format

Same as existing: `ID ColorName R G B` per line.

Example `gb_dmg.txt`:
```
1 Darkest 15 56 15
2 Dark 48 98 48
3 Light 139 172 15
4 Lightest 155 188 15
```

Genesis: 512 lines, R,G,B ∈ {0,32,64,96,128,160,192,224,255}.
