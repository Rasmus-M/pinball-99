package test;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 *
 * @author leonardo
 */
public class Ball {

    private View view;
    public Vec2 position = new Vec2(150, 330);
    public Vec2 velocity = new Vec2(-2, 3);
    private Vec2 gravity = new Vec2(0, 0.05);

    public Ball(View view) {
        this.view = view;
    }
    
    public void update() {
        gravity.set(0, 0.05);
        
        if (!checkCollision((int) position.x, (int) position.y)) {
            velocity = velocity.add(gravity);

            Vec2 positionTmp = position.add(new Vec2());
            for (double p=0; p<=1; p+=0.1) {
                Vec2 velocityTmp = velocity.add(new Vec2());
                velocityTmp.multiply(p);
                position = positionTmp.add(velocityTmp);
                if (checkCollision((int) position.x, (int) position.y)) {
                    break;
                }
            }
        }
        else {
            // calcula a reflexao reflex=incident ? 2*normal.dot(Incident)*normal;
            // http://www.pontov.com.br/site/index.php/matematica-e-fisica/332-vetores-guia-de-referencia
            Vec2 normal = view.calculateNormal((int) position.x, (int) position.y);

            normal.normalize();
            double gravityIntensity = gravity.dot(normal.perp());
            gravity = normal.perp();
            gravity.multiply(gravityIntensity);
            velocity = velocity.add(gravity);
            
            if (velocity.dot(normal) < 0) {
                normal.multiply(2 * velocity.dot(normal));
                velocity = velocity.sub(normal);
            }
            
            velocity.multiply(0.7);
            
            while (checkCollision((int) position.x, (int) position.y)) {
                normal = view.calculateNormal((int) position.x, (int) position.y);
                normal.normalize();
                //normal = velocity.add(new Vec2());
                //normal.normalize();
                position = position.add(normal);
            }
            
        }
    }

    public void setVelocity(double x, double y) {
        velocity.x += x;
        velocity.y += y;
    }
    
    public void draw(Graphics2D g) {
        g.setColor(Color.RED);
        g.fillOval((int) (position.x - 8), (int) (position.y - 8), 16, 16);
    }
    
    public boolean checkCollision(int x, int y) {
        return view.checkCollisionBallTable(x, y);
    }

}
