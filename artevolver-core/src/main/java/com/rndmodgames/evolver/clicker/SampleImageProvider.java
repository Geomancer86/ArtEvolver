package com.rndmodgames.evolver.clicker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides sample images for the Evolution Clicker.
 * Miyamoto: discovery and collection. Wright: meaningful unlock progression.
 *
 * <p><b>Default tree (25 samples):</b> Real-life photos from open-source/free sources.
 * Images load from {@code samples/{id}.jpg} or {@code samples/{id}.png}. Replace
 * placeholders with curated CC0 images that achieve 75–80%+ fitness. See samples/README.md.
 *
 * <p><b>Generated samples (25):</b> Programmatic fallbacks kept for testing. Always pickable
 * even after completion — samples never go into usedImageFingerprints.
 */
public final class SampleImageProvider {

    public static final int WIDTH = 720;
    public static final int HEIGHT = 468;

    /**
     * Optional alt unlock: if non-empty, sample unlocks when primary OR alt is met.
     * recommendedPreset: if non-null, the sample's "home" console preset id (e.g. "gbc").
     */
    public record SampleDef(String id, String name, String description,
                           String unlockType, double unlockValue,
                           String unlockTypeAlt, double unlockValueAlt,
                           String category, boolean secret, int sortOrder,
                           String recommendedPreset) {
        public SampleDef(String id, String name, String desc, String type, double val,
                         String typeAlt, double valAlt, String cat, boolean secret, int order) {
            this(id, name, desc, type, val, typeAlt, valAlt, cat, secret, order, null);
        }
        public SampleDef(String id, String name, String desc, String type, double val,
                         String cat, boolean secret, int order) {
            this(id, name, desc, type, val, "", -1, cat, secret, order, null);
        }
    }

    private static final Set<String> PROGRAMMATIC_IDS = Set.of(
            "gradient_sunset", "circles", "checker", "stripes", "diamond",
            "spiral", "rings", "grid_grad", "waves", "maze",
            "starfield", "hexagons", "voronoi", "noise_cloud", "mandala",
            "rose", "fractal_tree", "kaleidoscope", "aurora", "portal",
            "easter_egg", "hidden_gem", "legendary", "mythic", "omega",
            "retro_gb_castle", "retro_gb_hills",
            "retro_gbc_town", "retro_gbc_underwater",
            "retro_gen_skyline", "retro_gen_ruins",
            "retro_gba_forest", "retro_gba_beach",
            "retro_snes_mountains", "retro_snes_village"
    );

    private static final List<SampleDef> DEFS = new ArrayList<>();

    static {
        // ─── GAMEDEV TREE: Miyamoto (discovery), Wright (paths), Kojima (secrets), Sid Meier (meaningful gates) ───
        // STARTER — always available
        DEFS.add(new SampleDef("landscape", "Landscape", "Open vista.",
                "always", 0, "Starter", false, 1));
        DEFS.add(new SampleDef("ocean", "Ocean", "Seascape.",
                "always", 0, "Starter", false, 2));
        DEFS.add(new SampleDef("forest", "Forest", "Nature.",
                "always", 0, "Starter", false, 3));
        DEFS.add(new SampleDef("sunset", "Sunset", "Golden hour.",
                "always", 0, "Starter", false, 4));
        DEFS.add(new SampleDef("flowers", "Flowers", "Floral.",
                "always", 0, "Starter", false, 5));

        // APPRENTICE — Path A: EP | Path B: Clicks (either unlocks)
        DEFS.add(new SampleDef("mountain", "Mountain", "Peak vista.", "ep", 500, "clicks", 1000, "Apprentice", false, 10));
        DEFS.add(new SampleDef("canyon", "Canyon", "Desert canyon.", "ep", 2000, "clicks", 5000, "Apprentice", false, 11));
        DEFS.add(new SampleDef("lake", "Lake", "Reflection.", "ep", 5000, "clicks", 10000, "Apprentice", false, 12));
        DEFS.add(new SampleDef("meadow", "Meadow", "Green field.", "ep", 10000, "clicks", 20000, "Apprentice", false, 13));
        DEFS.add(new SampleDef("beach", "Beach", "Shoreline.", "ep", 20000, "clicks", 50000, "Apprentice", false, 14));

        // VETERAN — Path A: Ascensions | Path B: Masterpieces
        DEFS.add(new SampleDef("cityscape", "Cityscape", "Urban skyline.", "ascensions", 1, "masterpieces", 1, "Veteran", false, 20));
        DEFS.add(new SampleDef("architecture", "Architecture", "Structure.", "ascensions", 2, "masterpieces", 2, "Veteran", false, 21));
        DEFS.add(new SampleDef("portrait", "Portrait", "Face study.", "ascensions", 3, "masterpieces", 2, "Veteran", false, 22));
        DEFS.add(new SampleDef("wildlife", "Wildlife", "Animal.", "ascensions", 5, "masterpieces", 3, "Veteran", false, 23));
        DEFS.add(new SampleDef("night", "Night", "Nocturne.", "ascensions", 7, "masterpieces", 5, "Veteran", false, 24));

        // MASTER — Multiple paths; build toward Legendary
        DEFS.add(new SampleDef("abstract", "Abstract", "Non-representational.", "ascensions", 5, "masterpieces", 3, "Master", false, 30));
        DEFS.add(new SampleDef("macro", "Macro", "Close-up.", "ascensions", 7, "ep", 30000, "Master", false, 31));
        DEFS.add(new SampleDef("aerial", "Aerial", "Drone view.", "clicks", 30000, "masterpieces", 5, "Master", false, 32));
        DEFS.add(new SampleDef("street", "Street", "Candid.", "clicks", 50000, "ascensions", 5, "Master", false, 33));
        DEFS.add(new SampleDef("still_life", "Still Life", "Composition.", "ep", 40000, "masterpieces", 7, "Master", false, 34));

        // LEGENDARY — Iconic art. Public domain. Crown jewels.
        DEFS.add(new SampleDef("mona_lisa", "Mona Lisa", "Leonardo da Vinci, c.1503. Public domain.",
                "masterpieces", 5, "ascensions", 10, "Legendary", false, 40));
        DEFS.add(new SampleDef("starry_night", "Starry Night", "Van Gogh, 1889. Public domain.",
                "masterpieces", 3, "ep", 50000, "Legendary", false, 41));
        DEFS.add(new SampleDef("great_wave", "The Great Wave", "Hokusai, c.1831. Public domain.",
                "ascensions", 5, "clicks", 75000, "Legendary", false, 42));
        DEFS.add(new SampleDef("girl_pearl_earring", "Girl with a Pearl Earring", "Vermeer, c.1665. Public domain.",
                "masterpieces", 7, "ascensions", 7, "Legendary", false, 43));
        DEFS.add(new SampleDef("birth_of_venus", "Birth of Venus", "Botticelli, c.1485. Public domain.",
                "gf", 50, "masterpieces", 10, "Legendary", false, 44));
        DEFS.add(new SampleDef("american_gothic", "American Gothic", "Grant Wood, 1930. Public domain.",
                "ascensions", 10, "ep", 75000, "Legendary", false, 45));
        DEFS.add(new SampleDef("persistence_of_memory", "Persistence of Memory", "Dalí, 1931. Surrealist icon.",
                "gf", 100, "masterpieces", 10, "Legendary", false, 46));

        // SECRET — Kojima: subversion, suffering, discovery
        DEFS.add(new SampleDef("secret_suffer", "???", "You endured.",
                "miss_streak", 20, "clicks", 100000, "Secret", true, 50));
        DEFS.add(new SampleDef("secret_grind", "???", "Dedication.",
                "clicks", 100000, "ep", 100000, "Secret", true, 51));
        DEFS.add(new SampleDef("secret_ascended", "???", "Returned.",
                "ascensions", 15, "masterpieces", 15, "Secret", true, 52));
        DEFS.add(new SampleDef("secret_gilded", "???", "Golden.",
                "gf", 200, "", -1, "Secret", true, 53));
        DEFS.add(new SampleDef("secret_omega", "???", "The final canvas.",
                "gf", 500, "masterpieces", 20, "Secret", true, 54));

        // ─── GENERATED: programmatic samples, always available for testing. ───
        DEFS.add(new SampleDef("gradient_sunset", "Sunset Gradient", "A warm gradient from orange to purple.",
                "always", 0, "Generated", false, 60));
        DEFS.add(new SampleDef("circles", "Concentric Circles", "Nested circles for clean evolution.",
                "always", 0, "Generated", false, 61));
        DEFS.add(new SampleDef("checker", "Checkerboard", "Classic black and white pattern.",
                "always", 0, "Generated", false, 62));
        DEFS.add(new SampleDef("stripes", "Rainbow Stripes", "Horizontal color bands.",
                "always", 0, "Generated", false, 63));
        DEFS.add(new SampleDef("diamond", "Diamond Shape", "A central diamond on gradient.",
                "always", 0, "Generated", false, 64));
        DEFS.add(new SampleDef("spiral", "Spiral", "Archimedean spiral pattern.",
                "always", 0, "Generated", false, 65));
        DEFS.add(new SampleDef("rings", "Ripple Rings", "Expanding rings from center.",
                "always", 0, "Generated", false, 66));
        DEFS.add(new SampleDef("grid_grad", "Grid Gradient", "Mesh with smooth gradients.",
                "always", 0, "Generated", false, 67));
        DEFS.add(new SampleDef("waves", "Sine Waves", "Overlapping wave patterns.",
                "always", 0, "Generated", false, 68));
        DEFS.add(new SampleDef("maze", "Mini Maze", "A simple labyrinth pattern.",
                "always", 0, "Generated", false, 69));
        DEFS.add(new SampleDef("starfield", "Starfield", "Scattered points like stars.",
                "always", 0, "Generated", false, 70));
        DEFS.add(new SampleDef("hexagons", "Honeycomb", "Hexagonal tessellation.",
                "always", 0, "Generated", false, 71));
        DEFS.add(new SampleDef("voronoi", "Voronoi", "Cell-like subdivision.",
                "always", 0, "Generated", false, 72));
        DEFS.add(new SampleDef("noise_cloud", "Cloud Noise", "Soft perlin-like texture.",
                "always", 0, "Generated", false, 73));
        DEFS.add(new SampleDef("mandala", "Mandala", "Radial symmetry pattern.",
                "always", 0, "Generated", false, 74));
        DEFS.add(new SampleDef("rose", "Rose Curve", "Mathematical rose pattern.",
                "always", 0, "Generated", false, 75));
        DEFS.add(new SampleDef("fractal_tree", "Fractal Tree", "Recursive branch structure.",
                "always", 0, "Generated", false, 76));
        DEFS.add(new SampleDef("kaleidoscope", "Kaleidoscope", "Multi-fold symmetry.",
                "always", 0, "Generated", false, 77));
        DEFS.add(new SampleDef("aurora", "Aurora", "Northern lights simulation.",
                "always", 0, "Generated", false, 78));
        DEFS.add(new SampleDef("portal", "Portal", "Swirling vortex effect.",
                "always", 0, "Generated", false, 79));
        DEFS.add(new SampleDef("easter_egg", "???", "You found something.",
                "always", 0, "Generated", true, 80));
        DEFS.add(new SampleDef("hidden_gem", "???", "Something stirs.",
                "always", 0, "Generated", true, 81));
        DEFS.add(new SampleDef("legendary", "???", "A legend awakens.",
                "always", 0, "Generated", true, 82));
        DEFS.add(new SampleDef("mythic", "???", "Beyond the veil.",
                "always", 0, "Generated", true, 83));
        DEFS.add(new SampleDef("omega", "???", "The final canvas.",
                "always", 0, "Generated", true, 84));

        // ─── RETRO: console-specific samples generated at native resolution ───
        DEFS.add(new SampleDef("retro_gb_castle", "GB Castle", "A castle silhouette in four shades of green.",
                "always", 0, "", -1, "Retro", false, 90, "gb_dmg"));
        DEFS.add(new SampleDef("retro_gb_hills", "GB Hills", "Rolling hills under a pale sky.",
                "always", 0, "", -1, "Retro", false, 91, "gb_gray"));
        DEFS.add(new SampleDef("retro_gbc_town", "GBC Town", "A colorful pixel town with houses and trees.",
                "always", 0, "", -1, "Retro", false, 92, "gbc"));
        DEFS.add(new SampleDef("retro_gbc_underwater", "GBC Underwater", "Deep sea scene with coral and fish.",
                "always", 0, "", -1, "Retro", false, 93, "gbc"));
        DEFS.add(new SampleDef("retro_gen_skyline", "Genesis Skyline", "City skyline at sunset with rich gradients.",
                "always", 0, "", -1, "Retro", false, 94, "genesis_64"));
        DEFS.add(new SampleDef("retro_gen_ruins", "Genesis Ruins", "Ancient ruins with parallax layers.",
                "always", 0, "", -1, "Retro", false, 95, "genesis"));
        DEFS.add(new SampleDef("retro_gba_forest", "GBA Forest", "Dense forest with depth and atmosphere.",
                "always", 0, "", -1, "Retro", false, 96, "gba"));
        DEFS.add(new SampleDef("retro_gba_beach", "GBA Beach", "Tropical shoreline with turquoise water.",
                "always", 0, "", -1, "Retro", false, 97, "gba"));
        DEFS.add(new SampleDef("retro_snes_mountains", "SNES Mountains", "Mountain range with atmospheric haze.",
                "always", 0, "", -1, "Retro", false, 98, "snes_256"));
        DEFS.add(new SampleDef("retro_snes_village", "SNES Village", "Warm village scene at golden hour.",
                "always", 0, "", -1, "Retro", false, 99, "snes"));
    }

    public static List<SampleDef> getAllDefs() {
        return List.copyOf(DEFS);
    }

    public static SampleDef getDef(String id) {
        for (SampleDef d : DEFS) {
            if (d.id.equals(id)) return d;
        }
        return null;
    }

    /**
     * Try loading from resources: samples/{id}.jpg or samples/{id}.png.
     * Returns null if not found.
     */
    private static BufferedImage loadFromResources(String id) {
        ClassLoader cl = SampleImageProvider.class.getClassLoader();
        for (String ext : new String[]{"jpg", "jpeg", "png"}) {
            String path = "samples/" + id + "." + ext;
            try (InputStream is = cl.getResourceAsStream(path)) {
                if (is != null) {
                    BufferedImage img = ImageIO.read(is);
                    if (img != null && (img.getWidth() != WIDTH || img.getHeight() != HEIGHT)) {
                        BufferedImage resized = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = resized.createGraphics();
                        g.drawImage(img, 0, 0, WIDTH, HEIGHT, null);
                        g.dispose();
                        return resized;
                    }
                    return img;
                }
            } catch (IOException ignored) { /* try next */ }
        }
        return null;
    }

    /** Placeholder for real-photo ids when no file exists yet. */
    private static BufferedImage placeholderPhoto() {
        return gradientSunset();
    }

    public static BufferedImage generate(String id) {
        BufferedImage fromFile = loadFromResources(id);
        if (fromFile != null) return fromFile;
        if (PROGRAMMATIC_IDS.contains(id)) {
            return switch (id) {
                case "gradient_sunset" -> gradientSunset();
                case "circles" -> circles();
                case "checker" -> checker();
                case "stripes" -> stripes();
                case "diamond" -> diamond();
                case "spiral" -> spiral();
                case "rings" -> rings();
                case "grid_grad" -> gridGrad();
                case "waves" -> waves();
                case "maze" -> maze();
                case "starfield" -> starfield();
                case "hexagons" -> hexagons();
                case "voronoi" -> voronoi();
                case "noise_cloud" -> noiseCloud();
                case "mandala" -> mandala();
                case "rose" -> rose();
                case "fractal_tree" -> fractalTree();
                case "kaleidoscope" -> kaleidoscope();
                case "aurora" -> aurora();
                case "portal" -> portal();
                case "easter_egg" -> easterEgg();
                case "hidden_gem" -> hiddenGem();
                case "legendary" -> legendary();
                case "mythic" -> mythic();
                case "omega" -> omega();
                case "retro_gb_castle" -> retroGbCastle();
                case "retro_gb_hills" -> retroGbHills();
                case "retro_gbc_town" -> retroGbcTown();
                case "retro_gbc_underwater" -> retroGbcUnderwater();
                case "retro_gen_skyline" -> retroGenSkyline();
                case "retro_gen_ruins" -> retroGenRuins();
                case "retro_gba_forest" -> retroGbaForest();
                case "retro_gba_beach" -> retroGbaBeach();
                case "retro_snes_mountains" -> retroSnesMountains();
                case "retro_snes_village" -> retroSnesVillage();
                default -> gradientSunset();
            };
        }
        return placeholderPhoto();
    }

    private static BufferedImage create(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private static BufferedImage gradientSunset() {
        BufferedImage img = create(WIDTH, HEIGHT);
        for (int y = 0; y < HEIGHT; y++) {
            double t = (double) y / HEIGHT;
            int r = (int) (255 * (1 - t));
            int g = (int) (100 * (1 - t) + 50 * t);
            int b = (int) (150 + 100 * t);
            for (int x = 0; x < WIDTH; x++) {
                img.setRGB(x, y, (255 << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    private static BufferedImage circles() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(30, 30, 50));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        int cx = WIDTH / 2, cy = HEIGHT / 2;
        for (int i = 12; i >= 0; i--) {
            float hue = (i * 25) % 360 / 360f;
            g.setColor(Color.getHSBColor(hue, 0.6f, 0.9f));
            g.fillOval(cx - i * 25, cy - i * 25, i * 50, i * 50);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage checker() {
        BufferedImage img = create(WIDTH, HEIGHT);
        int size = 40;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                boolean white = ((x / size) + (y / size)) % 2 == 0;
                img.setRGB(x, y, white ? 0xFFFFFFFF : 0xFF1a1a2e);
            }
        }
        return img;
    }

    private static BufferedImage stripes() {
        BufferedImage img = create(WIDTH, HEIGHT);
        int[] colors = { 0xFFFF6B6B, 0xFFFFA94D, 0xFFFFE066, 0xFF69DB7C, 0xFF74C0FC, 0xFF9775FA };
        int h = HEIGHT / colors.length;
        for (int i = 0; i < colors.length; i++) {
            for (int y = i * h; y < Math.min((i + 1) * h, HEIGHT); y++) {
                for (int x = 0; x < WIDTH; x++) {
                    img.setRGB(x, y, 0xFF000000 | colors[i]);
                }
            }
        }
        return img;
    }

    private static BufferedImage diamond() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(50, 30, 80));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        int[] xp = { WIDTH / 2, WIDTH - 80, WIDTH / 2, 80 };
        int[] yp = { 40, HEIGHT / 2, HEIGHT - 40, HEIGHT / 2 };
        g.setColor(new Color(220, 180, 100));
        g.fillPolygon(xp, yp, 4);
        g.dispose();
        return img;
    }

    private static BufferedImage spiral() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(15, 15, 25));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        int cx = WIDTH / 2, cy = HEIGHT / 2;
        for (int i = 0; i < 500; i++) {
            double t = i * 0.04;
            int x = cx + (int) (t * 15 * Math.cos(t));
            int y = cy + (int) (t * 15 * Math.sin(t));
            float hue = (float) (t * 0.05 % 1);
            g.setColor(Color.getHSBColor(hue, 0.8f, 1f));
            g.fillOval(x - 4, y - 4, 8, 8);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage rings() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(20, 25, 40));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        int cx = WIDTH / 2, cy = HEIGHT / 2;
        for (int r = 200; r > 0; r -= 12) {
            float hue = (r % 60) / 60f;
            g.setColor(Color.getHSBColor(hue, 0.5f, 0.9f));
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage gridGrad() {
        BufferedImage img = create(WIDTH, HEIGHT);
        int step = 48;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double tx = (double) x / WIDTH;
                double ty = (double) y / HEIGHT;
                int r = (int) (50 + 150 * tx);
                int g = (int) (80 + 100 * ty);
                int b = (int) (120 + 80 * (1 - tx) * ty);
                if ((x / step + y / step) % 2 == 0) {
                    r = Math.min(255, r + 30);
                    g = Math.min(255, g + 30);
                    b = Math.min(255, b + 30);
                }
                img.setRGB(x, y, (255 << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    private static BufferedImage waves() {
        BufferedImage img = create(WIDTH, HEIGHT);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double v = Math.sin(x * 0.02) * 0.3 + Math.sin(y * 0.03) * 0.3
                        + Math.sin((x + y) * 0.015) * 0.2;
                v = (v + 0.8) / 1.6;
                int gray = (int) (100 + 155 * v);
                img.setRGB(x, y, (255 << 24) | (gray << 16) | (gray << 8) | (gray + 20));
            }
        }
        return img;
    }

    private static BufferedImage maze() {
        BufferedImage img = create(WIDTH, HEIGHT);
        int cell = 24;
        int cols = WIDTH / cell, rows = HEIGHT / cell;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int cx = x / cell, cy = y / cell;
                boolean wall = (cx + cy) % 3 == 0 || (cx * 7 + cy * 11) % 5 == 0;
                img.setRGB(x, y, wall ? 0xFF2a2a40 : 0xFFe8e8f0);
            }
        }
        return img;
    }

    private static BufferedImage starfield() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(5, 5, 15));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        for (int i = 0; i < 300; i++) {
            int x = (int) (Math.random() * WIDTH);
            int y = (int) (Math.random() * HEIGHT);
            int bright = 150 + (int) (Math.random() * 105);
            g.setColor(new Color(bright, bright, bright + 30));
            int sz = 1 + (int) (Math.random() * 2);
            g.fillOval(x, y, sz, sz);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage hexagons() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(25, 30, 45));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        int r = 30;
        for (int row = -1; row < HEIGHT / (r * 1.5) + 2; row++) {
            for (int col = -1; col < WIDTH / (r * 1.73) + 2; col++) {
                double cx = col * r * 1.73 + (row % 2 == 0 ? 0 : r * 0.865);
                double cy = row * r * 1.5;
                Polygon p = new Polygon();
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    p.addPoint((int) (cx + r * Math.cos(a)), (int) (cy + r * Math.sin(a)));
                }
                float hue = (float) ((col + row) % 12 / 12.0);
                g.setColor(Color.getHSBColor(hue, 0.4f, 0.7f));
                g.fillPolygon(p);
            }
        }
        g.dispose();
        return img;
    }

    private static BufferedImage voronoi() {
        BufferedImage img = create(WIDTH, HEIGHT);
        int n = 40;
        int[] px = new int[n], py = new int[n];
        int[] colors = new int[n];
        for (int i = 0; i < n; i++) {
            px[i] = (int) (Math.random() * WIDTH);
            py[i] = (int) (Math.random() * HEIGHT);
            colors[i] = 0xFF000000 | ((int) (Math.random() * 256) << 16)
                    | ((int) (Math.random() * 256) << 8) | (int) (Math.random() * 256);
        }
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int best = 0;
                double bestD = Double.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    double d = (x - px[i]) * (x - px[i]) + (y - py[i]) * (y - py[i]);
                    if (d < bestD) { bestD = d; best = i; }
                }
                img.setRGB(x, y, colors[best]);
            }
        }
        return img;
    }

    private static BufferedImage noiseCloud() {
        BufferedImage img = create(WIDTH, HEIGHT);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double n = Math.sin(x * 0.02 + 1) * Math.sin(y * 0.02 + 2) * 0.5
                        + Math.sin((x + y) * 0.015) * 0.5;
                n = (n + 1) / 2;
                int v = (int) (80 + 120 * n);
                int b = (int) (v + 30);
                img.setRGB(x, y, (255 << 24) | (v << 16) | (v << 8) | Math.min(255, b));
            }
        }
        return img;
    }

    private static BufferedImage mandala() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(20, 15, 30));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        int cx = WIDTH / 2, cy = HEIGHT / 2;
        int petals = 12;
        for (int p = 0; p < petals; p++) {
            double a0 = p * 2 * Math.PI / petals;
            int[] xp = new int[4];
            int[] yp = new int[4];
            for (int i = 0; i < 4; i++) {
                double a = a0 + i * Math.PI / 6;
                xp[i] = cx + (int) (180 * Math.cos(a));
                yp[i] = cy + (int) (180 * Math.sin(a));
            }
            g.setColor(Color.getHSBColor((float) (p / (float) petals), 0.7f, 0.9f));
            g.fillPolygon(xp, yp, 4);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage rose() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(25, 20, 35));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        int cx = WIDTH / 2, cy = HEIGHT / 2;
        for (int i = 0; i < 2000; i++) {
            double t = i * 0.01;
            double r = 150 * Math.sin(5 * t);
            int x = cx + (int) (r * Math.cos(t));
            int y = cy + (int) (r * Math.sin(t));
            g.setColor(Color.getHSBColor((float) (t / (2 * Math.PI) % 1), 0.8f, 1f));
            g.fillOval(x - 2, y - 2, 4, 4);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage fractalTree() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(15, 20, 25));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        drawBranch(g, WIDTH / 2, HEIGHT - 20, 0, 80, 0.7, 0);
        g.dispose();
        return img;
    }

    private static void drawBranch(Graphics2D g, int x, int y, double angle, double len, double scale, int depth) {
        if (len < 2 || depth > 10) return;
        int x2 = x + (int) (len * Math.sin(angle));
        int y2 = y - (int) (len * Math.cos(angle));
        g.setColor(new Color(100, 80 + depth * 15, 60));
        g.drawLine(x, y, x2, y2);
        drawBranch(g, x2, y2, angle - 0.5, len * scale, scale, depth + 1);
        drawBranch(g, x2, y2, angle + 0.5, len * scale, scale, depth + 1);
    }

    private static BufferedImage kaleidoscope() {
        BufferedImage img = create(WIDTH, HEIGHT);
        int folds = 8;
        int cx = WIDTH / 2, cy = HEIGHT / 2;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double dx = x - cx, dy = y - cy;
                double a = Math.atan2(dy, dx);
                double r = Math.sqrt(dx * dx + dy * dy);
                a = (a % (2 * Math.PI / folds) + 2 * Math.PI) % (2 * Math.PI / folds);
                float hue = (float) (a / (2 * Math.PI / folds));
                int v = (int) (100 + 100 * Math.sin(r * 0.02));
                img.setRGB(x, y, 0xFF000000 | (Color.HSBtoRGB(hue, 0.6f, v / 255f) & 0xFFFFFF));
            }
        }
        return img;
    }

    private static BufferedImage aurora() {
        BufferedImage img = create(WIDTH, HEIGHT);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double v = Math.sin(y * 0.015) * Math.sin(x * 0.01 + y * 0.02) * 0.5 + 0.5;
                int g = (int) (50 + 180 * v);
                int b = (int) (80 + 120 * v);
                img.setRGB(x, y, (255 << 24) | (30 << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    private static BufferedImage portal() {
        BufferedImage img = create(WIDTH, HEIGHT);
        int cx = WIDTH / 2, cy = HEIGHT / 2;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double dx = x - cx, dy = y - cy;
                double r = Math.sqrt(dx * dx + dy * dy);
                double a = Math.atan2(dy, dx);
                double swirl = a + r * 0.02;
                float hue = (float) (swirl / (2 * Math.PI) % 1);
                int v = (int) (80 + 100 * (1 - r / 400));
                if (v < 0) v = 0;
                if (v > 255) v = 255;
                img.setRGB(x, y, 0xFF000000 | (Color.HSBtoRGB(hue, 0.9f, v / 255f) & 0xFFFFFF));
            }
        }
        return img;
    }

    private static BufferedImage easterEgg() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 215, 0));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(200, 150, 0));
        for (int i = 0; i < 20; i++) {
            g.fillOval((int) (Math.random() * WIDTH), (int) (Math.random() * HEIGHT), 30, 40);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage hiddenGem() {
        BufferedImage img = create(WIDTH, HEIGHT);
        int[] colors = { 0xFF00CED1, 0xFF9370DB, 0xFFFF69B4 };
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int c = colors[(x / 100 + y / 100) % 3];
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    private static BufferedImage legendary() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(75, 0, 130));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(255, 215, 0));
        g.fillOval(WIDTH / 2 - 80, HEIGHT / 2 - 80, 160, 160);
        g.dispose();
        return img;
    }

    private static BufferedImage mythic() {
        BufferedImage img = create(WIDTH, HEIGHT);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double t = (x + y) * 0.005;
                int r = (int) (100 + 80 * Math.sin(t));
                int g = (int) (50 + 100 * Math.sin(t + 2));
                int b = (int) (200 + 55 * Math.sin(t + 4));
                img.setRGB(x, y, (255 << 24) | (r << 16) | (g << 8) | Math.min(255, b));
            }
        }
        return img;
    }

    private static BufferedImage omega() {
        BufferedImage img = create(WIDTH, HEIGHT);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 0, 0));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(255, 255, 255));
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.drawString("OMEGA", WIDTH / 2 - 100, HEIGHT / 2);
        g.dispose();
        return img;
    }

    // ═══════════════════════════════════════════════════════════════
    //  RETRO SAMPLES — native resolution per console
    // ═══════════════════════════════════════════════════════════════

    private static BufferedImage retroGbCastle() {
        int w = 160, h = 144;
        BufferedImage img = create(w, h);
        Color sky = new Color(155, 188, 15);
        Color light = new Color(139, 172, 15);
        Color mid = new Color(48, 98, 48);
        Color dark = new Color(15, 56, 15);
        Graphics2D g = img.createGraphics();
        g.setColor(sky);
        g.fillRect(0, 0, w, h);
        g.setColor(light);
        g.fillRect(0, 90, w, 54);
        g.setColor(mid);
        g.fillRect(55, 40, 50, 50);
        g.fillRect(60, 25, 10, 15);
        g.fillRect(90, 25, 10, 15);
        g.setColor(dark);
        g.fillRect(70, 55, 20, 35);
        g.fillRect(58, 35, 6, 5);
        g.fillRect(96, 35, 6, 5);
        for (int i = 0; i < 4; i++) {
            g.fillRect(57 + i * 12, 38, 4, 6);
        }
        g.setColor(mid);
        for (int x = 0; x < w; x += 8) {
            int th = 6 + (x * 7 + 3) % 12;
            g.fillRect(x, 90 - th, 6, th);
        }
        g.setColor(dark);
        g.fillRect(0, 120, w, 24);
        g.dispose();
        return img;
    }

    private static BufferedImage retroGbHills() {
        int w = 160, h = 144;
        BufferedImage img = create(w, h);
        int[] shades = {224, 168, 96, 32};
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(shades[0], shades[0], shades[0]));
        g.fillRect(0, 0, w, h);
        int[][] hills = {{20, 70, 80}, {80, 60, 100}, {130, 75, 70}};
        g.setColor(new Color(shades[1], shades[1], shades[1]));
        for (int[] hill : hills) {
            g.fillOval(hill[0] - hill[2] / 2, hill[1] - hill[2] / 3, hill[2], hill[2] * 2 / 3);
        }
        g.setColor(new Color(shades[2], shades[2], shades[2]));
        g.fillRect(0, 95, w, 49);
        for (int x = 10; x < w; x += 25) {
            g.fillRect(x, 80, 4, 15);
            g.fillOval(x - 5, 72, 14, 12);
        }
        g.setColor(new Color(shades[3], shades[3], shades[3]));
        g.fillRect(0, 130, w, 14);
        g.dispose();
        return img;
    }

    private static BufferedImage retroGbcTown() {
        int w = 160, h = 144;
        BufferedImage img = create(w, h);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(135, 206, 235));
        g.fillRect(0, 0, w, 70);
        g.setColor(new Color(100, 180, 100));
        g.fillRect(0, 70, w, 74);
        Color[] roofs = {new Color(180, 50, 50), new Color(50, 50, 180), new Color(180, 120, 30)};
        Color[] walls = {new Color(240, 220, 180), new Color(200, 200, 220), new Color(220, 200, 160)};
        int[] houseX = {10, 55, 105};
        for (int i = 0; i < 3; i++) {
            g.setColor(walls[i]);
            g.fillRect(houseX[i], 45, 40, 30);
            g.setColor(roofs[i]);
            int[] xp = {houseX[i] - 3, houseX[i] + 20, houseX[i] + 43};
            int[] yp = {45, 25, 45};
            g.fillPolygon(xp, yp, 3);
            g.setColor(new Color(100, 70, 40));
            g.fillRect(houseX[i] + 15, 58, 10, 17);
            g.setColor(new Color(180, 220, 255));
            g.fillRect(houseX[i] + 5, 50, 7, 7);
            g.fillRect(houseX[i] + 28, 50, 7, 7);
        }
        g.setColor(new Color(60, 120, 60));
        for (int x = 8; x < w; x += 20) {
            g.fillOval(x, 62, 16, 14);
            g.setColor(new Color(80, 60, 30));
            g.fillRect(x + 6, 73, 3, 8);
            g.setColor(new Color(60, 120, 60));
        }
        g.setColor(new Color(180, 170, 140));
        g.fillRect(0, 95, w, 12);
        g.setColor(new Color(255, 220, 50));
        g.fillOval(130, 5, 20, 20);
        g.dispose();
        return img;
    }

    private static BufferedImage retroGbcUnderwater() {
        int w = 160, h = 144;
        BufferedImage img = create(w, h);
        for (int y = 0; y < h; y++) {
            double t = (double) y / h;
            int r = (int) (20 + 30 * t);
            int gg = (int) (80 + 60 * t);
            int b = (int) (180 - 60 * t);
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, (255 << 24) | (r << 16) | (gg << 8) | b);
            }
        }
        Graphics2D g = img.createGraphics();
        Color[] corals = {new Color(200, 80, 100), new Color(220, 150, 60), new Color(100, 200, 120), new Color(180, 60, 160)};
        for (int i = 0; i < 6; i++) {
            int cx = 10 + i * 28;
            g.setColor(corals[i % corals.length]);
            for (int j = 0; j < 4; j++) {
                g.fillRect(cx + j * 3, 125 - j * 8, 4, j * 8 + 19);
            }
        }
        g.setColor(new Color(200, 180, 120));
        g.fillRect(0, 130, w, 14);
        Color fishBody = new Color(255, 180, 50);
        Color fishFin = new Color(255, 100, 50);
        int[][] fishPos = {{30, 40}, {90, 25}, {120, 60}, {50, 80}};
        for (int[] fp : fishPos) {
            g.setColor(fishBody);
            g.fillOval(fp[0], fp[1], 14, 8);
            g.setColor(fishFin);
            int[] tx = {fp[0], fp[0] - 6, fp[0]};
            int[] ty = {fp[1] + 1, fp[1] + 4, fp[1] + 7};
            g.fillPolygon(tx, ty, 3);
            g.setColor(Color.BLACK);
            g.fillRect(fp[0] + 10, fp[1] + 2, 2, 2);
        }
        for (int i = 0; i < 12; i++) {
            g.setColor(new Color(180, 220, 255, 120));
            int bx = (i * 17 + 5) % w, by = 10 + (i * 13) % 110;
            g.fillOval(bx, by, 4, 5);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage retroGenSkyline() {
        int w = 320, h = 224;
        BufferedImage img = create(w, h);
        for (int y = 0; y < h; y++) {
            double t = (double) y / h;
            int r = (int) (40 + 200 * Math.pow(t, 0.7));
            int gg = (int) (20 + 100 * t);
            int b = (int) (120 - 80 * t);
            r = Math.min(255, r); gg = Math.min(255, gg); b = Math.max(0, b);
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, (255 << 24) | (r << 16) | (gg << 8) | b);
            }
        }
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 200, 50));
        g.fillOval(130, 30, 60, 60);
        int[] bldgX = {10, 40, 65, 95, 120, 150, 175, 200, 225, 255, 280};
        int[] bldgH = {90, 130, 70, 110, 150, 80, 120, 60, 100, 140, 75};
        for (int i = 0; i < bldgX.length; i++) {
            int bw = 20 + (i % 3) * 5;
            int by = h - bldgH[i];
            g.setColor(new Color(30 + i * 5, 20 + i * 3, 40 + i * 4));
            g.fillRect(bldgX[i], by, bw, bldgH[i]);
            g.setColor(new Color(255, 230, 120));
            for (int wy = by + 5; wy < h - 10; wy += 12) {
                for (int wx = bldgX[i] + 3; wx < bldgX[i] + bw - 3; wx += 6) {
                    if ((wx + wy) % 3 != 0) g.fillRect(wx, wy, 3, 5);
                }
            }
        }
        g.setColor(new Color(20, 10, 30));
        g.fillRect(0, h - 20, w, 20);
        g.dispose();
        return img;
    }

    private static BufferedImage retroGenRuins() {
        int w = 320, h = 224;
        BufferedImage img = create(w, h);
        for (int y = 0; y < h; y++) {
            double t = (double) y / h;
            int r = (int) (100 + 80 * t);
            int gg = (int) (120 + 40 * t);
            int b = (int) (160 - 40 * t);
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, (255 << 24) | (r << 16) | (gg << 8) | b);
            }
        }
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(80, 100, 80));
        int[] mtnX = {0, 60, 120, 180, 240, 320};
        for (int i = 0; i < mtnX.length - 1; i++) {
            int peak = 50 + (i * 23) % 40;
            int[] px = {mtnX[i], (mtnX[i] + mtnX[i + 1]) / 2, mtnX[i + 1]};
            int[] py = {h, peak, h};
            g.fillPolygon(px, py, 3);
        }
        Color stone = new Color(160, 140, 100);
        Color stoneDark = new Color(120, 100, 70);
        int[][] pillars = {{60, 80}, {100, 100}, {140, 90}, {200, 110}, {250, 85}};
        for (int[] p : pillars) {
            g.setColor(stone);
            g.fillRect(p[0], h - p[1], 16, p[1] - 30);
            g.setColor(stoneDark);
            g.fillRect(p[0] - 2, h - p[1], 20, 8);
            g.fillRect(p[0] - 2, h - 30, 20, 8);
            g.setColor(new Color(140, 120, 80));
            for (int vy = h - p[1] + 10; vy < h - 35; vy += 10) {
                g.drawLine(p[0], vy, p[0] + 15, vy);
            }
        }
        g.setColor(stone);
        g.fillRect(55, h - 105, 105, 10);
        g.setColor(new Color(60, 100, 40));
        g.fillRect(0, h - 25, w, 25);
        for (int x = 5; x < w; x += 12) {
            int gh = 5 + (x * 7) % 10;
            g.setColor(new Color(40 + (x % 30), 80 + (x % 40), 30));
            g.fillRect(x, h - 25 - gh, 3, gh);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage retroGbaForest() {
        int w = 240, h = 160;
        BufferedImage img = create(w, h);
        for (int y = 0; y < h; y++) {
            double t = (double) y / h;
            int r = (int) (80 + 30 * t);
            int gg = (int) (140 - 50 * t);
            int b = (int) (60 + 40 * t);
            for (int x = 0; x < w; x++) {
                double noise = Math.sin(x * 0.1 + y * 0.05) * 10;
                int pr = Math.min(255, Math.max(0, r + (int) noise));
                int pg = Math.min(255, Math.max(0, gg + (int) noise));
                int pb = Math.min(255, Math.max(0, b + (int) (noise * 0.5)));
                img.setRGB(x, y, (255 << 24) | (pr << 16) | (pg << 8) | pb);
            }
        }
        Graphics2D g = img.createGraphics();
        int[][] trees = {{20, 60}, {60, 50}, {100, 70}, {140, 45}, {180, 65}, {210, 55}};
        for (int[] t : trees) {
            int trunk = t[1];
            g.setColor(new Color(80, 50, 30));
            g.fillRect(t[0] - 3, h - trunk, 8, trunk - 10);
            for (int layer = 0; layer < 3; layer++) {
                int shade = 40 + layer * 20;
                g.setColor(new Color(shade, 80 + layer * 30, shade - 10));
                int ly = h - trunk - layer * 15 + 5;
                int lw = 30 - layer * 6;
                g.fillOval(t[0] - lw / 2, ly, lw, 20);
            }
        }
        g.setColor(new Color(50, 90, 40));
        g.fillRect(0, h - 15, w, 15);
        for (int x = 0; x < w; x += 3) {
            g.setColor(new Color(30 + (x % 40), 70 + (x % 50), 20 + (x % 20)));
            g.fillRect(x, h - 15 - (x % 5), 2, (x % 5) + 2);
        }
        int[] rayX = {100, 120, 140, 160};
        g.setColor(new Color(255, 255, 180, 40));
        for (int rx : rayX) {
            int[] px = {rx, rx + 8, rx + 30, rx + 20};
            int[] py = {0, 0, h, h};
            g.fillPolygon(px, py, 4);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage retroGbaBeach() {
        int w = 240, h = 160;
        BufferedImage img = create(w, h);
        Graphics2D g = img.createGraphics();
        for (int y = 0; y < 60; y++) {
            double t = (double) y / 60;
            int r = (int) (100 + 80 * t);
            int gg = (int) (160 + 60 * t);
            int b = 250;
            g.setColor(new Color(r, gg, b));
            g.drawLine(0, y, w, y);
        }
        for (int y = 60; y < 100; y++) {
            double t = (double) (y - 60) / 40;
            int r = (int) (30 + 20 * t);
            int gg = (int) (160 + 40 * Math.sin(t * Math.PI));
            int b = (int) (200 - 30 * t);
            for (int x = 0; x < w; x++) {
                double wave = Math.sin(x * 0.08 + y * 0.3) * 3;
                int wy = y + (int) wave;
                if (wy >= 60 && wy < 100) {
                    img.setRGB(x, wy, (255 << 24) | (r << 16) | (gg << 8) | b);
                }
            }
        }
        for (int y = 100; y < h; y++) {
            double t = (double) (y - 100) / 60;
            int r = (int) (220 + 20 * t);
            int gg = (int) (200 + 20 * t);
            int b = (int) (150 + 40 * t);
            g.setColor(new Color(r, Math.min(255, gg), Math.min(255, b)));
            g.drawLine(0, y, w, y);
        }
        g.setColor(new Color(255, 200, 50));
        g.fillOval(180, 10, 30, 30);
        g.setColor(new Color(80, 50, 30));
        g.fillRect(195, 50, 5, 35);
        int cx = 197;
        g.setColor(new Color(40, 140, 40));
        int[] lx = {cx, cx + 20, cx + 5};
        int[] ly = {40, 55, 58};
        g.fillPolygon(lx, ly, 3);
        int[] lx2 = {cx, cx - 18, cx - 3};
        g.fillPolygon(lx2, ly, 3);
        int[] lx3 = {cx, cx + 15, cx + 3};
        int[] ly3 = {35, 48, 50};
        g.fillPolygon(lx3, ly3, 3);
        g.setColor(new Color(255, 255, 255, 80));
        for (int x = 10; x < w; x += 30) {
            g.fillOval(x, 95 + (x % 5), 20, 3);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage retroSnesMountains() {
        int w = 256, h = 224;
        BufferedImage img = create(w, h);
        for (int y = 0; y < h; y++) {
            double t = (double) y / h;
            int r = (int) (140 + 80 * t);
            int gg = (int) (160 + 60 * t);
            int b = (int) (200 - 60 * t);
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, (255 << 24) | (Math.min(255, r) << 16) | (Math.min(255, gg) << 8) | Math.max(0, b));
            }
        }
        Graphics2D g = img.createGraphics();
        int[][] mtns = {{-20, 40, 120}, {60, 30, 100}, {150, 50, 130}, {220, 35, 90}};
        for (int layer = 0; layer < mtns.length; layer++) {
            int alpha = 180 + layer * 20;
            int shade = 60 + layer * 25;
            g.setColor(new Color(shade, shade + 20, shade + 40, Math.min(255, alpha)));
            int[] m = mtns[layer];
            int[] px = {m[0], m[0] + m[2] / 2, m[0] + m[2]};
            int[] py = {h, m[1], h};
            g.fillPolygon(px, py, 3);
            if (layer < 2) {
                g.setColor(new Color(240, 240, 255, 150));
                int peakX = m[0] + m[2] / 2, peakY = m[1];
                int[] sx = {peakX, peakX - 15, peakX + 15};
                int[] sy = {peakY, peakY + 25, peakY + 25};
                g.fillPolygon(sx, sy, 3);
            }
        }
        g.setColor(new Color(60, 100, 50));
        g.fillRect(0, 170, w, 54);
        for (int x = 5; x < w; x += 15) {
            int th = 8 + (x * 3) % 15;
            g.setColor(new Color(40 + (x % 30), 80 + (x % 40), 30 + (x % 20)));
            g.fillOval(x - 5, 165 - th / 2, 12, th);
        }
        for (int y = 0; y < 60; y++) {
            double t = (double) y / 60;
            g.setColor(new Color(200, 210, 230, (int) (40 * (1 - t))));
            g.drawLine(0, 80 + y, w, 80 + y);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage retroSnesVillage() {
        int w = 256, h = 224;
        BufferedImage img = create(w, h);
        Graphics2D g = img.createGraphics();
        for (int y = 0; y < 80; y++) {
            double t = (double) y / 80;
            int r = (int) (255 - 60 * t);
            int gg = (int) (180 - 40 * t);
            int b = (int) (100 + 40 * t);
            g.setColor(new Color(r, gg, b));
            g.drawLine(0, y, w, y);
        }
        g.setColor(new Color(255, 180, 50));
        g.fillOval(200, 15, 35, 35);
        g.setColor(new Color(90, 140, 70));
        g.fillRect(0, 80, w, 144);
        g.setColor(new Color(160, 140, 100));
        g.fillRect(0, 140, w, 15);
        Color[][] houses = {
            {new Color(200, 60, 40), new Color(240, 220, 180)},
            {new Color(50, 80, 160), new Color(220, 210, 200)},
            {new Color(160, 100, 40), new Color(200, 190, 160)},
            {new Color(80, 140, 60), new Color(230, 225, 200)}
        };
        int[] hx = {15, 75, 140, 195};
        for (int i = 0; i < 4; i++) {
            int hy = 90;
            g.setColor(houses[i][1]);
            g.fillRect(hx[i], hy, 50, 35);
            g.setColor(houses[i][0]);
            int[] rx = {hx[i] - 3, hx[i] + 25, hx[i] + 53};
            int[] ry = {hy, hy - 18, hy};
            g.fillPolygon(rx, ry, 3);
            g.setColor(new Color(100, 70, 40));
            g.fillRect(hx[i] + 20, hy + 15, 10, 20);
            g.setColor(new Color(180, 220, 255));
            g.fillRect(hx[i] + 5, hy + 5, 10, 10);
            g.fillRect(hx[i] + 35, hy + 5, 10, 10);
            g.setColor(new Color(120, 80, 40));
            g.drawRect(hx[i] + 5, hy + 5, 10, 10);
            g.drawRect(hx[i] + 35, hy + 5, 10, 10);
        }
        g.setColor(new Color(70, 120, 50));
        for (int x = 30; x < w; x += 60) {
            g.fillOval(x, 155, 20, 15);
            g.setColor(new Color(80, 60, 30));
            g.fillRect(x + 8, 168, 4, 10);
            g.setColor(new Color(70, 120, 50));
        }
        g.setColor(new Color(200, 80, 60));
        g.fillRect(50, 185, 8, 30);
        g.setColor(new Color(180, 160, 120));
        g.fillRect(47, 183, 14, 4);
        g.dispose();
        return img;
    }
}
