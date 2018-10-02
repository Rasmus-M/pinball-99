package beerware;

import javax.swing.filechooser.FileFilter;
import java.io.File;

class AngleFileFilter extends FileFilter {
    public String getDescription() {
        return "(.gif) GIF Images - Angular raw data";
    }

    public boolean accept(File f) {
        if (!f.isFile())
            return true;

        return f.getName().endsWith(".gif");
    }
}
