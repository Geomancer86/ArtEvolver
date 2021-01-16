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
        
        invalidate();
    }

    @Override
    public void invalidate() {

        if (actorCell != null && this.getHeight() > 0 && actorAspectRatio > 0) {
            
            float containerAspectRatio = this.getWidth() / this.getHeight();
            
            if (containerAspectRatio >= actorAspectRatio) {
                // too wide (or equal): match the height!
                actorCell.width(this.getHeight() * actorAspectRatio).height(this.getHeight());
            } else {
                // too tall: match the width!
                actorCell.width(this.getWidth()).height(this.getWidth() / actorAspectRatio);
            }
        }

        super.invalidate();
    }
}