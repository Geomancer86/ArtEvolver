package com.rndmodgames.artevolver.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.rndmodgames.evolver.ArtEvolver;

/**
 * Art Evolver Manager v1
 * 
 * This manager is used to run/batch several instances of ArtEvolver
 * 
 * General purpose is to automatize folder processing for quick image candidate preselection
 * 
 * @author Geomancer86
 */
public class ArtEvolverManager {

    /**
     * Parameters
     */
    int instances;
    int qualityMode;
    int timeout;
    
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
    public ArtEvolverManager(String sourceFolder, int instances, int timeout) {
        
        this.sourceFolder = sourceFolder;
        this.instances = instances;
        this.timeout = timeout;
        
        //
//        System.out.println("PROCESSING IN " + ArtEvolver.MODES[ArtEvolver.CURRENT_MODE]);
    }
    
    public void readAllFiles() {
        
        int filecount = 0;
        
        try (Stream<Path> walk = Files.walk(Paths.get(sourceFolder))) {

            List<String> result = walk.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());

            for (String file : result) {

                // 
                processArtEvolver(file);
                
                filecount++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        
        /**
         * 
         */
        while(true) {
            
            // reset count
            int activeCount = 0;
            
            for (ArtEvolver evolver : evolvers) {
                
                if (evolver.isVisible()) {
                    
                    activeCount++;
                }
            }
            
            // 
            if (activeCount == 0) {
                
                break;
            }
            
            try {
                
                /**
                 * TODO: maybe timeout * activeCount
                 */
                TimeUnit.SECONDS.sleep(timeout);
                
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
//        System.out.println("FINISHED PROCESSING " + filecount + " IMAGES.");
        System.exit(0);
    }
    
    /**
     * 
     */
    public void processArtEvolver(String image) {
        
        try {
            
            ArtEvolver evolver = new ArtEvolver();
            
            evolver.setOfflineSourceImage(image);
            
            evolvers.add(evolver);
            
            evolver.start();
            
        } catch (IOException e) {

            e.printStackTrace();
        }

    }
    
    /**
     * 
     * @param args
     */
    public static void main(String[] args) {

//        String sourceFolder = "C:\\Media\\ArtEvolver2021\\sources";
//        String sourceFolder = "D:\\Media\\Exported - Instagram";
        String sourceFolder = "C:\\Media\\ArtEvolver2021\\sources2\\own";
        
        /**
         * Static Configuration
         */
//        ArtEvolver.CURRENT_MODE = ArtEvolver.QUICK_MODE;
        ArtEvolver.CURRENT_MODE = ArtEvolver.FASTEST_BATCH_MODE;
        
        ArtEvolverManager manager = new ArtEvolverManager(sourceFolder, 1, 5);
        
        // 
        manager.readAllFiles();
    }
}