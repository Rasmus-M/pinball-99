/**
 * Classe Vec2.
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Vec2 {
    
    public double x;
    public double y;

    public Vec2() {
    }

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public void set(Vec2 v) {
        this.x = v.x;
        this.y = v.y;
    }
    
    public Vec2 add(Vec2 b) {
        Vec2 r = new Vec2();
        add(this, b, r);
        return r;
    }
    
    public static void add(Vec2 a, Vec2 b, Vec2 r) {
        r.x = a.x + b.x;
        r.y = a.y + b.y;
    }

    public Vec2 sub(Vec2 b) {
        Vec2 r = new Vec2();
        sub(this, b, r);
        return r;
    }
    
    public static void sub(Vec2 a, Vec2 b, Vec2 r) {
        r.x = a.x - b.x;
        r.y = a.y - b.y;
    }

    public void multiply(double s) {
        multiply(s, this, this);
    }
    
    public static void multiply(double s, Vec2 v, Vec2 r) {
        r.x = s * v.x;
        r.y = s * v.y;
    }
    
    public double getSize() {
        return Math.abs(Math.sqrt(x * x + y * y));
    }
    
    // http://stackoverflow.com/questions/722073/how-do-you-normalize-a-zero-vector
    // nesta implementacao, ao tentar normalizar um vetor (0,0)
    // o resultado permanecera um vetor (0,0).
    public void normalize() {
        double s = getSize();
        if (s > 0) {
            multiply(1 / s);
        }
    }

    public static Vec2 newBySizeAngle(double size, double angle) {
       return new Vec2(Math.cos(angle) * size, Math.sin(angle) * size);
    }
    
    public double getAngle() {
        return Math.atan2(y, x);
    }
    
    public void rotate(double angle) {
        double s = Math.sin(angle);
        double c = Math.cos(angle);
        double newX = x * c - y * s;
        double newY = x * s + y * c;
        x = newX;
        y = newY;
    }    

    public double dot(Vec2 v) {
        return x * v.x + y * v.y;
    }
    
    public double getRelativeAngleBetween(Vec2 v) {
        return getSign(v) * Math.acos(dot(v) / (getSize() * v.getSize()));
    }

    // http://www.oocities.org/pcgpe/math2d.html
    // http://gamedev.stackexchange.com/questions/45412/understanding-math-used-to-determine-if-vector-is-clockwise-counterclockwise-f
    public int getSign(Vec2 v) {
        return (y * v.x > x * v.y) ? -1 : 1;
    }
    
    // cria um vetor perpendicular a este
    public Vec2 perp() {
        return new Vec2(-y, x);
    }
    
    // http://johnblackburne.blogspot.com.br/2012/02/perp-dot-product.html
    public double perpDot(Vec2 v) {
        return perp().dot(v);
    }
    
}
