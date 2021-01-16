package com.rndmodgames.artevolver;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * Simple container that sizes a widget to fit within its height and width,
 *  while maintaining the widget's original aspect ratio.
 */
public class AspectRatioFitter<T extends Actor> extends VisTable {

    private float actorAspectRatio;
    private Cell<T> actorCell;

    public AspectRatioFitter(T actor) {
        
        super();
        
        actorAspectRatio = actor.getWidth() / actor.getHeight();
        actorCell = this.add(actor);
    }

    @Override
    public void invalidate() {

        System.out.println("this.getHeight(): " + this.getHeight());
        System.out.println("this.getWidth() : " + this.getWidth());

        if (actorCell != null && this.getHeight() > 0 && actorAspectRatio > 0) {
            
            float containerAspectRatio = this.getWidth() / this.getHeight();
            
            System.out.println("containerAspectRatio: " + containerAspectRatio);
            System.out.println("actorAspectRatio    : " + actorAspectRatio);
            
//            if (this.getWidth() > this.getHeight()) {
//            
//                
//                
//            } else {
                
//                actorCell.width(1280-160).height(this.getWidth() / actorAspectRatio).grow();
//            }
            
            
            
            
            if (containerAspectRatio > actorAspectRatio) {
                
                // too wide (or equal): match the height!
                System.out.println("too wide (or equal): match the height!");
                actorCell.fillX().height(this.getHeight());
                
            } else {
                
                // too tall: match the width!
                System.out.println("too tall: match the width!");
                actorCell.fillY().height(this.getWidth() / actorAspectRatio);
                
            } 
//            else {
//                
//                System.out.println("FIX THIS RATIO");
//                
//                /**
//                 * 
//                 */
//                if (actorCell.getMinHeight() > actorCell.getMinWidth()) {
//                    
//                    System.out.println("STRETCH HEIGHT");
//                    actorCell.growX().growY();
//                    
//                } else {
                    
//                    System.out.println("STRETCH WIDTH");
//                    actorCell.width(1024-160).height(this.getWidth() / actorAspectRatio).grow();
//                }
//            }
        }
        
        if (actorCell != null) {
            System.out.println("actorCell.getHeight() : " + actorCell.getMinHeight());
            System.out.println("actorCell.getWidth() : " + actorCell.getMinWidth());
        }

        super.invalidate();
    }
}