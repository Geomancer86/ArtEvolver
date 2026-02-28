package com.rndmodgames.evolver.clicker;

/**
 * Retro console presets: resolution, palette, and color count.
 *
 * Each preset defines a real hardware configuration. The palette resource
 * name maps to a .txt file in src/main/resources/palettes/.
 */
public enum RetroPreset {

    GB_DMG  (160, 144, "gb_dmg",    4,   "Game Boy (DMG)",        "gb"),
    GB_GRAY (160, 144, "gb_gray",   4,   "Game Boy (Gray)",       "gb"),
    GBC     (160, 144, "gbc",       32,  "Game Boy Color",        "gb"),
    GBC_HIGH(160, 144, "gbc_high",  56,  "GBC High Color",        "gb"),
    GBA     (240, 160, "gba",       256, "Game Boy Advance",      "gba"),
    GENESIS_64(320, 224, "genesis_64", 64, "Genesis (64 on-screen)", "genesis"),
    GENESIS (320, 224, "genesis",   512, "Sega Genesis",          "genesis"),
    SNES_256(256, 224, "snes_256",  256, "SNES (Mode 1)",         "snes"),
    SNES    (256, 224, "snes",      512, "Super Nintendo",        "snes");

    private final int width;
    private final int height;
    private final String paletteResource;
    private final int colorCount;
    private final String displayName;
    private final String family;

    RetroPreset(int width, int height, String paletteResource, int colorCount,
                String displayName, String family) {
        this.width = width;
        this.height = height;
        this.paletteResource = paletteResource;
        this.colorCount = colorCount;
        this.displayName = displayName;
        this.family = family;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getTotalPixels() { return width * height; }
    public String getPaletteResource() { return paletteResource; }
    public int getColorCount() { return colorCount; }
    public String getDisplayName() { return displayName; }
    public String getFamily() { return family; }

    public static RetroPreset fromId(String id) {
        if (id == null) return null;
        for (RetroPreset p : values()) {
            if (p.paletteResource.equals(id) || p.name().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    /** Returns all presets belonging to a given family (gb, genesis, gba, snes). */
    public static RetroPreset[] byFamily(String family) {
        java.util.List<RetroPreset> list = new java.util.ArrayList<>();
        for (RetroPreset p : values()) {
            if (p.family.equals(family)) list.add(p);
        }
        return list.toArray(new RetroPreset[0]);
    }
}
