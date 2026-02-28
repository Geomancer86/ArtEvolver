package com.rndmodgames.evolver.clicker;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads retro console palettes from resource files or generates them
 * algorithmically (Genesis 9-bit, GBA/SNES 15-bit subsets).
 */
public class PaletteLoader {

    /**
     * Loads a palette by preset. For small palettes, reads from file.
     * For large palettes (Genesis full, GBA, SNES), generates algorithmically.
     */
    public static Color[] load(RetroPreset preset) {
        return switch (preset) {
            case GB_DMG, GB_GRAY, GBC, GBC_HIGH -> loadFromResource(preset.getPaletteResource());
            case GENESIS -> generateGenesis512();
            case GENESIS_64 -> generateGenesisSubset(64);
            case GBA -> generateEvenSubset(256, 32768);
            case SNES_256 -> generateEvenSubset(256, 32768);
            case SNES -> generateGenesis512(); // 512 distinct colors, good for SNES too
        };
    }

    /**
     * Loads palette from palettes/{name}.txt resource.
     */
    public static Color[] loadFromResource(String name) {
        String path = "palettes/" + name + ".txt";
        List<Color> colors = new ArrayList<>();
        try (InputStream is = PaletteLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("[PaletteLoader] Resource not found: " + path);
                return new Color[]{Color.BLACK, Color.WHITE};
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    int r = Integer.parseInt(parts[parts.length - 3]);
                    int g = Integer.parseInt(parts[parts.length - 2]);
                    int b = Integer.parseInt(parts[parts.length - 1]);
                    colors.add(new Color(
                            Math.min(255, Math.max(0, r)),
                            Math.min(255, Math.max(0, g)),
                            Math.min(255, Math.max(0, b))));
                }
            }
        } catch (Exception e) {
            System.err.println("[PaletteLoader] Error loading " + path + ": " + e.getMessage());
            return new Color[]{Color.BLACK, Color.WHITE};
        }
        return colors.toArray(new Color[0]);
    }

    /**
     * Genesis 9-bit: 3 bits per channel, 8 values each = 512 total colors.
     * VDP ramp: 0, 36, 73, 109, 146, 182, 219, 255
     */
    public static Color[] generateGenesis512() {
        int[] ramp = {0, 36, 73, 109, 146, 182, 219, 255};
        Color[] palette = new Color[512];
        int idx = 0;
        for (int r : ramp) {
            for (int g : ramp) {
                for (int b : ramp) {
                    palette[idx++] = new Color(r, g, b);
                }
            }
        }
        return palette;
    }

    /**
     * Genesis subset: pick N evenly-spaced colors from the 512 set.
     */
    public static Color[] generateGenesisSubset(int count) {
        Color[] full = generateGenesis512();
        if (count >= full.length) return full;
        Color[] subset = new Color[count];
        for (int i = 0; i < count; i++) {
            subset[i] = full[(int) ((long) i * full.length / count)];
        }
        return subset;
    }

    /**
     * Even subset from 15-bit RGB space. Picks N colors spread across
     * the color cube for GBA/SNES 256-color modes.
     */
    public static Color[] generateEvenSubset(int count, int totalSpace) {
        int cubeRoot = (int) Math.ceil(Math.cbrt(count));
        List<Color> colors = new ArrayList<>();
        for (int ri = 0; ri < cubeRoot && colors.size() < count; ri++) {
            for (int gi = 0; gi < cubeRoot && colors.size() < count; gi++) {
                for (int bi = 0; bi < cubeRoot && colors.size() < count; bi++) {
                    int r = (ri * 255) / Math.max(1, cubeRoot - 1);
                    int g = (gi * 255) / Math.max(1, cubeRoot - 1);
                    int b = (bi * 255) / Math.max(1, cubeRoot - 1);
                    colors.add(new Color(r, g, b));
                }
            }
        }
        return colors.toArray(new Color[0]);
    }
}
