import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Convert {

    public static void main(String[] args) throws IOException {
        new Convert().run();
    }

    private void run() throws IOException {
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
            byte[] raw = readFile(file);
            File specialFile = new File("./" + getBaseFilename(file) + "-special.png");
            byte[] special = specialFile.exists() ? readSpecialFile(specialFile) : null;
            int bankSize = 0x2000;
            int headerSize = 0x0000;
            int dataSize = 0x2000;
            int banks = (raw.length / dataSize) + (raw.length % dataSize == 0 ? 0 : 1);
            byte[] rom = new byte[banks * bankSize];
            int i = 0;
            for (int b = 0; b < banks; b++) {
                for (int j = 0; j < dataSize; j++) {
                    byte value = i < raw.length ? (byte) (raw[i] | (special != null ? special[i] : 0)) : 0;
                    rom[b * bankSize + headerSize + j] = value;
                    i++;
                }
            }
            String outputFilename = "..\\bin\\" + getBaseFilename(file) + ".bin";
            FileOutputStream fos = new FileOutputStream(outputFilename);
            fos.write(rom);
            fos.close();
        }
    }

    private String getBaseFilename(File file) {
        return file.getName().split("\\.")[0];
    }

    private byte[] readFile(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        byte[] raw = new byte[image.getHeight() * image.getWidth()];
        int i = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xff; // 0, 2, 4, 6 ... 254, 255 = empty
                int value = red == 255 ? 0 : (red / 2) + 1;
                if (value == 128) {
                    value = 127;
                }
                raw[i++] = (byte) value;
            }
        }
        return raw;
    }

    private byte[] readSpecialFile(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        byte[] raw = new byte[image.getHeight() * image.getWidth()];
        int i = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int value = (rgb & 0x00ffffff) == 0x00ff00ff ? 128 : 0;
                raw[i++] = (byte) value;
            }
        }
        return raw;
    }
}
