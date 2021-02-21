package com.rndmodgames.artevolver.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.rndmodgames.artevolver.ArtEvolver;

public class DesktopLauncher {
    
	public static void main (String[] arg) {
	    
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		
		/**
		 * Resolution
		 */
		config.width = 1280;
		config.height = 768;
		
		/**
		 * Disable FPS Throttling
		 */
		config.vSyncEnabled = false;
		config.foregroundFPS = 0;
		config.backgroundFPS = 0;
		
		new LwjglApplication(new ArtEvolver(), config);
	}
}
