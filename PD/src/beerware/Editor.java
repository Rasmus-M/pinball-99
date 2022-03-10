package beerware;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import static beerware.PD.WIDTH_SCALE;

class Editor extends JPanel {
    private static final int m_version = 1;
    private Image m_offscreen;
    private Image m_backdrop;
    private ArrayList m_polygons;
    private Object m_selectedPolygon;
    private int m_selectedVertex;
    private int m_selectedEdge;
    private boolean m_dragging = false;
    private int m_nx;
    private int m_ny;
    private int m_dragReferenceX;
    private int m_dragReferenceY;
    private int m_zoom;
    private ArrayList m_copyBuffer;
    private File m_file;

    private ArrayList duplicatePolygon(ArrayList polygon) {
        ArrayList newPolygon = new ArrayList();
        Iterator i = polygon.iterator();
        while (i.hasNext()) {
            Point p = (Point) i.next();
            newPolygon.add(new Point((int) p.getX(), ((int) p.getY())));
        }
        return newPolygon;
    }

    public File getFile() {
        return m_file;
    }

    public boolean load(File f) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            try {
                Integer i = (Integer) ois.readObject();
                if (i.intValue() == 1)                // Version 1
                {
                    Object o = ois.readObject();
                    ArrayList level = (ArrayList) o;
                    m_polygons = level;
                }

                m_selectedPolygon = null;
                m_selectedEdge = -1;
                m_selectedVertex = -1;
                m_dragging = false;
                m_file = f;
                repaint();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                ois.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean save(File f) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
            try {
                oos.writeObject(new Integer(m_version));
                oos.writeObject(m_polygons);
                m_file = f;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                oos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public ArrayList getPolygons() {
        return m_polygons;
    }

    public int getZoomLevel() {
        return m_zoom;
    }

    public void setZoomLevel(int zoomLevel) {
        if (m_zoom < 1)
            return;
        m_zoom = zoomLevel;
        m_selectedVertex = -1;
        m_selectedEdge = -1;
        repaint();
    }

    public Editor(String backdrop) {
        super();

        m_file = null;

        m_zoom = 3;

        m_polygons = new ArrayList();
                  /*
        ArrayList polygon=new ArrayList();
            polygon.add(new Point(0,0));
            polygon.add(new Point(50,0));
            polygon.add(new Point(50,100));
            polygon.add(new Point(0,100));
        m_polygons.add(polygon);

        polygon=new ArrayList();
            polygon.add(new Point(50,101));
            polygon.add(new Point(100,101));
            polygon.add(new Point(150,101));
            polygon.add(new Point(100,200));
            polygon.add(new Point(50,200));
        m_polygons.add(polygon);
        */

        m_selectedPolygon = null;
        m_selectedVertex = -1;
        m_selectedEdge = -1;
        m_dragging = false;

        m_backdrop = Toolkit.getDefaultToolkit().getImage(backdrop);

        MediaTracker mt = new MediaTracker(this);
        mt.addImage(m_backdrop, 0);

        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
            ;
        }

        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), "copy");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), "paste");

        getActionMap().put("copy", new AbstractAction("copy") {
            public void actionPerformed(ActionEvent evt) {
                if (m_selectedPolygon != null) {
                    m_copyBuffer = duplicatePolygon((ArrayList) m_selectedPolygon);
                }
            }
        });

        getActionMap().put("paste", new AbstractAction("paste") {
            public void actionPerformed(ActionEvent evt) {
                if (m_copyBuffer != null) {
                    Iterator i = m_copyBuffer.iterator();
                    while (i.hasNext()) {
                        Point p = (Point) i.next();
                        p.translate(10, 5);
                    }
                    m_polygons.add(m_copyBuffer);
                    m_selectedPolygon = m_copyBuffer;
                    m_selectedVertex = -1;
                    m_selectedEdge = -1;
                    m_dragging = false;
                    m_copyBuffer = duplicatePolygon((ArrayList) m_selectedPolygon);
                    repaint();
                }
            }
        });

        getActionMap().put("delete", new AbstractAction("delete") {
            public void actionPerformed(ActionEvent evt) {
                if (m_selectedPolygon != null) {
                    ArrayList polygon = (ArrayList) m_selectedPolygon;

                    if (m_selectedVertex != -1) {
                        polygon.remove(m_selectedVertex);
                        m_selectedVertex = -1;
                        m_selectedEdge = -1;
                        if (polygon.size() < 3) {
                            m_polygons.remove(m_polygons.indexOf(m_selectedPolygon));
                            m_selectedPolygon = null;
                        }
                        repaint();
                        return;
                    }

                    if (JOptionPane.showConfirmDialog(Editor.this.getParent(), "Are you sure?", "Delete polygon", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                        m_polygons.remove(m_polygons.indexOf(m_selectedPolygon));
                        m_selectedPolygon = null;
                        m_selectedVertex = -1;
                        m_selectedEdge = -1;
                        repaint();
                        return;
                    }
                }
            }
        });

        setRequestFocusEnabled(true);
        requestFocus();

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                requestFocus();
            }

            public void mousePressed(MouseEvent e) {
                int mx = e.getX() / WIDTH_SCALE / m_zoom;
                int my = e.getY() / m_zoom;
                requestFocus();

                if (m_selectedVertex != -1) {
                    m_dragging = true;
                    repaint();
                } else {
                    if (m_selectedPolygon != null) {
                        Polygon poly = new Polygon();
                        Iterator j = ((ArrayList) m_selectedPolygon).iterator();
                        while (j.hasNext()) {
                            Point p = (Point) j.next();
                            poly.addPoint((int) p.getX(), (int) p.getY());
                        }

                        if (poly.contains(mx, my)) {
                            m_dragging = true;
                            m_dragReferenceX = mx;
                            m_dragReferenceY = my;
                            repaint();
                        }
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (m_selectedVertex != -1) {
                    m_selectedEdge = -1;
                    m_dragging = false;
                    repaint();
                }
            }

            public void mouseClicked(MouseEvent e) {
                int mx = e.getX() / WIDTH_SCALE / m_zoom;
                int my = e.getY() / m_zoom;

                if (e.getClickCount() == 1) {
                    if ((m_selectedEdge != -1) && (m_selectedPolygon != null)) {
                        ArrayList al = (ArrayList) m_selectedPolygon;
                        m_selectedEdge = (m_selectedEdge + 1) % al.size();
                        al.add(m_selectedEdge, new Point(m_nx, m_ny));

                        m_selectedVertex = m_selectedEdge;
                        m_selectedEdge = -1;
                        m_dragging = false;
                        repaint();
                        return;
                    }

                    Iterator i = m_polygons.iterator();
                    while (i.hasNext()) {
                        ArrayList polygon = (ArrayList) i.next();
                        Polygon poly = new Polygon();
                        Iterator j = polygon.iterator();
                        while (j.hasNext()) {
                            Point p = (Point) j.next();
                            poly.addPoint((int) p.getX(), (int) p.getY());
                        }

                        if (poly.contains(mx, my)) {
                            m_selectedPolygon = polygon;
                            m_selectedVertex = -1;
                            m_selectedEdge = -1;
                            m_dragging = false;
                            repaint();
                            return;
                        }
                    }

                    m_selectedPolygon = null;
                    m_selectedVertex = -1;
                    m_selectedEdge = -1;
                    m_dragging = false;
                    repaint();
                } else if (e.getClickCount() == 2) {
                    ArrayList newPoly = new ArrayList();
                    newPoly.add(new Point(mx, my - 20));
                    newPoly.add(new Point(mx + 20, my + 20));
                    newPoly.add(new Point(mx - 20, my + 20));
                    m_polygons.add(newPoly);
                    m_selectedPolygon = newPoly;
                    m_selectedVertex = -1;
                    m_selectedEdge = -1;
                    m_dragging = false;
                    repaint();
                    return;
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (!m_dragging)
                    return;

                if (m_selectedVertex != -1) {
                    int mx = e.getX() / WIDTH_SCALE / m_zoom;
                    int my = e.getY() / m_zoom;

                    ArrayList polygon = (ArrayList) m_selectedPolygon;
                    Point point = (Point) polygon.get(m_selectedVertex);
                    point.move(mx, my);

                    repaint();
                } else {
                    if (m_selectedPolygon != null) {
                        int mx = e.getX() / WIDTH_SCALE / m_zoom;
                        int my = e.getY() / m_zoom;
                        int dx = mx - m_dragReferenceX;
                        int dy = my - m_dragReferenceY;

                        ArrayList polygon = (ArrayList) m_selectedPolygon;
                        Iterator i = polygon.iterator();
                        while (i.hasNext()) {
                            Point p = (Point) i.next();
                            p.translate(dx, dy);
                        }

                        m_dragReferenceX = mx;
                        m_dragReferenceY = my;

                        repaint();
                    }
                }
            }

            public void mouseMoved(MouseEvent e) {
                double mx = e.getX() / WIDTH_SCALE / m_zoom;
                double my = e.getY() / m_zoom;
                mx *= WIDTH_SCALE;
                boolean redraw = false;

                m_dragging = false;

                if (m_selectedPolygon != null) {
                    // Find closest vertex
                    int selectedIndex = -1;
                    double minRadius = Double.MAX_VALUE;
                    int index = 0;

                    if (!e.isControlDown()) {
                        Iterator j = ((ArrayList) m_selectedPolygon).iterator();
                        while (j.hasNext()) {
                            Point p = (Point) j.next();
                            double x = p.getX() * WIDTH_SCALE;
                            double y = p.getY();
                            double r = (x - mx) * (x - mx) + (y - my) * (y - my);
                            if (r < minRadius) {
                                minRadius = r;
                                selectedIndex = index;
                            }
                            index++;
                        }

                        if (minRadius > 10 * 10)
                            selectedIndex = -1;

                        if (selectedIndex != m_selectedVertex) {
                            m_selectedVertex = selectedIndex;
                            redraw = true;
                        }
                    } else
                        m_selectedVertex = -1;


                    // Find closest edge
                    selectedIndex = -1;
                    minRadius = Double.MAX_VALUE;
                    ArrayList al = (ArrayList) m_selectedPolygon;
                    for (index = 0; index < al.size(); index++) {
                        Point p1 = (Point) al.get(index);
                        Point p2 = (Point) al.get((index + 1) % al.size());
                        double x1 = p1.getX() * WIDTH_SCALE;
                        double y1 = p1.getY();
                        double x2 = p2.getX() * WIDTH_SCALE;
                        double y2 = p2.getY();
                        double ux = mx - x1;
                        double uy = my - y1;
                        double vx = x2 - x1;
                        double vy = y2 - y1;
                        double vl = Math.sqrt(vx * vx + vy * vy);
                        if (vl != 0) {
                            ux /= vl;
                            uy /= vl;
                            vx /= vl;
                            vy /= vl;
                            double s = ux * vx + uy * vy;
                            if ((s >= 0) && (s <= 1)) {
                                double va = -vy;
                                double vb = vx;
                                double vc = -(va * x1 + vb * y1);
                                double d = Math.abs(va * mx + vb * my + vc);

                                if ((d < minRadius) && (d < 10)) {
                                    m_nx = (int) ((x1 + s * (x2 - x1)) / WIDTH_SCALE + 0.5);
                                    m_ny = (int) (y1 + s * (y2 - y1) + 0.5);
                                    minRadius = d;
                                    selectedIndex = index;
                                }
                            }
                        }
                    }

                    if (minRadius > 10)
                        selectedIndex = -1;

//					if (selectedIndex!=m_selectedEdge)
//					{
                    m_selectedEdge = selectedIndex;
                    redraw = true;
//					}

                    if (redraw) {
                        repaint();
                    }
                }
            }
        });
    }

    public Image getBackDrop() {
        return m_backdrop;
    }

    public Dimension getPreferredSize() {
        return new Dimension(m_backdrop.getWidth(null) * m_zoom, m_backdrop.getHeight(null) * m_zoom);
    }

    public void update(Graphics g) {
        paint(g);
    }

    public static Polygon getMCOutline(int[] ix, int[] iy, double r) {
        ArrayList output = new ArrayList();

        for (int i = 0; i < ix.length; i++) {
            double x1 = ix[i % ix.length] * WIDTH_SCALE;
            double y1 = iy[i % ix.length];
            double x2 = ix[(i + 1) % ix.length] * WIDTH_SCALE;
            double y2 = iy[(i + 1) % ix.length];
            double x3 = ix[(i + 2) % ix.length] * WIDTH_SCALE;
            double y3 = iy[(i + 2) % ix.length];

            double ux = x2 - x1;
            double uy = y2 - y1;
            double vx = x3 - x2;
            double vy = y3 - y2;
            double ul = Math.sqrt(ux * ux + uy * uy);
            double vl = Math.sqrt(vx * vx + vy * vy);
            ux /= ul;
            uy /= ul;
            vx /= vl;
            vy /= vl;
            double cosa = ux * vx + uy * vy;

            double ua = -uy;
            double ub = ux;
            double uc = -(ua * x1 + ub * y1);
            double va = -vy;
            double vb = vx;
            double vc;

            double d = ua * x3 + ub * y3 + uc;    // Orthogonal distance to line

            // Stroke lines
            double ox1 = x1 - ua * r;
            double oy1 = y1 - ub * r;
            double ox2 = x2 - ua * r;
            double oy2 = y2 - ub * r;
            double ox3 = x2 - va * r;
            double oy3 = y2 - vb * r;
            double ox4 = x3 - va * r;
            double oy4 = y3 - vb * r;

            //output.addPoint((int)(ox1)/2,(int)oy1);

            if (d > 0)            // Convex corner, add circle
            {
                output.add(new Point2D.Double(ox2 / WIDTH_SCALE, oy2));

                ux = ox2 - x2;
                uy = oy2 - y2;
                vx = ox3 - x2;
                vy = oy3 - y2;
                ux /= r;
                uy /= r;
                vx /= r;
                vy /= r;

                // Calculate begin angle
                double bega, enda;
                if (ux > 0)
                    bega = -Math.asin(uy);
                else
                    bega = Math.asin(uy) - Math.PI;

                // Calculate end angle
                enda = -Math.asin(vy);
                if (vx > 0)
                    enda = -Math.asin(vy);
                else
                    enda = Math.asin(vy) - Math.PI;

                // Draw arc
                double lx = ox2 / WIDTH_SCALE;
                double ly = oy2;

                if (bega < enda)
                    enda -= Math.PI * 2;

                double step = (enda - bega) / 13.0;
                for (int ii = 0; ii < 13; ii++) {
                    bega += step;
                    double x = (x2 + Math.cos(bega) * r) / WIDTH_SCALE;
                    double y = y2 - Math.sin(bega) * r;
                    output.add(new Point2D.Double(x, y));
                    lx = x;
                    ly = y;
                }
            } else                // Concave corner, clip lines
            {
                uc = -(ua * ox1 + ub * oy1);
                vc = -(va * ox3 + vb * oy3);

                double tmp = (ub * va - ua * vb);
                if (tmp != 0) {
                    double y = (ua * vc - uc * va) / tmp;
                    double x;
                    if (ua != 0)
                        x = -(ub * y + uc) / ua;
                    else
                        x = -(vb * y + vc) / va;
                    x /= WIDTH_SCALE;
                    output.add(new Point2D.Double(x, y));
                }
            }
        }

        // Remove interal self-intersecting loops
        for (int i = 0; i < output.size(); i++) {
            Point2D.Double a_beg = (Point2D.Double) output.get(i);
            Point2D.Double a_end = (Point2D.Double) output.get((i + 1) % output.size());

            double x = 0, y = 0;
            boolean intersect = false;
            int j;
            // Scan forward from i+2 and the whole revolution
            for (j = i + 2; j < i + 1 + output.size(); j++) {
                Point2D.Double b_beg = (Point2D.Double) output.get(j % output.size());
                Point2D.Double b_end = (Point2D.Double) output.get((j + 1) % output.size());

                double x1 = a_beg.getX();
                double y1 = a_beg.getY();
                double x2 = a_end.getX();
                double y2 = a_end.getY();
                double x3 = b_beg.getX();
                double y3 = b_beg.getY();
                double x4 = b_end.getX();
                double y4 = b_end.getY();
                double numerator1 = (x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3);
                double numerator2 = (x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3);
                double denominator = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
                if ((denominator != 0) && (numerator1 != 0) && (numerator2 != 0)) {
                    double t1 = numerator1 / denominator;
                    double t2 = numerator2 / denominator;
                    if ((t1 >= 0) && (t1 <= 1) && (t2 >= 0) && (t2 <= 1)) {
                        x = x1 + t1 * (x2 - x1);
                        y = y1 + t1 * (y2 - y1);
                        intersect = true;
                        break;
                    }
                }
            }

            if (intersect) {
                int verticesToRemoveForward = j - i;
                int verticesToRemoveBackward = (i + output.size()) - j;
                int verticesToRemove = verticesToRemoveForward;
//                System.out.println("removing "+verticesToRemoveForward+"/"+verticesToRemoveBackward+" i="+(i%output.size())+" j="+(j%output.size()));

                if (verticesToRemoveForward > verticesToRemoveBackward) {
                    i = j;
                    verticesToRemove = verticesToRemoveBackward;
                }

                for (int l = 0; l < verticesToRemove; l++)
                    output.remove((i + 1) % output.size());
                output.add((i + 1) % output.size(), new Point2D.Double(x, y));

                // Restart i-loop
                i = -1;
            }
        }

        // Output polygon and remove duplicate vertices.
        Polygon polygon = new Polygon();
        int lastx = -100, lasty = -100;
        for (int i = 0; i < output.size(); i++) {
            Point2D.Double point = (Point2D.Double) output.get(i);
            int x = (int) (point.getX() + 1);
            int y = (int) (point.getY() + 0.5);
            if ((x != lastx) || (y != lasty)) {
                double d = Math.sqrt((x - lastx) * (x - lastx) + (y - lasty) * (y - lasty));
                if (d >= 3.0) {
                    polygon.addPoint(x, y);
                    lastx = x;
                    lasty = y;
                }
            }
        }

        return polygon;
    }

    public void paint(Graphics g) {
        int w = m_backdrop.getWidth(null) / WIDTH_SCALE;
        int h = m_backdrop.getHeight(null);

        if (m_offscreen == null)
            m_offscreen = createImage(w, h);

        Graphics os = m_offscreen.getGraphics();

        os.drawImage(m_backdrop, 0, 0, w, h, null);

        int x[], y[];

        Iterator i;
        i = m_polygons.iterator();
        os.setColor(new Color(255, 255, 255, 255));
        while (i.hasNext()) {
            ArrayList polygon = (ArrayList) i.next();
            if (polygon == m_selectedPolygon) {
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

                Polygon outline = getMCOutline(x, y, 7.5);
                //os.fillPolygon(outline);
                os.drawPolygon(outline);
            }
        }

        i = m_polygons.iterator();
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

            if (polygon == m_selectedPolygon)
                os.setColor(new Color(255, 255, 96, 128));
            else
                os.setColor(new Color(255, 255, 96, 96));
            os.fillPolygon(x, y, x.length);

            if (polygon == m_selectedPolygon) {
                //os.setColor(new Color(255,255,255));
                //os.drawPolygon(x,y,x.length);
                os.setColor(new Color(0, 0, 255));
                for (int k = 0; k < x.length; k++) {
                    if ((k == m_selectedEdge) && (!m_dragging)) {
                        os.setColor(new Color(255, 255, 0));
                        os.drawLine(x[k], y[k], x[(k + 1) % x.length], y[(k + 1) % x.length]);
                        os.setColor(new Color(0, 0, 255));
                    }

                    if ((k == m_selectedVertex) && (!m_dragging))
                        os.fillRect(x[k] - 1, y[k] - 1, 3, 3);
                    else
                        os.fillRect(x[k], y[k], 1, 1);
                }

                if ((m_selectedEdge != -1) && (!m_dragging)) {
                    os.setColor(Color.BLACK);
                    os.fillRect(m_nx, m_ny, 1, 1);
                }
            }
            //else
            //	os.drawPolygon(x,y,x.length);
        }

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.drawImage(m_offscreen, 0, 0, w * m_zoom * WIDTH_SCALE, h * m_zoom, null);
    }
}
