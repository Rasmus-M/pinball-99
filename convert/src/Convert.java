import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Convert {

    public static void main(String[] args) throws IOException {
        BufferedImage image = ImageIO.read(new File("angles.png"));
        byte[] raw = new byte[image.getHeight() * image.getWidth()];
        int i = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                raw[i++] = (byte) (((rgb >> 16) + 1) & 0xff);
            }
        }
        int bankSize = 0x2000;
        int headerSize = 0x0000;
        int dataSize = 0x2000;
        int banks = (raw.length / dataSize) + (raw.length % dataSize == 0 ? 0 : 1);
        byte[] rom = new byte[banks * bankSize];
        i = 0;
        for (int b = 0; b < banks; b++) {
            for (int j = 0; j < dataSize; j++) {
                rom[b * bankSize + headerSize + j] = i < raw.length ? raw[i++] : 0;
            }
        }
        FileOutputStream fos = new FileOutputStream("coll.bin");
        fos.write(rom);
        fos.close();
    }
}
