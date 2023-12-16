package com.rndmodgames.evolver.exporter;

import java.util.List;

import com.rndmodgames.evolver.Triangle;

public class PaintByColorsExporter {

    public PaintByColorsExporter() {}
    
    //
    public static void paintByColors(List<Triangle> drawing) {
        
        int triangleCount = 0;
        
        for (Triangle triangle : drawing) {
            
            System.out.println("TRIANGLE: " + triangleCount);
            
//            System.out.println("XPOLY   : " + triangle.getxPoly());
//            System.out.println("XPOLY   : " + triangle.getyPoly());
            
            System.out.println("COLOR ID: " + triangle.getPalleteColor().getId());
            System.out.println("COLOR   : " + triangle.getPalleteColor().getName());
            
            triangleCount++;
            
            System.out.println("--------------------------------------------------------------------------------");
        }
    }
}