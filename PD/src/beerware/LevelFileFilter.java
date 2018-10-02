package beerware;

import javax.swing.filechooser.FileFilter;
import java.io.File;

class LevelFileFilter extends FileFilter {
    public String getDescription() {
        return "(.pd64) Pinball Dreams 64 - Level files";
    }

    public boolean accept(File f) {
        if (!f.isFile())
            return true;

        return f.getName().endsWith(".pd64");
    }
}
