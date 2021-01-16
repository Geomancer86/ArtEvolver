package com.rndmodgames.artevolver;

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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooser.Mode;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;
import com.kotcrab.vis.ui.widget.file.FileTypeFilter;

/**
 * Main Art Evolver Screen
 * 
 * https://github.com/libgdx/libgdx/wiki/Pixmaps
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
     * Source Image File
     */
    Pixmap sourceImage = null; 
    
    public MainScreen(Game game) {
        
        this.game = game;
        
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
        
        //
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                
                System.out.println("START EVOLVING!");
                
                //
                stopButton.setDisabled(false);
                startButton.setDisabled(true);
            }
        });
        
        //
        stopButton.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                
                System.out.println("STOP EVOLVING!");
                
                //
                startButton.setDisabled(false);
                stopButton.setDisabled(true);
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