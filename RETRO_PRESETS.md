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
| `snes_512` | 512×448 | 15-bit | 32,768 | Hi-res (pseudo 512) |

---

## Technical Notes

- **SNES 512×448**: Horizontal Pseudo 512 Mode; max resolution. Most games used 256×224.
- **GBC 56 colors** (`gbc_high`): Donkey Kong Country GBC used custom extended palettes; 56 is approximate for "high-end" GBC look.
- **Genesis 64 on-screen**: VDP allows 64 colors simultaneously (4×16 palettes); 512 total in palette RAM.

## High-End References (User-Requested)

| Game | Closest Preset | Notes |
|------|----------------|-------|
| Alone in the Dark | `genesis` or custom 256 | PC 320×200, 256 colors |
| Donkey Kong Country GBC | `gbc_high` | GBC port, 56 colors |
| Cannon Fodder | `genesis` | Amiga/Genesis palette |
| Donkey Kong Country (SNES) | `snes` | 256×224, pre-rendered 3D |

---

## Algorithm Approach Per Preset

| Preset | Pixels | Colors | Copies/Color | Primary Mode | Alt Mode |
|--------|--------|--------|-------------|-------------|---------|
| `gb_dmg` | 23,040 | 4 | ~5,760 | Permutation | — |
| `gb_gray` | 23,040 | 4 | ~5,760 | Permutation | — |
| `gbc` | 23,040 | 32 | ~720 | Permutation | — |
| `gbc_high` | 23,040 | 56 | ~411 | Permutation | — |
| `gba_8` | 38,400 | 8 | 4,800 | Permutation | — |
| `gba_256` | 38,400 | 256 | 150 | Permutation | Palette Evo (256 from 32k) |
| `gba` | 38,400 | 32,768 | ~1.17 | Permutation | — |
| `genesis_64` | 71,680 | 64 | 1,120 | Permutation | Palette Evo (64 from 512) |
| `genesis` | 71,680 | 512 | 140 | Permutation | — |
| `genesis_256` | 57,344 | 512 | 112 | Permutation | — |
| `snes_256` | 57,344 | 256 | 224 | Permutation | Palette Evo (256 from 32k) |
| `snes` | 57,344 | 32,768 | ~1.75 | Permutation | — |
| `snes_512` | 229,376 | 32,768 | ~7 | Permutation | — |

**Key insight**: Presets with sub-selection (Genesis 64-from-512, SNES 256-from-32k)
have two interesting optimization problems — the permutation game AND the palette selection game.

## Implementation Order

1. **gb_dmg** — Simplest (4 colors, 160×144). Validates pixel permutation mode.
2. **gbc** — 32 colors. Same resolution. Validates medium-palette permutation.
3. **genesis** — 512 colors, 320×224. Validates large-palette permutation.
4. **genesis_64** — 64-from-512. First candidate for Palette Evolution mode.
5. **gba** — 240×160, full color.
6. **snes** — 256×224, full color.

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

Genesis: 512 lines. 9-bit RGB: 3 bits per channel → 8 values per channel. Common ramp: R,G,B ∈ {0, 36, 73, 109, 146, 182, 219, 255} (linear) or {0, 32, 64, 96, 128, 160, 192, 224} (alternate). Use VDP-accurate ramp for authenticity.
