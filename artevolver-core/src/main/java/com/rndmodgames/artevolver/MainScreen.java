package com.rndmodgames.artevolver;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooser.Mode;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;
import com.kotcrab.vis.ui.widget.file.FileTypeFilter;
import com.rndmodgames.evolver.AbstractEvolver;
import com.rndmodgames.evolver.Palette;

/**
 * Main Art Evolver Screen
 * 
 * @author Geomancer86
 */
public class MainScreen implements Screen {

    // parent reference
    Game game;
    
    /**
     * LibGDX Components
     */
    Stage stage;
    SpriteBatch batch;
    Texture sourceTexture;
    
    /**
     * Main Screen Tables
     */
    VisTable evolverTable = null;
    VisTable imageTable = null;
    VisTable menuTable = null;
    AspectRatioFitter<VisTable> imageFitter = null;
    
    /**
     * Parameters
     */
    private static int CPU_CORES                    = 1; // 1-ALL_CORES
    private static int THREADS                      = 1; // 1-x
    private static int POPULATION                   = 2; // 2-x
    
    private static final String DEFAULT_PALETTE = "Sherwin-Williams";
    private static int PALETTES                     = 1; // 1-x
    
    private static int TRIANGLES_WIDTH            = 1; // 
    private static int TRIANGLES_HEIGHT           = 1; // 
    
    private static float TRIANGLE_SIZE_WIDTH  = 3.0f;
    private static float TRIANGLE_SIZE_HEIGHT = 3.0f;
    
    private static float TRIANGLE_SCALE = 1.0f;

    private static Integer [] AVAILABLE_PALETTE_COUNTS = new Integer [] { 1, 2, 3, 4, 5, 6, 7, 8 };
    
    private static Integer [] AVAILABLE_TRIANGLE_WIDTHS = new Integer [] {  10, 20, 80, 90};
    
    private static Integer [] AVAILABLE_TRIANGLE_HEIGHTS = new Integer [] { 10, 20, 53, 80, 90 };
     
    private VisSelectBox<Integer> trianglesWidthSelectBox = null;
    private VisSelectBox<Integer> trianglesHeightSelectBox = null;
    
    /**
     * Statistics
     */
    VisLabel triangleCount = new VisLabel("0");
    VisLabel bestScore = new VisLabel("0.0");
    
    VisLabel populationValueLabel = new VisLabel("0");
    VisLabel totalMutations = new VisLabel("0");
    VisLabel goodMutations = new VisLabel("0");
    VisLabel goodMutationRate = new VisLabel("0.0");
    
    /**
     * Evolver
     */
    private static Palette palette;
    private boolean isRunning = false;
    private List<AbstractEvolver> population = new ArrayList<>();
    
    /**
     * Source Image File
     */
    Pixmap sourceImage = null;
    
    //
    public MainScreen(Game game) throws URISyntaxException {
        
        this.game = game;
        
        //
        loadPalettes();
        
        //
        batch = new SpriteBatch();
        
        // 
        stage = new Stage(new ScreenViewport());
        
        /**
         * Main Screen Table
         */
        final VisTable table = new VisTable();
        table.setFillParent(true);
        table.setDebug(true, true);

        /**
         * Evolver Table
         */
        evolverTable = new VisTable(true);
        
        /**
         * Evolver/Source Image Table
         */
        imageTable = new VisTable();
        
        /**
         * Main Menu Table
         */
        menuTable = new VisTable(true);
        menuTable.pad(5);
        
        /**
         * Parameters Table
         */
        final VisTable parameters = new VisTable(true);
//        parameters.setDebug(true);
        
        /**
         * Statistics Table
         */
        final VisTable statistics = new VisTable(true);
//        statistics.setDebug(true);
        
        /**
         * Set Source Image Button
         */
        final VisTextButton setSourceImageButton = new VisTextButton("Set Source Image");
        setSourceImageButton.setDisabled(true);
        
        /**
         * Select File Button
         */
        final VisTextButton selectFileButton = new VisTextButton("Select Image File");
        
        /**
         * Evolution Start/Stop
         */
        final VisTextButton startButton = new VisTextButton("Start");
        startButton.setDisabled(true);
        
        final VisTextButton stopButton = new VisTextButton("Stop");
        stopButton.setDisabled(true);

        /**
         * File Chooser
         */
        FileChooser.setSaveLastDirectory(true);
        FileChooser.setDefaultPrefsName(ArtEvolver.PREFERENCES_NAME);
        
        /**
         * File Type Filter
         * 
         * TODO: allow all files but show a "File type not supported message" on any exception Seeting/Loading/Comparing/Evolving
         */
        FileTypeFilter typeFilter = new FileTypeFilter(true); // allow "All Types" mode where all files are shown
        typeFilter.addRule("Image files (*.png, *.jpg, *.gif)", "png", "jpg", "gif");
        
        final FileChooser fileChooser = new FileChooser(Mode.OPEN);
        fileChooser.setFileTypeFilter(typeFilter);

        // 
        fileChooser.setListener(new FileChooserAdapter() {
            
            @Override
            public void selected (Array<FileHandle> files) {
                
                // 
                sourceImage = new Pixmap(new FileHandle(files.get(0).file().getAbsolutePath()));
                
                // Enable Set Source Image
                setSourceImageButton.setDisabled(false);
            }
        });
        
        /**
         * Select File Button
         */
        selectFileButton.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                
                // displaying chooser with fade in animation
                stage.addActor(fileChooser.fadeIn());
            }
        });
        
        /**
         * Set Source Image Button
         */
        setSourceImageButton.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                
                // start evolving the image
                System.out.println("Set Source Image!");
                System.out.println("sourceImage: " + sourceImage.getWidth() + " x " + sourceImage.getHeight());
                
                // Create a Texture from Pixmap
                sourceTexture = new Texture(sourceImage);
                
                // Set the Source Image Background on Main Table
                /**
                 * Draw Source Texture/Current Best Evolver
                 * 
                 *  - Keep Aspect Ratio [OK]
                 *  - Stretch to Fit    [WIP]
                 */
                imageTable.clear();
                    
                imageTable.setWidth(sourceTexture.getWidth());
                imageTable.setHeight(sourceTexture.getHeight());
                
                imageTable.setBackground(new TextureRegionDrawable(new TextureRegion(sourceTexture)));
                    
                imageFitter = new AspectRatioFitter<>(imageTable);
                
                evolverTable.clear();
                evolverTable.add(imageFitter);
                
                /**
                 * Enable Evolution Start Button
                 */
                startButton.setDisabled(false);
            }
        });
        
        /**
         * Parameters
         */
        VisLabel palettesLabel = new VisLabel("Palettes:");
        VisLabel trianglesWidth = new VisLabel("Width (Triangles):");
        VisLabel trianglesHeight = new VisLabel("Height (Triangles):");

        // 
        VisSelectBox<Integer> palettesSelectBox = new VisSelectBox<>();
        palettesSelectBox.setItems(AVAILABLE_PALETTE_COUNTS);
        
        palettesSelectBox.addListener(new ChangeListener() {
            
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                
                // The selected number of Palettes
                PALETTES = palettesSelectBox.getSelected();
                
                System.out.println("PALETTES: " + PALETTES);
                
                // Update Palettes
                try {
                    loadPalettes();
                } catch (URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        
        //
        trianglesWidthSelectBox = new VisSelectBox<>();
        trianglesWidthSelectBox.setItems(AVAILABLE_TRIANGLE_WIDTHS);
        
        trianglesWidthSelectBox.addListener(new ChangeListener() {
            
            @Override
            public void changed(ChangeEvent event, Actor actor) {

                //
                updateTrianglesArrangement();
            }
        });
        
        trianglesHeightSelectBox = new VisSelectBox<>();
        trianglesHeightSelectBox.setItems(AVAILABLE_TRIANGLE_HEIGHTS);
        
        trianglesHeightSelectBox.addListener(new ChangeListener() {
            
            @Override
            public void changed(ChangeEvent event, Actor actor) {

                //
                updateTrianglesArrangement();
            }
        });
        
        //
        parameters.add(palettesLabel).grow();
        parameters.add(palettesSelectBox).grow();
        parameters.row();
        
        //
        parameters.add(trianglesWidth).grow();
        parameters.add(trianglesWidthSelectBox).grow();
        parameters.row();
        
        //
        parameters.add(trianglesHeight).grow();
        parameters.add(trianglesHeightSelectBox).grow();
        parameters.row();
        
        /**
         * Statistics
         *  - Triangle Count
         *  - Best Score
         *  - Average Score
         *  - Population Size
         *  - Total Mutations
         *  - Good Mutations
         *  - Good Mutation Overall Rate
         *  - Good Mutation Rate Past 5 Minutes
         *  - Good Mutations Per Minute
         *  - Good Mutations Per Hour
         */
        VisLabel triangleCountLabel = new VisLabel("Triangle Count:");
        VisLabel bestScoreLabel = new VisLabel("Best Score:");
        
        VisLabel populationLabel = new VisLabel("Population:");
        VisLabel totalMutationsLabel = new VisLabel("Total:");
        VisLabel goodMutationsLabel = new VisLabel("Good:");
        VisLabel goodMutationRateLabel = new VisLabel("Success Rate:");
        
        // align numbers to the right
        triangleCount.setAlignment(Align.right);
        bestScore.setAlignment(Align.right);
        populationValueLabel.setAlignment(Align.right);
        totalMutations.setAlignment(Align.right);
        goodMutations.setAlignment(Align.right);
        goodMutationRate.setAlignment(Align.right);
        
        //
        statistics.add(triangleCountLabel).grow();
        statistics.add(triangleCount).grow();
        
        //
        statistics.row();
        statistics.add(bestScoreLabel).grow();
        statistics.add(bestScore).grow();
        
        // 
        statistics.row();
        statistics.add(populationLabel).grow();
        statistics.add(populationValueLabel).grow();
        
        //
        statistics.row();
        statistics.add(totalMutationsLabel).grow();
        statistics.add(totalMutations).grow();
        
        //
        statistics.row();
        statistics.add(goodMutationsLabel).grow();
        statistics.add(goodMutations).grow();
        
        //
        statistics.row();
        statistics.add(goodMutationRateLabel).grow();
        statistics.add(goodMutationRate).grow();
        
        //
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                
                System.out.println("START EVOLVING!");
                
                //
                isRunning = true;
                stopButton.setDisabled(false);
                startButton.setDisabled(true);
                
                /**
                 * - If Evolvers are null this is the first run,
                 *      - Initialize Evolvers
                 *  
                 * - Start Evolvers
                 */
                if (population.isEmpty()) {
                    
                    // create Evolver Instances as configured by THREADS x CORES
                    for (int a = 0; a < THREADS*CPU_CORES; a++) {
                        
                        //
//                        population.add(e)
                    }
                }
            }
        });
        
        //
        stopButton.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                
                System.out.println("STOP EVOLVING!");
                
                //
                isRunning = false;
                startButton.setDisabled(false);
                stopButton.setDisabled(true);
                
                /**
                 * Stop Evolvers
                 */
            }
        });
        
        //
        menuTable.row().colspan(2);
        menuTable.add(setSourceImageButton).fill();
        
        //
        menuTable.row().colspan(2);
        menuTable.add(selectFileButton).fill();
        
        menuTable.row().colspan(2);
        menuTable.addSeparator();
        
        //
        menuTable.row().colspan(2);
        menuTable.add(parameters).grow();
        menuTable.row().colspan(2);
        menuTable.addSeparator();
        
        //
        menuTable.row().colspan(2);
        menuTable.add(statistics).grow();
        menuTable.row().colspan(2);
        menuTable.addSeparator();
        
        menuTable.add(startButton).fill();
        menuTable.add(stopButton).fill();

        /**
         * Menu size relative to screen width, make dynamic when resizing
         */
        table.add(evolverTable).width(Gdx.graphics.getWidth()*0.9f).expand();
        table.add(menuTable).width(Gdx.graphics.getWidth()*0.1f).expand().right().top();
        
        stage.addActor(table);
    }
    
    @Override
    public void show() {

        // Add input capabilities
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
       
        // Clear blit
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                
        /**
         * Draw Source Texture/Current Best Evolver
         * 
         *  - Keep Aspect Ratio [OK]
         *  - Stretch to Fit    [WIP]
         */
//        if (sourceTexture != null) {
//        
//            imageTable.clear();
//            
//            imageTable.setWidth(sourceTexture.getWidth());
//            imageTable.setHeight(sourceTexture.getHeight());
//            
//            imageTable.setBackground(new TextureRegionDrawable(new TextureRegion(sourceTexture)));
//            
//            imageFitter = new AspectRatioFitter<>(imageTable);
//            
//            evolverTable.clear();
//            evolverTable.add(imageFitter);
//        }
        
        // Draw UI
        stage.act();
        stage.draw();  
    }

    /**
     * 
     */
    public void evolve() {
        
    }
    
    /**
     * 80x53
     */
    public void setDefaultTrianglesArrangement() {
        
        // ideal width for 4 palettes
        if (palette.getNumberOfColors() == 4260) {
            
            trianglesWidthSelectBox.setSelected(80);
            trianglesHeightSelectBox.setSelected(53);
        }
    }
    
    /**
     * 
     */
    public void updateTrianglesArrangement() {
        
    }
    
    /**
     * Loads/Reloads the Palettes
     * @throws URISyntaxException 
     */
    public void loadPalettes() throws URISyntaxException {
        
        //
        try {
            
            palette = new Palette(DEFAULT_PALETTE, PALETTES);
            
        } catch (IOException e) {
            
            // TODO error loading palette/exit
            e.printStackTrace();
            System.exit(-1);
        }
        
        triangleCount.setText(palette.getNumberOfColors());
        
        // update parameters
        setDefaultTrianglesArrangement();
    }
    
    @Override
    public void resize(int width, int height) {
        
        // Update viewport on resize to keep event coordinate for buttons/widgets
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void hide() {
        
        System.out.println("HIDE WAS CALLED ON MAIN SCREEN - DISPOSE");
        
        // Hide will be called after switching to a separate screen
        dispose();
    }

    @Override
    public void dispose() {

        stage.dispose();
    }
}