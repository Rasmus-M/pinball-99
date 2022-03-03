package beerware;/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * JackAsser wrote this file. As long as you retain this notice you
 * can do whatever you want with this stuff. If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return, JackAsser.
 * ----------------------------------------------------------------------------
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class PD extends JFrame {

    public final static int WIDTH_SCALE = 1;

    private Action m_actionLoad;
    private Action m_actionSave;
    private Action m_actionSaveAs;
    private Action m_actionExport;
    private Action m_actionExit;
    private Editor m_editor;
    private static final String m_baseTitle = "Pinball Dreams (c64) Wall beerware.Editor";
    private String m_holeFile;

    private void exit() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure?", "Exit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            setVisible(false);
            dispose();
            System.exit(0);
        }
    }

    public PD(String holeFile) {
        super(m_baseTitle);

        m_holeFile = holeFile;

        setResizable(true);
        getContentPane().setLayout(new BorderLayout());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        setTitle(m_baseTitle + " - <new level>");

        m_actionLoad = new AbstractAction("Load...") {
            public void actionPerformed(ActionEvent evt) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new LevelFileFilter());
                int returnVal = chooser.showOpenDialog(PD.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    if (m_editor.load(chooser.getSelectedFile())) {
                        setTitle(m_baseTitle + " - " + m_editor.getFile().getAbsolutePath());
                    }
                }
            }
        };

        m_actionSave = new AbstractAction("Save...") {
            public void actionPerformed(ActionEvent evt) {
                if (m_editor.getFile() != null) {
                    if (m_editor.save(m_editor.getFile())) {
                        setTitle(m_baseTitle + " - " + m_editor.getFile().getAbsolutePath());
                    }
                } else
                    m_actionSaveAs.actionPerformed(evt);
            }
        };

        m_actionSaveAs = new AbstractAction("Save As...") {
            public void actionPerformed(ActionEvent evt) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new LevelFileFilter());
                int returnVal = chooser.showSaveDialog(PD.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    if (m_editor.save(chooser.getSelectedFile())) {
                        setTitle(m_baseTitle + " - " + m_editor.getFile().getAbsolutePath());
                    }
                }
            }
        };

        m_actionExport = new AbstractAction("Export...") {
            public void actionPerformed(ActionEvent evt) {
                Exporter exporter = new Exporter(m_editor, m_holeFile);
            }
        };

        m_actionExit = new AbstractAction("Exit") {
            public void actionPerformed(ActionEvent evt) {
                exit();
            }
        };

        m_editor = new Editor("bitmap.gif");
        JScrollPane jsp = new JScrollPane(m_editor);
        jsp.setWheelScrollingEnabled(true);
        jsp.getViewport().setPreferredSize(new Dimension(960, 800));
        getContentPane().add(jsp, BorderLayout.CENTER);

        m_editor.load(new File("level.pd64"));

        m_editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK), "load");
        m_editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK), "save");
        m_editor.getActionMap().put("load", m_actionLoad);
        m_editor.getActionMap().put("save", m_actionSave);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(new JMenuItem(m_actionLoad));
        file.add(new JMenuItem(m_actionSave));
        file.add(new JMenuItem(m_actionSaveAs));
        file.add(new JMenuItem(m_actionExport));
        file.add(new JSeparator());
        file.add(new JMenuItem(m_actionExit));
        mb.add(file);
        //JMenu edit=new JMenu("Edit");
        //    edit.add(new JMenuItem("Bitmap..."));
        //mb.add(edit);
        topPanel.add(mb, BorderLayout.NORTH);

        JToolBar tb = new JToolBar();
        tb.add(m_actionLoad);
        tb.add(m_actionSave);
        tb.add(new JToolBar.Separator());

        /*
        JComboBox editMode = new JComboBox();
        editMode.addItem("Polygons");
        editMode.addItem("Lines");
        editMode.setMaximumSize(new Dimension(80, 1000));
        tb.add(editMode);
        */

        final JComboBox zoomLevel = new JComboBox();
        zoomLevel.addItem("100%");
        zoomLevel.addItem("200%");
        zoomLevel.addItem("300%");
        zoomLevel.addItem("400%");
        zoomLevel.addItem("500%");
        zoomLevel.addItem("600%");
        zoomLevel.addItem("700%");
        zoomLevel.addItem("800%");
        zoomLevel.setMaximumSize(new Dimension(80, 1000));
        zoomLevel.setSelectedIndex(m_editor.getZoomLevel() - 1);
        tb.add(zoomLevel);

        zoomLevel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                m_editor.setZoomLevel(zoomLevel.getSelectedIndex() + 1);
            }
        });

        tb.setFloatable(false);
        topPanel.add(tb, BorderLayout.CENTER);
        getContentPane().add(topPanel, BorderLayout.NORTH);

        pack();
        setVisible(true);
        toFront();
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java beerware.PD holefile");
            System.exit(-1);
        }

        Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 1);
        new PD(args[0]);
    }
}
