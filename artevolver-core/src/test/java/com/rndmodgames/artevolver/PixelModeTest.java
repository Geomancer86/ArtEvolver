package com.rndmodgames.artevolver;

import com.rndmodgames.evolver.clicker.*;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;
import com.rndmodgames.evolver.clicker.ProfileManager;

/**
 * Tests for the retro pixel mode: PixelGrid, PixelFitnessEngine, PaletteLoader, RetroPreset.
 */
class PixelModeTest {

    @Test
    void testRetroPresetFromId() {
        assertEquals(RetroPreset.GB_DMG, RetroPreset.fromId("gb_dmg"));
        assertEquals(RetroPreset.GENESIS, RetroPreset.fromId("genesis"));
        assertEquals(RetroPreset.SNES, RetroPreset.fromId("snes"));
        assertNull(RetroPreset.fromId("nonexistent"));
        assertNull(RetroPreset.fromId(null));
    }

    @Test
    void testPaletteLoaderGbDmg() {
        Color[] palette = PaletteLoader.loadFromResource("gb_dmg");
        assertEquals(4, palette.length);
        for (Color c : palette) assertNotNull(c);
    }

    @Test
    void testPaletteLoaderGbc() {
        Color[] palette = PaletteLoader.loadFromResource("gbc");
        assertEquals(32, palette.length);
    }

    @Test
    void testPaletteLoaderGenesis512() {
        Color[] palette = PaletteLoader.generateGenesis512();
        assertEquals(512, palette.length);
        assertEquals(new Color(0, 0, 0), palette[0]);
        assertEquals(new Color(255, 255, 255), palette[511]);
        // VDP-accurate ramp: level 1 = 52, level 2 = 87 (not linear 36, 73)
        assertEquals(new Color(52, 0, 0), palette[64]);   // r=1,g=0,b=0
        assertEquals(new Color(0, 87, 0), palette[16]);  // r=0,g=2,b=0
        assertEquals(new Color(0, 0, 52), palette[1]);   // r=0,g=0,b=1
    }

    @Test
    void testPaletteLoaderGenesis64Subset() {
        Color[] palette = PaletteLoader.generateGenesisSubset(64);
        assertEquals(64, palette.length);
        assertTrue(java.util.Arrays.asList(palette).contains(new Color(255, 0, 0)));
        assertTrue(java.util.Arrays.asList(palette).contains(new Color(0, 255, 0)));
        assertTrue(java.util.Arrays.asList(palette).contains(new Color(0, 0, 255)));
    }

    @Test
    void testPixelGridInit() {
        Color[] palette = new Color[]{Color.BLACK, Color.WHITE, Color.RED, Color.BLUE};
        int w = 4, h = 4;
        BufferedImage ref = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ref.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 2, 4);
        g.setColor(Color.WHITE);
        g.fillRect(2, 0, 2, 4);
        g.dispose();

        PixelGrid grid = new PixelGrid(w, h, palette);
        grid.initFromReference(ref);

        assertEquals(16, grid.getTotalPixels());
        BufferedImage rendered = grid.render();
        assertNotNull(rendered);
        assertEquals(w, rendered.getWidth());
        assertEquals(h, rendered.getHeight());
    }

    @Test
    void testPixelFitnessSwapDelta() {
        Color[] palette = new Color[]{Color.BLACK, Color.WHITE};
        int w = 2, h = 1;
        BufferedImage ref = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ref.setRGB(0, 0, Color.BLACK.getRGB());
        ref.setRGB(1, 0, Color.WHITE.getRGB());

        PixelGrid grid = new PixelGrid(w, h, palette);
        grid.initFromReference(ref);

        PixelFitnessEngine engine = new PixelFitnessEngine(grid, ref);
        double initialScore = engine.getScore();
        assertTrue(initialScore >= 0 && initialScore <= 1.0);

        if (grid.getColorIndex(0) == 0 && grid.getColorIndex(1) == 1) {
            assertEquals(1.0, initialScore, 0.001);
            long delta = engine.computeSwapDelta(0, 1);
            assertTrue(delta > 0);
        }
    }

    @Test
    void testPixelFitnessScoreMatchesFormula() {
        Color[] palette = PaletteLoader.loadFromResource("gb_dmg");
        int w = 10, h = 10;
        BufferedImage ref = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ref.createGraphics();
        g.setColor(new Color(100, 150, 50));
        g.fillRect(0, 0, w, h);
        g.dispose();

        PixelGrid grid = new PixelGrid(w, h, palette);
        grid.initFromReference(ref);
        PixelFitnessEngine engine = new PixelFitnessEngine(grid, ref);

        double score = engine.getScore();
        assertTrue(score > 0, "Score should be positive");
        assertTrue(score <= 1.0, "Score should be <= 1.0");

        long manualDiff = 0;
        for (int i = 0; i < grid.getTotalPixels(); i++) {
            Color c = grid.getColor(i);
            int r = (ref.getRGB(i % w, i / w) >> 16) & 0xff;
            int gg = (ref.getRGB(i % w, i / w) >> 8) & 0xff;
            int b = ref.getRGB(i % w, i / w) & 0xff;
            manualDiff += Math.abs(r - c.getRed()) + Math.abs(gg - c.getGreen()) + Math.abs(b - c.getBlue());
        }
        double manualScore = 1.0 - ((double) manualDiff / (w * h * 3.0 * 255.0));
        assertEquals(manualScore, score, 0.0001);
    }

    @Test
    void testSwapImprovesThenApply() {
        Color[] palette = new Color[]{new Color(0, 0, 0), new Color(255, 255, 255)};
        int w = 2, h = 1;
        BufferedImage ref = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ref.setRGB(0, 0, Color.WHITE.getRGB());
        ref.setRGB(1, 0, Color.BLACK.getRGB());

        PixelGrid grid = new PixelGrid(w, h, palette);
        // Force wrong assignment: pixel0=BLACK, pixel1=WHITE (opposite of ref)
        grid.getColorIndices()[0] = 0;
        grid.getColorIndices()[1] = 1;

        PixelFitnessEngine engine = new PixelFitnessEngine(grid, ref);
        double before = engine.getScore();

        long delta = engine.computeSwapDelta(0, 1);
        assertTrue(delta < 0, "Swap should improve fitness (negative delta)");

        engine.applySwapWithDelta(0, 1, delta);
        double after = engine.getScore();
        assertTrue(after > before, "Score should improve after swap");
        assertEquals(1.0, after, 0.001, "Should be perfect match");
    }

    @Test
    void testProfileSaveLoadRoundTrip() {
        ClickerState original = new ClickerState();

        // Simulate some progress by poking via reflection-free approach:
        // We'll just verify the save/load JSON round-trip works
        String json = original.toSaveJson();
        assertNotNull(json);
        assertTrue(json.contains("\"saveVersion\":1"));
        assertTrue(json.contains("\"ep\":"));
        assertTrue(json.contains("\"upgradeLevels\":"));
        assertTrue(json.contains("\"gallery\":"));

        ClickerState restored = new ClickerState();
        restored.loadFromSaveJson(json);

        // Verify restored state matches
        assertEquals(original.getEp(), restored.getEp(), 0.001);
        assertEquals(original.getMc(), restored.getMc(), 0.001);
        assertEquals(original.getGf(), restored.getGf(), 0.001);
        assertEquals(original.getTotalClicks(), restored.getTotalClicks());
        assertEquals(original.getAscensionCount(), restored.getAscensionCount());
    }

    @Test
    void testProfileSaveLoadWithData() {
        ClickerState state = new ClickerState();
        String json = state.toSaveJson();

        // Inject some data into the JSON to test parsing
        String modified = json.replace("\"ep\":0.0", "\"ep\":1234.5")
                              .replace("\"totalClicks\":0", "\"totalClicks\":999")
                              .replace("\"ascensionCount\":0", "\"ascensionCount\":5")
                              .replace("\"gf\":0.0", "\"gf\":42.7");

        ClickerState loaded = new ClickerState();
        loaded.loadFromSaveJson(modified);

        assertEquals(1234.5, loaded.getEp(), 0.01);
        assertEquals(999, loaded.getTotalClicks());
        assertEquals(5, loaded.getAscensionCount());
        assertEquals(42.7, loaded.getGf(), 0.01);
    }

    @Test
    void testProfileManagerSanitize() {
        assertEquals("Player", ProfileManager.sanitize(null));
        assertEquals("Player", ProfileManager.sanitize(""));
        assertEquals("Player", ProfileManager.sanitize("   "));
        assertEquals("Hello World", ProfileManager.sanitize("Hello World"));
        assertEquals("Test123", ProfileManager.sanitize("Test123!@#$%"));
        assertEquals("my-profile", ProfileManager.sanitize("my-profile"));
    }

    @Test
    void testClickerEnginePixelMode() {
        Color[] palette = PaletteLoader.loadFromResource("gb_dmg");
        assertNotNull(palette);
        assertTrue(palette.length >= 4);

        int w = 160, h = 144;
        BufferedImage ref = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ref.createGraphics();
        g.setColor(new Color(48, 98, 48));
        g.fillRect(0, 0, 80, 144);
        g.setColor(new Color(155, 188, 15));
        g.fillRect(80, 0, 80, 144);
        g.dispose();

        ClickerEngine engine = new ClickerEngine();
        engine.initPixelMode(ref, RetroPreset.GB_DMG);

        assertTrue(engine.isInitialized());
        assertTrue(engine.isPixelMode());
        assertEquals(RetroPreset.GB_DMG, engine.getActivePreset());
        assertEquals(160 * 144, engine.getTriangleCount());
        assertTrue(engine.getFitness() > 0);
        assertTrue(engine.getStartingFitness() > 0);

        ClickerEngine.ClickResult result = engine.performClick(1, 5, 0, 0);
        assertNotNull(result);

        BufferedImage rendered = engine.getRenderedImage();
        assertNotNull(rendered);
        int expectedScale = Math.max(1, 640 / 160);
        assertEquals(160 * expectedScale, rendered.getWidth());
        assertEquals(144 * expectedScale, rendered.getHeight());
    }
}
