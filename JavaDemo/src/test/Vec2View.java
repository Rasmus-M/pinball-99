package test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;

/**
 *
 * @author leonardo
 */
public class Vec2View extends Vec2 {

    private Color color;
    private Polygon seta = new Polygon();
    
    public Vec2View(Color color) {
        this.color = color;
    }

    public Vec2View(Color color, double x, double y) {
        super(x, y);
        this.color = color;
    }
    
    public void draw(Graphics2D g, int tx, int ty) {
        AffineTransform t = g.getTransform();
        g.translate(tx, ty);
        g.setColor(color);
        g.drawLine(0, 0, (int) x, (int) y);
        
        g.rotate(getAngle());
        seta.reset();
        int size = (int) getSize();
        seta.addPoint(size-10, 4);
        seta.addPoint(size, 0);
        seta.addPoint(size-10, -4);
        g.fillPolygon(seta);
        
        g.setTransform(t);
    }
    
}
