package beerware;

import acme.jpm.Encoders.GifEncoder;
import acme.jpm.Encoders.ImageEncoder;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import static beerware.PD.WIDTH_SCALE;

class Exporter extends JDialog {
    private Editor m_editor;
    private JTextArea m_codePreview;
    private ArrayList m_polygons;
    private Preview m_preview;
    private JRadioButton humanReadable;
    private JRadioButton rleData;
    private TreeMap m_holes;
    private String m_holeFile;

    class Preview extends JPanel {
        public static final int VIEWMODE_HUMAN_READABLE = 0;
        public static final int VIEWMODE_RLE_DATA = 1;

        private Image m_offScreen;
        private Editor m_editor;
        private Image m_backDrop;
        private Image m_humanReadable;
        private Image m_rle;
        private int m_raw[];
        private int m_viewMode;

        public Preview(Editor editor) {
            m_editor = editor;
            m_viewMode = VIEWMODE_HUMAN_READABLE;

            int w = m_editor.getBackDrop().getWidth(null) / WIDTH_SCALE;
            int h = m_editor.getBackDrop().getHeight(null);

            m_backDrop = GrayFilter.createDisabledImage(m_editor.getBackDrop());
            m_humanReadable = m_editor.createImage(w, h);

            m_raw = new int[w * h];
            m_rle = m_editor.createImage(w, h);

            MediaTracker mt = new MediaTracker(this);
            mt.addImage(m_backDrop, 0);
            mt.addImage(m_humanReadable, 1);
            mt.addImage(m_rle, 2);
            try {
                mt.waitForAll();
            } catch (InterruptedException e) {
                ;
            }

            Graphics os = m_humanReadable.getGraphics();
            Graphics rle = m_rle.getGraphics();
            os.drawImage(m_backDrop, 0, 0, w, h, null);

            rle.setColor(new Color(255, 255, 0));
            rle.fillRect(0, 0, w, h);

            Iterator i = m_polygons.iterator();
            while (i.hasNext()) {
                Polygon polygon = (Polygon) i.next();
                os.setColor(new Color(255, 255, 96, 96));
                os.fillPolygon(polygon);

                rle.setColor(new Color(0, 255, 0));
                rle.fillPolygon(polygon);

                // Draw edges
                for (int ei = 0; ei < polygon.npoints; ei++) {
                    int x1 = polygon.xpoints[ei];
                    int y1 = polygon.ypoints[ei];
                    int x2 = polygon.xpoints[(ei + 1) % polygon.npoints];
                    int y2 = polygon.ypoints[(ei + 1) % polygon.npoints];
                    int dx = -(x2 - x1) * WIDTH_SCALE;
                    int dy = y2 - y1;
                    float _h = (float) Math.atan2((double) dy, (double) dx);
                    if (_h < 0)
                        _h += Math.PI * 2.0f;

                    float _s = 1.0f;
                    float _v = 1.0f;
                    float rgb[] = hsv2rgb(_h, _s, _v);
                    os.setColor(new Color(rgb[0], rgb[1], rgb[2]));
                    os.drawLine(x2, y2, x1, y1);

                    int c = (int) ((_h / (Math.PI * 2.0f)) * 256.0f + 0.5f);
                    c += 1024;
                    c &= 255;

                    // Flip, rotate and fix... so it gets right for the engine... =)
                    c += 128;
                    c &= 254;

                    rle.setColor(new Color(c, c, c));
                    rle.drawLine(x2, y2, x1, y1);
                }

                // Draw averaged vertices
                for (int ei = 0; ei < polygon.npoints; ei++) {
                    int x1 = polygon.xpoints[ei];
                    int y1 = polygon.ypoints[ei];
                    int x2 = polygon.xpoints[(ei + 1) % polygon.npoints];
                    int y2 = polygon.ypoints[(ei + 1) % polygon.npoints];
                    int x3 = polygon.xpoints[(ei + 2) % polygon.npoints];
                    int y3 = polygon.ypoints[(ei + 2) % polygon.npoints];
                    double dx1 = -(x3 - x2) * WIDTH_SCALE;
                    double dy1 = y3 - y2;
                    double dx2 = -(x2 - x1) * WIDTH_SCALE;
                    double dy2 = y2 - y1;
                    if ((Math.abs(dx1) + Math.abs(dy1) > 0) && (Math.abs(dx2) + Math.abs(dy2) > 0)) {
                        float _h1 = (float) Math.atan2((double) dy1, (double) dx1);
                        if (_h1 < 0)
                            _h1 += Math.PI * 2.0f;
                        float _h2 = (float) Math.atan2((double) dy2, (double) dx2);
                        if (_h2 < 0)
                            _h2 += Math.PI * 2.0f;

                        float _s = 1.0f;
                        float _v = 1.0f;
                        float rgb1[] = hsv2rgb(_h1, _s, _v);
                        float rgb2[] = hsv2rgb(_h2, _s, _v);
                        float rgb[] = new float[3];
                        rgb[0] = (rgb1[0] + rgb2[0]) / 2.0f;
                        rgb[1] = (rgb1[1] + rgb2[1]) / 2.0f;
                        rgb[2] = (rgb1[2] + rgb2[2]) / 2.0f;
                        os.setColor(new Color(rgb[0], rgb[1], rgb[2]));
                        os.fillRect(x2, y2, 1, 1);

                        int c1 = (int) ((_h1 / (Math.PI * 2.0f)) * 256.0f + 0.5f);
                        int c2 = (int) ((_h2 / (Math.PI * 2.0f)) * 256.0f + 0.5f);

                        c1 += 1024;
                        c2 += 1024;
                        c1 &= 255;
                        c2 &= 255;

                        int c = (c1 + c2) / 2 + (Math.abs(c1 - c2) > 128 ? 128 : 0);

                        // Flip, rotate and fix... so it gets right for the engine... =)
                        c += 128;
                        c &= 254;

                        rle.setColor(new Color(c, c, c));
                        rle.fillRect(x2, y2, 1, 1);
                    }
                }
            }

            PixelGrabber pg = new PixelGrabber(m_rle, 0, 0, w, h, m_raw, 0, w);
            try {
                pg.grabPixels();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // AARRGGBB (AA=0xFF=Opaque AA=0x00=Transparent)
            int angles[] = new int[1000]; // x,a,x,a,x,a...
            int nrAngles = 0;
            for (int sy = 0; sy < h; sy++) {
                // Analyse line
                nrAngles = 0;
                for (int sx = 0; sx < w; sx++) {
                    int v = m_raw[sx + sy * w];
                    if (v == 0xff00ff00)  // Solid
                    {

                    } else if (v == 0xffffff00)  // Air
                    {

                    } else                // Edge
                    {
                        angles[nrAngles * 2 + 0] = sx;
                        angles[nrAngles * 2 + 1] = v & 0xfe;
                        nrAngles++;
                    }
                }

                // Angle extend fill lines
                if (nrAngles > 0) {
                    for (int sx = 0; sx < w; sx++) {
                        int v = m_raw[sx + sy * w];
                        if (v == 0xff00ff00)  // Solid
                        {
                            int nearestX = Integer.MAX_VALUE;
                            int angle = 0;
                            for (int l = 0; l < nrAngles; l++) {
                                int x = angles[l * 2 + 0];
                                if (Math.abs(x - sx) < nearestX) {
                                    nearestX = Math.abs(x - sx);
                                    angle = angles[l * 2 + 1];
                                }
                            }
                            angle &= 0xfe;
                            m_raw[sx + sy * w] = 0xff000000 | (angle << 16) | (angle << 8) | angle;
                        }
                    }
                }
            }
            m_rle = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, m_raw, 0, w));
        }

        public void setViewMode(int viewMode) {
            m_viewMode = viewMode;
            repaint();
        }

        public int getViewMode() {
            return m_viewMode;
        }

        public void update(Graphics g) {
            paint(g);
        }

        // 0<=h<2*PI. 0<=r,g,b,s,v<=1
        private float[] hsv2rgb(float h, float s, float v) {
            float rgb[] = new float[3];
            float r = 0, g = 0, b = 0, p, q, f, t;
            int i;

            if (s == 0) {
                rgb[0] = rgb[1] = rgb[2] = 1.0f;
                return rgb;
            }

            h /= Math.PI / 3;
            i = (int) h;
            f = h - i;
            p = v * (1.0f - s);
            q = v * (1.0f - (s * f));
            t = v * (1.0f - (s * (1.0f - f)));

            switch (i) {
                case 0:
                    r = v;
                    g = t;
                    b = p;
                    break;
                case 1:
                    r = q;
                    g = v;
                    b = p;
                    break;
                case 2:
                    r = p;
                    g = v;
                    b = t;
                    break;
                case 3:
                    r = p;
                    g = q;
                    b = v;
                    break;
                case 4:
                    r = t;
                    g = p;
                    b = v;
                    break;
                case 5:
                    r = v;
                    g = p;
                    b = q;
                    break;
            }

            rgb[0] = r;
            rgb[1] = g;
            rgb[2] = b;

            return rgb;
        }

        public void paint(Graphics g) {
            int w = m_backDrop.getWidth(null) / WIDTH_SCALE;
            int h = m_backDrop.getHeight(null);

            if (m_viewMode == VIEWMODE_HUMAN_READABLE)
                g.drawImage(m_humanReadable, 0, 0, w * 2 * WIDTH_SCALE, h * 2, null);
            else if (m_viewMode == VIEWMODE_RLE_DATA)
                g.drawImage(m_rle, 0, 0, w * 2 * WIDTH_SCALE, h * 2, null);
        }

        public Dimension getPreferredSize() {
            return new Dimension(m_backDrop.getWidth(null) * 2, m_backDrop.getHeight(null) * 2);
        }

        public int getLevelWidth() {
            return m_editor.getBackDrop().getWidth(null) / WIDTH_SCALE;
        }

        public int getLevelHeight() {
            return m_editor.getBackDrop().getHeight(null);
        }

        public int[] getRaw() {
            return m_raw;
        }

        public void setRaw(int[] raw, Image i) {
            m_raw = raw;
            m_rle = i;
            setViewMode(VIEWMODE_RLE_DATA);
        }
    }

    private void generateCode(String name, String tableName) {
        StringBuffer sb = new StringBuffer();

        int w = m_preview.getLevelWidth();
        int h = m_preview.getLevelHeight();
        int[] raw = m_preview.getRaw();

        int duplicateTable[] = new int[h];
        int rleData[][] = new int[h][256];
        int rleDataSize[] = new int[h];
        int pointertable[] = new int[h];

        int size = 0;
        int duplicates = 0;

        // Locate duplicates
        for (int row = 0; row < h; row++) {
            for (int x = 0; x < 256; x++)
                rleData[row][x] = -1;
            rleDataSize[row] = 0;
            duplicateTable[row] = row;
        }

        for (int row = 1; row < h; row++) {
            boolean same = false;
            int crow = 0;
            for (crow = 0; crow < row; crow++) {
                same = true;
                for (int x = 0; x < w; x++) {
                    if (raw[x + row * w] != raw[x + crow * w]) {
                        same = false;
                        break;
                    }
                }
                if (same)
                    break;
            }
            if (same) {
                duplicates++;
                duplicateTable[row] = crow;
            }
        }

        // RLE-encode
        for (int row = 0; row < h; row++) {
            if (duplicateTable[row] != row) {
                pointertable[row] = duplicateTable[row];
                continue;
            }

            pointertable[row] = row;

            boolean first = true;
            boolean changeOnLastX = false;

            int lastValue = raw[row * w];
            if (lastValue == 0xffffff00)  // Air
                lastValue = 0x55;
            else
                lastValue = lastValue & 0xff;

            for (int x = 0; x < w; x++) {
                int v = raw[x + row * w];
                if (v == 0xffffff00)  // Air
                    v = 0x55;
                else if ((v & 0x00ff0000) == 0x00ff0000)    // Free value
                    v = v & 0xff;
                else
                    v = v & 0xfe;

                if (v != lastValue) {
                    rleData[row][rleDataSize[row]++] = lastValue;
                    if (x < w - 1)
                        rleData[row][rleDataSize[row]++] = x;
                    else
                        rleData[row][rleDataSize[row]++] = 0xa1;

                    if (x == w - 1)
                        changeOnLastX = true;
                    lastValue = v;
                }
            }
            if (!changeOnLastX) {
                rleData[row][rleDataSize[row]++] = lastValue;
                rleData[row][rleDataSize[row]++] = 0xa1;
            }
        }

        // Make the values relative
        for (int row = 0; row < h; row++) {
            if (duplicateTable[row] == row) {
                for (int i = rleDataSize[row] - 1; i > 3; i -= 2)
                    rleData[row][i] = rleData[row][i] - rleData[row][i - 2];
            }
        }

        // Hole stuffing -------------------------------------------------------------------------------------------------

        readHoleFile();

        // hole2rows holds the resulting rows that where stuffed into the hole denoted by key (key is infact a string of [hole size:labelindex])
        TreeMap hole2rows = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2) {
                String s1 = (String) o1;
                String s2 = (String) o2;
                String split1[];
                String split2[];
                split1 = s1.split(":");
                split2 = s2.split(":");
                int i1 = Integer.parseInt(split1[0]);
                int i2 = Integer.parseInt(split2[0]);
                int l1 = Integer.parseInt(split1[1]);
                int l2 = Integer.parseInt(split2[1]);
                return ((i2 - i1) * 10000) + (l1 - l2);
            }
        });

        // Simble data structor that marks if a row is stuff into a hole or not
        HashSet stuffedRows = new HashSet();

        int totalHoleBytes = 0;
        int totalUsedBytes = 0;
        {
            TreeMap sortedRows = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2) {
                    int i1 = ((Integer) o1).intValue();
                    int i2 = ((Integer) o2).intValue();
                    return i2 - i1;
                }
            });

            // Sort all rows according to size. Map sizes and array of rows.
            for (int row = 0; row < h; row++) {
                if (duplicateTable[row] == row) {
                    Integer rowSize = new Integer(rleDataSize[row]);
                    Integer rowIndex = new Integer(row);

                    ArrayList rows = (ArrayList) sortedRows.get(rowSize);
                    if (rows == null) {
                        rows = new ArrayList();
                        sortedRows.put(rowSize, rows);
                    }
                    rows.add(rowIndex);
                }
            }

/*
			// Print row size distribution
			System.out.println("Row size distribution:");
			Iterator it=sortedRows.keySet().iterator();
			while (it.hasNext())
			{
				Integer rowSize=(Integer)it.next();
				ArrayList rows=(ArrayList)sortedRows.get(rowSize);
				System.out.print("\t"+rowSize+"b: ");
				for (int j=0;j<rows.size();j++)
					System.out.print(rows.get(j)+" ");
				System.out.println();
			}
*/

            // Fill all holes as much as possible
            Iterator holeI = m_holes.keySet().iterator();
            while (holeI.hasNext()) {
                Object holeKey = holeI.next();
                int amount = ((Integer) m_holes.get(holeKey)).intValue();
                int holeSize = ((Integer) holeKey).intValue();

                int used = 0;
                for (int subHole = 0; subHole < amount; subHole++) {
                    String subHoleKey = holeKey + ":" + (amount - subHole - 1);
                    totalHoleBytes += holeSize;
                    int holeSizeLeft = holeSize;
//					System.out.println("Filling hole "+(subHole+1)+"/"+amount+" of size "+holeSize);

                    Iterator rowI = sortedRows.keySet().iterator();
                    while ((rowI.hasNext()) && (holeSizeLeft > 0)) {
                        Object rowKey = rowI.next();
                        int rowSize = ((Integer) rowKey).intValue();
                        if (rowSize > holeSizeLeft)
                            continue;
                        ArrayList rows = (ArrayList) sortedRows.get(rowKey);
                        if (rows.size() == 0)
                            continue;

                        while ((rows.size() > 0) && (rowSize <= holeSizeLeft) && (holeSizeLeft > 0)) {
                            holeSizeLeft -= rowSize;
                            Object row = rows.remove(0);

                            ArrayList rowsInHole = (ArrayList) hole2rows.get(subHoleKey);
                            if (rowsInHole == null) {
                                rowsInHole = new ArrayList();
                                hole2rows.put(subHoleKey, rowsInHole);
                            }
                            rowsInHole.add(row);
                            stuffedRows.add(row);

//							System.out.println("\tFitted row "+row+". size="+rowSize+" leftInHole="+holeSizeLeft);
                            totalUsedBytes += rowSize;
                        }
                    }

                    if (holeSizeLeft != holeSize)
                        used++;
                }

                amount -= used;
                m_holes.put(holeKey, new Integer(amount));
            }

//			System.out.println("Managed to fill "+totalUsedBytes+"/"+totalHoleBytes+" bytes into the holes (holes utilization "+(totalUsedBytes*100/totalHoleBytes)+"%)");
        }

        // Trim last byte if possible of remaining rows
        int trimmedLastBytes = 0;
        for (int row = 0; row < h - 1; row++) {
            if ((duplicateTable[row] == row) && (!stuffedRows.contains(new Integer(row)))) {
                int nextrow = row + 1;
                for (; nextrow < h; nextrow++) {
                    if ((duplicateTable[nextrow] == nextrow) && (!stuffedRows.contains(new Integer(nextrow))))
                        break;
                }

                if (nextrow < h) {
                    if (rleData[nextrow][0] >= rleData[row][rleDataSize[row] - 1]) {
                        rleDataSize[row]--;
                        trimmedLastBytes++;
                    }
                }
            }
        }

        // output source --------------------------------------------------------------------------------------------------

        // Output holes that are left
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(m_holeFile + ".left")));

            Iterator holeI = m_holes.keySet().iterator();
            while (holeI.hasNext()) {
                Object holeKey = holeI.next();
                int amount = ((Integer) m_holes.get(holeKey)).intValue();
                if (amount == 0)
                    continue;
                int holeSize = ((Integer) holeKey).intValue();
                out.println((holeSize / 8) + "," + amount);
            }
            out.close();
            System.out.println("Generated " + m_holeFile + ".left");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Output hole stuffed rows
        Iterator holeI = hole2rows.keySet().iterator();
        while (holeI.hasNext()) {
            String subHoleKey = (String) holeI.next();
            String split[] = subHoleKey.split(":");
            int holeSize = Integer.parseInt(split[0]);
            int labelIndex = Integer.parseInt(split[1]);
            sb.append("*=empty" + holeSize + "bytes" + labelIndex + "\n");

            Iterator rowI = ((ArrayList) hole2rows.get(subHoleKey)).iterator();
            while (rowI.hasNext()) {
                int row = ((Integer) rowI.next()).intValue();
                sb.append(name + row + " .byte ");

                for (int i = 0; i < rleDataSize[row]; i++) {
                    sb.append("$" + Integer.toHexString(rleData[row][i]));
                    if (i < rleDataSize[row] - 1)
                        sb.append(",");
                    if ((i & 1) == 1)
                        sb.append(" ");
                }
                sb.append("\n");

                // Locate duplicate rows to row for conformance in output
                for (int x = row + 1; x < h; x++) {
                    if (duplicateTable[x] == row)
                        sb.append(";row" + x + " is a duplicate of row" + row + "\n");
                }
            }

            sb.append("\n");
        }

        // Output non hole stuffed rows
        sb.append("*=nonstuffedrows\n");
        for (int row = 0; row < h; row++) {
            if (stuffedRows.contains(new Integer(row)))
                continue;

            if (duplicateTable[row] != row) {
                if (!stuffedRows.contains(new Integer(duplicateTable[row])))
                    sb.append(";row" + row + " is a duplicate of row" + duplicateTable[row] + "\n");
                continue;
            }

            sb.append(name + row + " .byte ");
            for (int i = 0; i < rleDataSize[row]; i++) {
                sb.append("$" + Integer.toHexString(rleData[row][i]));
                size++;
                if (i < rleDataSize[row] - 1)
                    sb.append(",");
                if ((i & 1) == 1)
                    sb.append(" ");
            }
            sb.append("\n");
            sb.append(";$" + Integer.toHexString(size) + "\n");
        }
        sb.append("\n");

        // Output pointer table
        sb.append(tableName + "low\n");
        for (int row = 0; row < h; row++) {
            if ((row % 8) == 0)
                sb.append("   .byte ");

            sb.append("<" + name);
            sb.append(pointertable[row]);
            size += 1;

            if ((row % 8) == 7)
                sb.append("\n");
            else
                sb.append(", ");
        }

        sb.append("\n");
        sb.append(tableName + "high\n");
        for (int row = 0; row < h; row++) {
            if ((row % 8) == 0)
                sb.append("   .byte ");
            sb.append(">" + name);
            sb.append(pointertable[row]);
            size += 1;

            if ((row % 8) == 7)
                sb.append("\n");
            else
                sb.append(", ");
        }

        sb.append("\n");
        sb.append(";Duplicate lines = " + duplicates + "\n");
        sb.append(";Trimmed last bytes = " + trimmedLastBytes + "\n");
        sb.append(";Stuffed hole bytes = " + totalUsedBytes + "/" + totalHoleBytes + "\n");
        sb.append(";Total size = " + size + " ($" + Integer.toHexString(size) + ")" + "\n");

        m_codePreview.setText(sb.toString());
    }

    private void exportRaw() {
        try {
            int w = m_preview.getLevelWidth();
            int h = m_preview.getLevelHeight();
            int[] raw = m_preview.getRaw();
            byte out[] = new byte[w * h];
            for (int yp = 0; yp < h; yp++)
                for (int xp = 0; xp < w; xp++) {
                    byte b;
                    if (raw[xp + yp * w] == 0xffffff00)
                        b = 0x55;
                    else if ((raw[xp + yp * w] & 0x00ff0000) == 0x00ff0000)
                        b = (byte) (raw[xp + yp * w] & 0xff);
                    else
                        b = (byte) (raw[xp + yp * w] & 0xfe);
                    out[xp + yp * w] = b;
                }

            FileOutputStream fos = new FileOutputStream("angles.raw");
            fos.write(out);
            fos.close();

            JOptionPane.showConfirmDialog(this, "Into file: angles.raw", "Raw data exported...", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(this, "An exception occurred. Check console for information.", "Raw data exported...", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importAngles() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setFileFilter(new AngleFileFilter());
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            Image i = Toolkit.getDefaultToolkit().getImage(file.getAbsolutePath());
            MediaTracker mt = new MediaTracker(this);
            mt.addImage(i, 0);
            try {
                mt.waitForAll();
            } catch (InterruptedException e) {
                ;
            }

            int w = m_preview.getLevelWidth();
            int h = m_preview.getLevelHeight();
            int[] raw = new int[w * h];
            PixelGrabber pg = new PixelGrabber(i, 0, 0, w, h, raw, 0, w);
            try {
                pg.grabPixels();
            } catch (InterruptedException e) {
                ;
            }
            m_preview.setRaw(raw, i);
            humanReadable.setEnabled(false);
            rleData.doClick();
            m_preview.repaint();
        }
    }

    private void exportAnglesOld() {
        try {
            int w = m_preview.getLevelWidth();
            int h = m_preview.getLevelHeight();
            int[] raw = m_preview.getRaw();
            byte out[] = new byte[w * h];
            for (int yp = 0; yp < h; yp++)
                for (int xp = 0; xp < w; xp++) {
                    byte b;
                    if (raw[xp + yp * w] == 0xffffff00)
                        b = 0x55;
                    else
                        b = (byte) (raw[xp + yp * w] & 0xfe);
                    out[xp + yp * w] = b;
                }

            byte r[], g[], b[];
            r = new byte[256];
            g = new byte[256];
            b = new byte[256];
            for (int i = 0; i < 256; i++) {
                r[i] = b[i] = (byte) 255;
                g[i] = (byte) 0;
            }
            for (int i = 0; i < 256; i += 2)
                r[i] = g[i] = b[i] = (byte) i;
            r[0x55] = (byte) 255;
            g[0x55] = (byte) 255;
            b[0x55] = (byte) 0;

            ColorModel cm = new IndexColorModel(8, 256, r, g, b);
            MemoryImageSource mis = new MemoryImageSource(w, h, cm, out, 0, w);

            JFileChooser chooser = new JFileChooser(".");
            chooser.setFileFilter(new AngleFileFilter());
            int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                FileOutputStream fos = new FileOutputStream(file);
                ImageEncoder ie = new GifEncoder(mis, fos, false);
                ie.encode();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(this, "An exception occurred. Check console for information.", "Exporting data...", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportAngles() {
        try {
            int w = m_preview.getLevelWidth();
            int h = m_preview.getLevelHeight();

            byte r[], g[], b[];
            r = new byte[256];
            g = new byte[256];
            b = new byte[256];
            for (int i = 0; i < 256; i++) {
                r[i] = b[i] = (byte) 255;
                g[i] = (byte) 0;
            }
            for (int i = 0; i < 256; i += 2)
                r[i] = g[i] = b[i] = (byte) i;
            r[0x55] = (byte) 255;
            g[0x55] = (byte) 255;
            b[0x55] = (byte) 0;

            IndexColorModel cm = new IndexColorModel(8, 256, r, g, b);
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, cm);
            WritableRaster raster = image.getRaster();

            int[] raw = m_preview.getRaw();
            for (int yp = 0; yp < h; yp++) {
                for (int xp = 0; xp < w; xp++) {
                    int pixel;
                    if (raw[xp + yp * w] == 0xffffff00)
                        pixel = 0x55;
                    else
                        pixel = raw[xp + yp * w] & 0xfe;
                    raster.setPixel(xp, yp, new int[]{pixel});
                }
            }
            JFileChooser chooser = new JFileChooser(".");
            chooser.setFileFilter(new AngleFileFilter());
            int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                FileOutputStream fos = new FileOutputStream(file);
                ImageIO.write(image, "gif", fos);
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(this, "An exception occurred. Check console for information.", "Exporting data...", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void readHoleFile() {
        m_holes = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2) {
                int i1 = ((Integer) o1).intValue();
                int i2 = ((Integer) o2).intValue();
                return i2 - i1;
            }
        });

        try {
            BufferedReader br = new BufferedReader(new FileReader(m_holeFile));

            while (true) {
                String line = br.readLine();
                if (line == null)
                    break;

                String explode[] = line.split(",");
                int bytes = Integer.parseInt(explode[0]) * 8;
                int number = Integer.parseInt(explode[1]);
                m_holes.put(new Integer(bytes), new Integer(number));
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Exporter(Editor editor, String holeFile) {
        super();
        setTitle("Export...");
        setBackground(Color.lightGray);
        setModal(true);
        setResizable(false);

        m_holeFile = holeFile;

        m_editor = editor;

        m_polygons = new ArrayList();

        // Grab the source polygons, outline them and store.
        ArrayList polygons = m_editor.getPolygons();
        int x[], y[];
        Iterator i = polygons.iterator();
        while (i.hasNext()) {
            ArrayList polygon = (ArrayList) i.next();
            x = new int[polygon.size()];
            y = new int[polygon.size()];

            Iterator j = polygon.iterator();
            int index = 0;
            while (j.hasNext()) {
                Point p = (Point) j.next();
                x[index] = (int) p.getX();
                y[index] = (int) p.getY();
                index++;
            }
            m_polygons.add(Editor.getMCOutline(x, y, 7.5));
        }

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();

        getContentPane().setLayout(gbl);

        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;

        m_preview = new Preview(m_editor);

        JScrollPane jsp1 = new JScrollPane(m_preview, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp1.setBorder(BorderFactory.createTitledBorder("Graphical Output"));
        jsp1.setWheelScrollingEnabled(true);
        jsp1.getViewport().setPreferredSize(new Dimension(640, 400));
        jsp1.getViewport().setMaximumSize(new Dimension(640, 10000));
        //jsp1.getViewport().setMinimumSize(new Dimension(640,0));
        //getContentPane().add(jsp1, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;

        m_codePreview = new JTextArea();
        m_codePreview.setFont(new Font("Courier New", Font.PLAIN, 12));
        m_codePreview.setAutoscrolls(true);
        JScrollPane jsp2 = new JScrollPane(m_codePreview);
        jsp2.setBorder(BorderFactory.createTitledBorder("Code Preview"));
        jsp2.setWheelScrollingEnabled(true);
        jsp2.setPreferredSize(new Dimension(300, 400));
        //getContentPane().add(jsp2, gbc);

        getContentPane().add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jsp1, jsp2), gbc);

        gbc.gridwidth = 3;//GridBagConstraints.RELATIVE;

        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_START;


        JPanel jp = new JPanel();
        jp.setLayout(new FlowLayout(FlowLayout.LEFT));
        jp.setPreferredSize(new Dimension(200, 130));
        jp.setBorder(BorderFactory.createTitledBorder("View Mode"));

        humanReadable = new JRadioButton("Human Readable");
        humanReadable.setSelected(true);
        humanReadable.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                m_preview.setViewMode(Preview.VIEWMODE_HUMAN_READABLE);
            }
        });
        jp.add(humanReadable);
        rleData = new JRadioButton("RLE Data");
        rleData.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                m_preview.setViewMode(Preview.VIEWMODE_RLE_DATA);
            }
        });
        jp.add(rleData);

        ButtonGroup bg = new ButtonGroup();
        bg.add(humanReadable);
        bg.add(rleData);

        JButton exportRaw = new JButton("Save as raw");
        exportRaw.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportRaw();
            }
        });
        jp.add(exportRaw);
        getContentPane().add(jp, gbc);


        JPanel jp2 = new JPanel();
        jp2.setLayout(new FlowLayout(FlowLayout.LEFT));
        jp2.setPreferredSize(new Dimension(300, 130));
        jp2.setBorder(BorderFactory.createTitledBorder("Code generation"));

        JLabel l;

        l = new JLabel("Row names:");
        jp2.add(l);
        final JTextField rownames = new JTextField("row");
        rownames.setPreferredSize(new Dimension(200, 25));
        jp2.add(rownames);

        l = new JLabel("Table name:");
        jp2.add(l);
        final JTextField tablename = new JTextField("rowtable");
        tablename.setPreferredSize(new Dimension(200, 25));
        jp2.add(tablename);

        JButton generate = new JButton("Generate code");
        jp2.add(generate);

        generate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                generateCode(rownames.getText(), tablename.getText());
            }
        });
        getContentPane().add(jp2, gbc);

        JPanel jp3 = new JPanel();
        jp3.setLayout(new FlowLayout(FlowLayout.LEFT));
        jp3.setPreferredSize(new Dimension(180, 130));
        jp3.setBorder(BorderFactory.createTitledBorder("Export/Import angles"));

        JButton exportAngles = new JButton("Export");
        exportAngles.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportAngles();
            }
        });
        jp3.add(exportAngles);

        JButton importAngles = new JButton("Import");
        importAngles.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importAngles();
            }
        });
        jp3.add(importAngles);
        getContentPane().add(jp3, gbc);

        generateCode("row", "rowtable");

        pack();
        setVisible(true);
    }
}
