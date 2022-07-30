package com.rndmodgames.artevolver.processrunner;

import com.rndmodgames.evolver.ArtEvolverTools;

/**
 * Art Evolver Process Runner v1
 * 
 * - TODO: WIP Command Line Art Evolver
 * 
 * 
 * @author Geomancer86
 */
public class ProcessRunner {

    /**
     * TODO: this needs ALL THE PARAMETERS REQUIRED TO WORK TO BE 
     *                                                              - PASSED AS COMMAND LINE ARGS
     *                                                              - SANITIZED/VALIDATED
     *                                                              - SET AS MAIN CLASS
     */
    public ProcessRunner() {
        
        ArtEvolverTools.getImageEvolver(0, 0, 0, 0, null, false);
        
        //
        start();
    }
    
    /**
     * 
     */
    public void start() {
        
        System.out.println("Art Evolver Started");
        

    }
    
    /**
     * 
     */
    public void stop() {
        
    }
    
    /**
     * 
     */
    public void pause() {
        
    }
    
    /**
     * 
     */
    public void status() {
        
    }
    
    
    /**
     * 
     */
    public static void main (String args []) {
    
        ProcessRunner processRunner = new ProcessRunner();
        
        processRunner.start();
    }
}