package com.rndmodgames.evolver;

/**
 * ArtEvolver v3.0
 * 
 *  - LibGDX/OpenGL Version
 * 
 * @author Geomancer86
 */
public class LibGDXEvolver extends AbstractEvolver {

    private long threadID = -1;

    // parameters
    private int iterationsPerCycle = 1;
    
    // stats
    private boolean isStarted = true;
    private boolean isRunning = false;
    
    private long started = 0;
    private long previousTime = 0;
    private long elapsedTime = 0;
    private long iterations = 0;
    private long goodIterations = 0;
    
    /**
     * 
     */
    public LibGDXEvolver() {
        
    }
    
    //
    public void setRunning(boolean running) {
        
        this.isRunning = running;
    }
    
    // 
    public void setThreadId(long threadID) {
        
        this.threadID = threadID;
    }
    
    //
    public void setIterationsPerCycle(int iterations) {
        
        this.iterationsPerCycle = iterations;
    }
    
    //
    public long getIterations() {
        
        return iterations;
    }
    
    //
    public long getGoodIterations() {
        
        return goodIterations;
    }
    
    //
    public long getElapsedTime() {
        
        return elapsedTime;
    }
    
    @Override
    public void run() {
        // TODO Auto-generated method stub
        
        System.out.println("Start new LibGDXEvolver thread!");

        while(true) {

            // initialize time counts
            this.started = System.currentTimeMillis();
            this.previousTime = this.started;
            
            while(isRunning) {
                
                long start = System.currentTimeMillis();

                //
                evolve(start, this.iterationsPerCycle);
            }
            
            // hack to avoid losing sysou access after setting isRunning to false
            // TODO: research why/how to fix
            System.out.print("");
        }
    }

    @Override
    public void evolve(long start, int iterations) {

        if (isRunning) {
            this.elapsedTime += start - this.previousTime;
            this.iterations += iterations;
            this.previousTime = start;
            
            // evolve
            for (int a = 0; a < iterations; a++) {
                
                
            }
        }
        
        // hack to avoid losing sysou access after setting isRunning to false
        // TODO: research why/how to fix
        System.out.print(""); 
    }
}