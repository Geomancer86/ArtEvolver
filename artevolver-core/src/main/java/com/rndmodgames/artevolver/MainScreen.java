package com.rndmodgames.artevolver;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooser.Mode;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;

/**
 * Main Art Evolver Screen
 * 
 * https://github.com/libgdx/libgdx/wiki/Pixmaps
 * 
 * @author Geomancer86
 */
public class MainScreen implements Screen {

    Game game;
    Stage stage;
    
    // 
    SpriteBatch batch;
    Texture sourceTexture;
    
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
         * Main Menu Table
         */
        final VisTable menuTable = new VisTable(true);
        
        /**
         * New Game: 
         *  - New Game Screen
         */
        final VisTextButton startEvolvingButton = new VisTextButton("Open Image");
        final VisTextButton selectFileButton = new VisTextButton("Select Image");

        /**
         * File Chooser
         */
        FileChooser.setSaveLastDirectory(true);
        FileChooser.setDefaultPrefsName(ArtEvolver.PREFERENCES_NAME);
        
        final FileChooser fileChooser = new FileChooser(Mode.OPEN);

        // 
        fileChooser.setListener(new FileChooserAdapter() {
            
            @Override
            public void selected (Array<FileHandle> files) {
                
                // 
                System.out.println("Selected File: " + files.get(0).file().getAbsolutePath());
                
                sourceImage = new Pixmap(new FileHandle(files.get(0).file().getAbsolutePath()));
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
        
        startEvolvingButton.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                
                // start evolving the image
                System.out.println("Start Evolving!");
                System.out.println("sourceImage: " + sourceImage.getWidth() + " x " + sourceImage.getHeight());
                
                // Create a Texture from Pixmap
                sourceTexture = new Texture(sourceImage);
            }
        });

        //
        menuTable.row();
        menuTable.add(startEvolvingButton);
        menuTable.row();
        menuTable.add(selectFileButton);
        
        //
        table.add(menuTable).width(160).expand().right();
        
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
        
        //
        if (sourceTexture != null) {
        
            batch.begin();
            
//            batch.draw(sourceTexture, delta, delta, delta, delta, delta, delta, delta, delta);
            
//            batch.draw(sourceTexture, 0, 0);
            batch.end();
        }
        
        // Draw
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