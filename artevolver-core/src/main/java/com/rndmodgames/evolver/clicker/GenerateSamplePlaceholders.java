package com.rndmodgames.evolver.clicker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Generates placeholder images for the real-photo sample tree.
 * Run once to create samples in artevolver-core/src/main/resources/samples/.
 * Replace with curated CC0 images (75–80%+ fitness) from Unsplash, Pexels, Pixabay.
 *
 * Usage: mvn exec:java -Dexec.mainClass="com.rndmodgames.evolver.clicker.GenerateSamplePlaceholders"
 */
public final class GenerateSamplePlaceholders {

    private static final int W = SampleImageProvider.WIDTH;
    private static final int H = SampleImageProvider.HEIGHT;

    private static final List<String> REAL_IDS = List.of(
            "landscape_01", "ocean_01", "forest_01", "sunset_01", "flowers_01",
            "mountain_01", "canyon_01", "lake_01", "meadow_01", "beach_01",
            "cityscape_01", "portrait_01", "architecture_01", "wildlife_01", "night_01",
            "abstract_01", "macro_01", "aerial_01", "street_01", "still_life_01",
            "secret_01", "secret_02", "secret_03", "secret_04", "secret_05"
    );

    public static void main(String[] args) throws IOException {
        Path outDir = Paths.get("src/main/resources/samples");
        Files.createDirectories(outDir);
        System.out.println("Writing to " + outDir.toAbsolutePath());

        for (String id : REAL_IDS) {
            BufferedImage img = createPlaceholder(id);
            Path out = outDir.resolve(id + ".jpg");
            ImageIO.write(img, "jpg", out.toFile());
            System.out.println("  " + out.getFileName());
        }
        System.out.println("Done. Replace with real CC0 images for 75–80%+ fitness. See samples/README.md.");
    }

    private static BufferedImage createPlaceholder(String id) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Photo-like gradients per id theme
        if (id.startsWith("landscape")) skyGroundGradient(g, 100, 80, 60, 60, 90, 50);
        else if (id.startsWith("ocean")) oceanGradient(g);
        else if (id.startsWith("forest")) forestGradient(g);
        else if (id.startsWith("sunset")) sunsetGradient(g);
        else if (id.startsWith("flowers")) floralGradient(g);
        else if (id.startsWith("mountain")) mountainGradient(g);
        else if (id.startsWith("canyon")) canyonGradient(g);
        else if (id.startsWith("lake")) lakeGradient(g);
        else if (id.startsWith("meadow")) meadowGradient(g);
        else if (id.startsWith("beach")) beachGradient(g);
        else if (id.startsWith("cityscape")) cityGradient(g);
        else if (id.startsWith("portrait")) portraitGradient(g);
        else if (id.startsWith("architecture")) archGradient(g);
        else if (id.startsWith("wildlife")) wildlifeGradient(g);
        else if (id.startsWith("night")) nightGradient(g);
        else if (id.startsWith("abstract")) abstractGradient(g);
        else if (id.startsWith("macro")) macroGradient(g);
        else if (id.startsWith("aerial")) aerialGradient(g);
        else if (id.startsWith("street")) streetGradient(g);
        else if (id.startsWith("still_life")) stillLifeGradient(g);
        else skyGroundGradient(g, 40, 30, 50, 60, 80, 70); // secret

        g.dispose();
        return img;
    }

    private static void skyGroundGradient(Graphics2D g, int r1, int g1, int b1, int r2, int g2, int b2) {
        for (int y = 0; y < H; y++) {
            double t = (double) y / H;
            int r = (int) (r1 + (r2 - r1) * t);
            int gv = (int) (g1 + (g2 - g1) * t);
            int b = (int) (b1 + (b2 - b1) * t);
            g.setColor(new Color(r, gv, b));
            g.fillRect(0, y, W, 1);
        }
    }

    private static void oceanGradient(Graphics2D g) {
        for (int y = 0; y < H; y++) {
            double t = (double) y / H;
            int b = 80 + (int) (120 * t);
            int gv = 100 + (int) (60 * t);
            g.setColor(new Color(20, gv, b));
            g.fillRect(0, y, W, 1);
        }
    }

    private static void forestGradient(Graphics2D g) {
        for (int y = 0; y < H; y++) {
            double t = (double) y / H;
            int gv = 60 + (int) (120 * (1 - t));
            int r = 30 + (int) (40 * (1 - t));
            g.setColor(new Color(r, gv, 25));
            g.fillRect(0, y, W, 1);
        }
    }

    private static void sunsetGradient(Graphics2D g) {
        for (int y = 0; y < H; y++) {
            double t = (double) y / H;
            int r = 255 - (int) (150 * t);
            int gv = 100 + (int) (80 * t);
            int b = 80 + (int) (100 * t);
            g.setColor(new Color(Math.min(255, r), gv, b));
            g.fillRect(0, y, W, 1);
        }
    }

    private static void floralGradient(Graphics2D g) {
        skyGroundGradient(g, 200, 100, 150, 180, 80, 120);
    }

    private static void mountainGradient(Graphics2D g) {
        skyGroundGradient(g, 135, 180, 220, 80, 90, 60);
    }

    private static void canyonGradient(Graphics2D g) {
        for (int y = 0; y < H; y++) {
            double t = (double) y / H;
            int r = 180 - (int) (80 * t);
            int gv = 100 - (int) (40 * t);
            int b = 60 - (int) (30 * t);
            g.setColor(new Color(r, gv, b));
            g.fillRect(0, y, W, 1);
        }
    }

    private static void lakeGradient(Graphics2D g) {
        skyGroundGradient(g, 100, 150, 200, 40, 80, 100);
    }

    private static void meadowGradient(Graphics2D g) {
        skyGroundGradient(g, 120, 160, 200, 60, 140, 50);
    }

    private static void beachGradient(Graphics2D g) {
        skyGroundGradient(g, 135, 200, 255, 240, 220, 160);
    }

    private static void cityGradient(Graphics2D g) {
        skyGroundGradient(g, 60, 80, 120, 80, 80, 90);
    }

    private static void portraitGradient(Graphics2D g) {
        skyGroundGradient(g, 200, 160, 150, 180, 140, 120);
    }

    private static void archGradient(Graphics2D g) {
        skyGroundGradient(g, 200, 210, 220, 120, 110, 100);
    }

    private static void wildlifeGradient(Graphics2D g) {
        skyGroundGradient(g, 100, 120, 100, 80, 90, 60);
    }

    private static void nightGradient(Graphics2D g) {
        for (int y = 0; y < H; y++) {
            double t = (double) y / H;
            int v = (int) (20 + 40 * (1 - t));
            g.setColor(new Color(v, v, v + 30));
            g.fillRect(0, y, W, 1);
        }
    }

    private static void abstractGradient(Graphics2D g) {
        skyGroundGradient(g, 120, 100, 180, 180, 120, 200);
    }

    private static void macroGradient(Graphics2D g) {
        skyGroundGradient(g, 80, 60, 50, 180, 160, 140);
    }

    private static void aerialGradient(Graphics2D g) {
        skyGroundGradient(g, 100, 150, 200, 90, 130, 80);
    }

    private static void streetGradient(Graphics2D g) {
        skyGroundGradient(g, 180, 190, 200, 100, 95, 90);
    }

    private static void stillLifeGradient(Graphics2D g) {
        skyGroundGradient(g, 220, 200, 180, 180, 170, 150);
    }
}
