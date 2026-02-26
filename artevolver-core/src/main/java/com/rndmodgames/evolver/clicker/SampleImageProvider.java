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

    public record SampleDef(String id, String name, String description,
                           String unlockType, double unlockValue, String category,
                           boolean secret, int sortOrder) {}

    private static final Set<String> PROGRAMMATIC_IDS = Set.of(
            "gradient_sunset", "circles", "checker", "stripes", "diamond",
            "spiral", "rings", "grid_grad", "waves", "maze",
            "starfield", "hexagons", "voronoi", "noise_cloud", "mandala",
            "rose", "fractal_tree", "kaleidoscope", "aurora", "portal",
            "easter_egg", "hidden_gem", "legendary", "mythic", "omega"
    );

    private static final List<SampleDef> DEFS = new ArrayList<>();

    static {
        // ─── REAL TREE: file-based (samples/*.jpg or .png). Curate for 75–80%+ fitness. ───
        DEFS.add(new SampleDef("landscape_01", "Landscape", "Open-source landscape photo.",
                "always", 0, "Starter", false, 1));
        DEFS.add(new SampleDef("ocean_01", "Ocean", "Free ocean / seascape.",
                "always", 0, "Starter", false, 2));
        DEFS.add(new SampleDef("forest_01", "Forest", "Nature forest scene.",
                "always", 0, "Starter", false, 3));
        DEFS.add(new SampleDef("sunset_01", "Sunset", "Golden hour sky.",
                "always", 0, "Starter", false, 4));
        DEFS.add(new SampleDef("flowers_01", "Flowers", "Floral photography.",
                "always", 0, "Starter", false, 5));

        DEFS.add(new SampleDef("mountain_01", "Mountain", "Mountain vista.",
                "ep", 500, "Apprentice", false, 10));
        DEFS.add(new SampleDef("canyon_01", "Canyon", "Desert canyon.",
                "ep", 2000, "Apprentice", false, 11));
        DEFS.add(new SampleDef("lake_01", "Lake", "Calm lake reflection.",
                "ep", 5000, "Apprentice", false, 12));
        DEFS.add(new SampleDef("meadow_01", "Meadow", "Green meadow.",
                "ep", 10000, "Apprentice", false, 13));
        DEFS.add(new SampleDef("beach_01", "Beach", "Beach shoreline.",
                "ep", 20000, "Apprentice", false, 14));

        DEFS.add(new SampleDef("cityscape_01", "Cityscape", "Urban skyline.",
                "ascensions", 1, "Veteran", false, 20));
        DEFS.add(new SampleDef("portrait_01", "Portrait", "People / portrait.",
                "ascensions", 2, "Veteran", false, 21));
        DEFS.add(new SampleDef("architecture_01", "Architecture", "Building / structure.",
                "ascensions", 3, "Veteran", false, 22));
        DEFS.add(new SampleDef("wildlife_01", "Wildlife", "Animal photography.",
                "ascensions", 5, "Veteran", false, 23));
        DEFS.add(new SampleDef("night_01", "Night", "Night scene.",
                "ascensions", 7, "Veteran", false, 24));

        DEFS.add(new SampleDef("abstract_01", "Abstract", "Abstract art.",
                "masterpieces", 1, "Master", false, 30));
        DEFS.add(new SampleDef("macro_01", "Macro", "Close-up detail.",
                "masterpieces", 2, "Master", false, 31));
        DEFS.add(new SampleDef("aerial_01", "Aerial", "Aerial / drone view.",
                "masterpieces", 3, "Master", false, 32));
        DEFS.add(new SampleDef("street_01", "Street", "Street photography.",
                "masterpieces", 5, "Master", false, 33));
        DEFS.add(new SampleDef("still_life_01", "Still Life", "Still life composition.",
                "masterpieces", 7, "Master", false, 34));

        DEFS.add(new SampleDef("secret_01", "???", "You found something.",
                "clicks", 5000, "Secret", true, 40));
        DEFS.add(new SampleDef("secret_02", "???", "Something stirs.",
                "ep", 30000, "Secret", true, 41));
        DEFS.add(new SampleDef("secret_03", "???", "A legend awakens.",
                "ascensions", 10, "Secret", true, 42));
        DEFS.add(new SampleDef("secret_04", "???", "Beyond the veil.",
                "masterpieces", 10, "Secret", true, 43));
        DEFS.add(new SampleDef("secret_05", "???", "The final canvas.",
                "gf", 100, "Secret", true, 44));

        // ─── GENERATED: programmatic samples, always available for testing. ───
        DEFS.add(new SampleDef("gradient_sunset", "Sunset Gradient", "A warm gradient from orange to purple.",
                "always", 0, "Generated", false, 50));
        DEFS.add(new SampleDef("circles", "Concentric Circles", "Nested circles for clean evolution.",
                "always", 0, "Generated", false, 51));
        DEFS.add(new SampleDef("checker", "Checkerboard", "Classic black and white pattern.",
                "always", 0, "Generated", false, 52));
        DEFS.add(new SampleDef("stripes", "Rainbow Stripes", "Horizontal color bands.",
                "always", 0, "Generated", false, 53));
        DEFS.add(new SampleDef("diamond", "Diamond Shape", "A central diamond on gradient.",
                "always", 0, "Generated", false, 54));
        DEFS.add(new SampleDef("spiral", "Spiral", "Archimedean spiral pattern.",
                "always", 0, "Generated", false, 55));
        DEFS.add(new SampleDef("rings", "Ripple Rings", "Expanding rings from center.",
                "always", 0, "Generated", false, 56));
        DEFS.add(new SampleDef("grid_grad", "Grid Gradient", "Mesh with smooth gradients.",
                "always", 0, "Generated", false, 57));
        DEFS.add(new SampleDef("waves", "Sine Waves", "Overlapping wave patterns.",
                "always", 0, "Generated", false, 58));
        DEFS.add(new SampleDef("maze", "Mini Maze", "A simple labyrinth pattern.",
                "always", 0, "Generated", false, 59));
        DEFS.add(new SampleDef("starfield", "Starfield", "Scattered points like stars.",
                "always", 0, "Generated", false, 60));
        DEFS.add(new SampleDef("hexagons", "Honeycomb", "Hexagonal tessellation.",
                "always", 0, "Generated", false, 61));
        DEFS.add(new SampleDef("voronoi", "Voronoi", "Cell-like subdivision.",
                "always", 0, "Generated", false, 62));
        DEFS.add(new SampleDef("noise_cloud", "Cloud Noise", "Soft perlin-like texture.",
                "always", 0, "Generated", false, 63));
        DEFS.add(new SampleDef("mandala", "Mandala", "Radial symmetry pattern.",
                "always", 0, "Generated", false, 64));
        DEFS.add(new SampleDef("rose", "Rose Curve", "Mathematical rose pattern.",
                "always", 0, "Generated", false, 65));
        DEFS.add(new SampleDef("fractal_tree", "Fractal Tree", "Recursive branch structure.",
                "always", 0, "Generated", false, 66));
        DEFS.add(new SampleDef("kaleidoscope", "Kaleidoscope", "Multi-fold symmetry.",
                "always", 0, "Generated", false, 67));
        DEFS.add(new SampleDef("aurora", "Aurora", "Northern lights simulation.",
                "always", 0, "Generated", false, 68));
        DEFS.add(new SampleDef("portal", "Portal", "Swirling vortex effect.",
                "always", 0, "Generated", false, 69));
        DEFS.add(new SampleDef("easter_egg", "???", "You found something.",
                "always", 0, "Generated", true, 70));
        DEFS.add(new SampleDef("hidden_gem", "???", "Something stirs.",
                "always", 0, "Generated", true, 71));
        DEFS.add(new SampleDef("legendary", "???", "A legend awakens.",
                "always", 0, "Generated", true, 72));
        DEFS.add(new SampleDef("mythic", "???", "Beyond the veil.",
                "always", 0, "Generated", true, 73));
        DEFS.add(new SampleDef("omega", "???", "The final canvas.",
                "always", 0, "Generated", true, 74));
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
}
