package com.rndmodgames.artevolver;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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
 * @author Geomancer86
 */
public class MainScreen implements Screen {

    Game game;
    Stage stage;
    SpriteBatch batch;
    Texture img;
    
    public MainScreen(Game game) {
        
        this.game = game;
        
        stage = new Stage(new ScreenViewport());

        final VisTable table = new VisTable();
        table.setFillParent(true);

        /**
         * New Game: 
         *  - New Game Screen
         */
        final VisTextButton newGameButton = new VisTextButton("Open Image");
        final VisTextButton selectFileButton = new VisTextButton("Select Image");

        /**
         * File Chooser
         * 
         * TODO: point to /assets samples image folder by default
         */
        FileChooser.setSaveLastDirectory(true);
        
        final FileChooser fileChooser = new FileChooser(Mode.OPEN);

        fileChooser.setListener(new FileChooserAdapter() {
            
            @Override
            public void selected (Array<FileHandle> files) {
                
                for (FileHandle file : files) {
                    
                    System.out.println("Selected File: " + file.file().getAbsolutePath());
                }
            }
        });
        
        newGameButton.addCaptureListener(new InputListener() {
            
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                
                if (newGameButton.isPressed()) {

                    System.out.println("Load Image");
                }
            }
        });
        
        //button listener
        selectFileButton.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                
                //displaying chooser with fade in animation
                stage.addActor(fileChooser.fadeIn());
            }
        });
        
        table.row();
        table.add(newGameButton).fill();
        
        table.row();
        table.add(selectFileButton).fill();
        
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