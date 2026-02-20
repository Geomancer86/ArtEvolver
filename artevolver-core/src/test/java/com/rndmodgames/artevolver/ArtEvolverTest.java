package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import com.rndmodgames.evolver.Palette;
import com.rndmodgames.evolver.PalleteColor;

class ArtEvolverTest {

    static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    @Test
    @DisabledIf("isHeadless")
    void artEvolverInstanceTest() throws IOException, URISyntaxException {
        com.rndmodgames.evolver.ArtEvolver evolver = new com.rndmodgames.evolver.ArtEvolver();
        assertNotNull(evolver);
        evolver.setVisible(false);
        evolver.dispose();
    }

    @Test
    void paletteTest() throws IOException, URISyntaxException {
        Palette palette = new Palette("Sherwin-Williams", 1);
        assertNotNull(palette);
    }
    
    @Test
    void getPaletteNumberOfColorsTest() throws IOException, URISyntaxException {
        Palette palette = new Palette("Sherwin-Williams", 1);
        assertTrue(palette.getNumberOfColors() > 0);
    }
    
    @Test
    void randomizePaletteTest() throws IOException, URISyntaxException {
        Palette palette = new Palette("Sherwin-Williams", 1);
        PalleteColor first = palette.getColor(0);
        palette.randomize();
        assertNotEquals(palette.getColor(0), first);
    }
    
    @Test
    void removeColorTest() throws IOException, URISyntaxException {
        Palette palette = new Palette("Sherwin-Williams", 1);
        int existingColors = palette.getNumberOfColors();
        palette.removeColor(0);
        assertEquals(existingColors - 1, palette.getNumberOfColors());
    }
    
    @Test
    void getColorFromPaletteTest() throws IOException, URISyntaxException {
        Palette palette = new Palette("Sherwin-Williams", 1);
        assertNotNull(palette.getColor(0).getColor());
    }
    
    @Test
    void orderPalettesByColorTest() throws IOException, URISyntaxException {
        Palette palette = new Palette("Sherwin-Williams", 1);
        palette.orderByBLUE();
        palette.orderByGREEN();
        palette.orderByRED();
        palette.orderByLuminescence();
        assertNotNull(palette.getColor(0));
    }
}
