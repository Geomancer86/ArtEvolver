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
    
    public long getGoodIterations() {
        
        return goodIterations;
    }
    
    @Override
    public void run() {
        // TODO Auto-generated method stub
        
        System.out.println("Start new LibGDXEvolver thread!");
        
        this.started = System.currentTimeMillis();
        
        while(true) {

            while(isRunning) {
                
//                long start = System.currentTimeMillis();

//                evolve(start, this.iterationsPerCycle);
                evolve(1, this.iterationsPerCycle);
            }
            
            // hack to avoid losing sysou access after setting isRunning to false
            // TODO: research why/how to fix
//            System.out.print("");
        }
    }

    @Override
    public void evolve(long start, int iterations) {

//        this.elapsedTime += start - this.started;
        this.iterations += iterations;
        
//        System.out.println("elapsed: " + ((float) this.elapsedTime / 1000000f) + ", iterations: " + this.iterations);
        
        // evolve
//        for (int a = 0; a < iterations; a++) {
//            
//            
//        }
        
//        System.out.println(toString());
    }

//    @Override
//    public String toString() {
//        return "LibGDXEvolver [threadID=" + threadID + ", isStarted=" + isStarted + ", isRunning=" + isRunning
//                + ", started=" + started + ", elapsedTime=" + elapsedTime + ", iterations=" + iterations + ", "
//                + (getClass() != null ? "getClass()=" + getClass() + ", " : "") + "hashCode()=" + hashCode() + ", "
//                + (super.toString() != null ? "toString()=" + super.toString() : "") + "]";
//    }
}