package beerware;

import javax.swing.filechooser.FileFilter;
import java.io.File;

class RawFileFilter extends FileFilter {
    public String getDescription() {
        return "(.pd64raw) Pinball Dreams 64 - Raw files";
    }

    public boolean accept(File f) {
        if (!f.isFile())
            return true;

        return f.getName().endsWith(".pd64raw");
    }
}
