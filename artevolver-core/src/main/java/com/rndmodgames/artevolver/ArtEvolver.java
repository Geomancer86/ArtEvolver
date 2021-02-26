package com.rndmodgames.artevolver;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.kotcrab.vis.ui.VisUI;

/**
 * ArtEvolver v3.0.0
 * 
 *  - Reingeneering of ArtEvolver v2.02
 *      - Swing to LibGDX
 *      - Genetic Algorithm Rewrite
 *      - Functional UI
 *      - Parameter Saving/Restoring
 *      - Evolution Saving/Restoring
 * 
 * @author Geomancer86
 */
public class ArtEvolver extends Game {
    
    private Game instance;
    
    /**
     * 
     */
    private Preferences preferences;
    
    /**
     * 
     */
    public static final String PREFERENCES_NAME = "ArtEvolver Preferences";
    
    @Override
    public void create() {
        
        this.instance = this;
        
        preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        
//        preferences.putString("app.version", "v3.0.0");
//        saveUserPreferences();
//        System.out.println("App Version: " + preferences.getString("app.version"));
        
        // Load VisUI
        VisUI.load(Gdx.files.internal("skin/tixel.json"));
        
        // Load Main Screen
        this.setScreen(new MainScreen(instance));
    }
	
    /**
     * Returns a Saved Preference
     */
    public String getPreference(String key) {
        
        return preferences.getString(key);
    }
    
    /**
     * Utility method used to update User Preferences
     */
    public void updateUserPreference(String key, String value) {
        
//      System.out.println("updateUserPreference() " + key + ", " + value);
        
        preferences.putString(key, value);
        
        // Save after each change
        saveUserPreferences();
    }
    
    /**
     * Utility method used to bulk save User Preferences on Settings Screen Exit
     */
    public void saveUserPreferences() {
        
        // bulk update your preferences
        // NOTE: only will get persisted if you explicitly call the flush() method
        preferences.flush();
    }
}