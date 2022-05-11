package com.rndmodgames.artevolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rndmodgames.evolver.Palette;
import com.rndmodgames.evolver.PalleteColor;

class ArtEvolverTest {

    static com.rndmodgames.evolver.ArtEvolver evolver;
    
    @BeforeAll
    static void beforeAll() throws IOException, URISyntaxException {
        
        evolver = new com.rndmodgames.evolver.ArtEvolver();
    }
    
    @Test
    void setSourceImageTest() throws IOException {
        
//        URL url = getClass().getResource("../../../sherwin.txt").toURI().toURL();

//        File file = new File(url.getPath().replace("%20"," "));
        
        evolver.setOfflineSourceImage("./000_zeldathumb-1920-789452.jpg");
        
        // Load image
        evolver.loadImage();
        
        // Call set source image after setting Original Image to something in Image Load
        evolver.setSourceImage();
        
        //
        assertNotNull(evolver.getResizedOriginal());
        
        // close the window to avoid waiting
        evolver.dispatchEvent(new WindowEvent(evolver, WindowEvent.WINDOW_CLOSING));
    }
    
    @Test
    void artEvolverInstanceSecondTest() {
        
        //
        assertNotNull(evolver);
    }
    
    @Test
    void paletteTest() throws IOException, URISyntaxException {
        
        //
        Palette palette = new Palette("Sherwin-Williams", 1);

        //
        assertNotNull(palette);
    }
    
    @Test
    void getPaletteNumberOfColorsTest() throws IOException, URISyntaxException {
        
        //
        Palette palette = new Palette("Sherwin-Williams", 1);

        //
        assertEquals(1065, palette.getNumberOfColors());
    }
    
    @Test
    void randomizePaletteTest() throws IOException, URISyntaxException {
        
        //
        Palette palette = new Palette("Sherwin-Williams", 1);
        
        PalleteColor first = palette.getColor(0);
        
        // shuffle
        palette.randomize();
                
        // might fail sometimes!
        assertNotEquals(palette.getColor(0), first);
    }
    
    @Test
    void removeColorTest() throws IOException, URISyntaxException {
        
        //
        Palette palette = new Palette("Sherwin-Williams", 1);
        
        // remove one color
        palette.removeColor(0);
        
        //
        assertEquals(1064, palette.getNumberOfColors());
    }
    
    @Test
    void getColorFromPaletteTest() throws IOException, URISyntaxException {
     
        //
        Palette palette = new Palette("Sherwin-Williams", 1);
        
        //
        assertNotNull(palette.getColor(0).getColor());
    }
    
    @Test
    void orderPalettesByColorTest() throws IOException, URISyntaxException {
        
        //
        Palette palette = new Palette("Sherwin-Williams", 1);
        
        //
        palette.orderByBLUE();
        palette.orderByGREEN();
        palette.orderByRED();
        palette.orderByLuminescence();
        
        //
        assertNotNull(palette.getColor(0));
        assertEquals(1065, palette.getNumberOfColors());
    }
}
