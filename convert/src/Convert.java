import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Convert {

    public static void main(String[] args) throws IOException {
        JFileChooser fileChooser = new JFileChooser(".");
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().endsWith("png") || f.getName().endsWith("gif");
            }
            @Override
            public String getDescription() {
                return "Image files";
            }
        });
        int result = fileChooser.showOpenDialog(null);
        if (result == JOptionPane.YES_OPTION) {
            File file = fileChooser.getSelectedFile();
            BufferedImage image = ImageIO.read(file);
            byte[] raw = new byte[image.getHeight() * image.getWidth()];
            int i = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    // Add 1 to wrap around because 255 is empty background
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
            String outputFilename = "..\\bin\\" + file.getName().replaceAll("\\.(png|gif)", ".bin");
            FileOutputStream fos = new FileOutputStream(outputFilename);
            fos.write(rom);
            fos.close();
        }
    }
}
