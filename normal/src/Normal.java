public class Normal {

    public static void main(String[] args) {

        for (int angle = 0; angle < 255; angle += 2) {
            Vec2 normal = new Vec2(0, -50);
            double a = (2 * Math.PI) * (angle / 255d);
            normal.rotate(-a);
            normal.normalize();
            int deg = (int) Math.round(a * 360.0 / (2 * Math.PI));
            System.out.println("       data " + hex(normal.x) + "," + hex(normal.y) + "                ; " + deg + "º");
        }
    }

    private static String hex(double d) {
        int i = (int) Math.round(d * 0x0100);
        String hex = Integer.toHexString(i);
        if (hex.length() > 4) {
            hex = hex.substring(hex.length() - 4);
        } else {
            while (hex.length() < 4) {
                hex = "0" + hex;
            }
        }
        return ">" + hex;
    }
}
