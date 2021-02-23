package com.rndmodgames.artevolver.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import com.rndmodgames.evolver.ArtEvolver;

/**
 * 
 * @author Geomancer86
 */
public class ArtEvolverManager {

    /**
     * Parameters
     */
    int instances;
    int qualityMode;
    
    //
    List<ArtEvolver> evolvers = new ArrayList<>();
    
    // 
    String sourceFolder;
    
    /**
     * Set a Folder
     * Iterate over all Images
     * Run ArtEvolver in the fastest mode
     * Log all the stats
     */
    public ArtEvolverManager(String sourceFolder, int instances) {
        
        this.sourceFolder = sourceFolder;
        this.instances = instances;
    }
    
    public void readAllFiles() {
        
        try (Stream<Path> walk = Files.walk(Paths.get(sourceFolder))) {

            List<String> result = walk.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());

            for (String file : result) {

                processArtEvolver(file);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     */
    public void processArtEvolver(String image) {
        
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                try {

                    ArtEvolver instance = new ArtEvolver();

                    /**
                     * Set the Image Name
                     */
                    instance.setOfflineSourceImage(image);
                    instance.start();

                } catch (IOException e) {

                    e.printStackTrace();
                }
            }

        });
    }
    
    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

        String sourceFolder = "F:\\Media\\ArtEvolver2021\\sources";
        
        /**
         * Static Configuration
         */
        ArtEvolver.CURRENT_MODE = ArtEvolver.FASTEST_MODE;
        
        ArtEvolverManager manager = new ArtEvolverManager(sourceFolder, 1);
        
        manager.readAllFiles();
        
//        SwingUtilities.invokeLater(new Runnable() {
//            
//            @Override
//            public void run() {
//                
//                try {
//                    
//                    ArtEvolver instance = new ArtEvolver();
//                    
//                    /**
//                     * Set the Data Folder
//                     */
//                    
//                    
//                } catch (IOException e) {
//                    
//                    e.printStackTrace();
//                }
//            }
//            
//        });
    }
}