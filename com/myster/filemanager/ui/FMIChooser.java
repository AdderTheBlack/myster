/* 

 Title:			Myster Open Source
 Author:			Andrew Trumper
 Description:	Generic Myster Code
 
 This code is under GPL

 Copyright Andrew Trumper 2000-2001
 */

package com.myster.filemanager.ui;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Hashtable;

import com.general.util.Timer;
import com.general.util.TimerThread;
import com.myster.filemanager.FileTypeListManager;
import com.myster.pref.ui.PreferencesPanel;
import com.myster.type.MysterType;
import com.myster.util.TypeChoice;

/**
 * The FMICHooser is the FileManagerInterfaceChooser. It's the GUI for the
 * FileManager prefs panel. It's built to be as independent from the internal
 * working of the FileManager, dispite having access to the sweat, sweat inners.
 */

public class FMIChooser extends PreferencesPanel {
    boolean inited = false;

    private String path;

    private FileTypeListManager manager;

    private TypeChoice choice;

    private Checkbox checkbox;

    private Button button;

    private Label pathLabel;

    private Label folderl, filelistl;

    private List flist;

    private Button setAllButton;

    private Hashtable hash = new Hashtable();

    private static final int XPAD = 10;

    private static final int SAB = 200;

    private static final int MAX_PATH_LABEL_SIZE = STD_XSIZE - 100 - 3 * XPAD - 5;

    public FMIChooser(FileTypeListManager manager) {
        this.manager = manager;
        setLayout(null);
        //if (inited==true) System.exit(0);
        inited = true;

        choice = new TypeChoice();
        choice.setLocation(5, 4);
        choice.setSize(STD_XSIZE - XPAD - XPAD - SAB, 20);
        choice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent a) {
                restoreState();
                repaint();
            }
        });
        add(choice);

        setAllButton = new Button("Set all paths to this path");
        setAllButton.setLocation(STD_XSIZE - XPAD - SAB, 4);
        setAllButton.setSize(SAB, 20);
        setAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                String newPath = path;

                for (int i = 0; i < choice.getItemCount(); i++) {

                    //Figure out what the bool.. should be (hack)
                    Object o = hash.get(choice.getType(i));
                    boolean bool_temp;
                    if (o != null) {
                        bool_temp = ((SettingsStruct) (o)).shared;
                    } else {
                        bool_temp = FMIChooser.this.manager.isShared(choice.getType(i));
                    } //end

                    hash.put(choice.getType(i), new SettingsStruct(choice.getType(i), newPath,
                            bool_temp));
                    System.out.println(newPath);
                }
            }
        });
        add(setAllButton);

        path = manager.getPathFromType(choice.getType());

        checkbox = new Checkbox("Share this type", manager.isShared(choice.getType()));
        checkbox.setLocation(10, 55);
        checkbox.setSize(150, 25);
        checkbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                hash.put(choice.getType(), new SettingsStruct(choice.getType(), path, checkbox
                        .getState()));
            }
        });
        add(checkbox);

        button = new Button("Set Folder");
        button.setLocation(STD_XSIZE - 100 - XPAD, 55);
        button.setSize(100, 25);
        button.addActionListener(new ActionListener() {
            public synchronized void actionPerformed(ActionEvent a) {
                FileDialog dialog = new FileDialog(new Frame(),
                        "Choose a directory and press save", FileDialog.SAVE);
                dialog.setFile("Choose a directory and press save");
                dialog.show(); //show choose dir dialog
                String p = dialog.getDirectory();

                if (p == null) { //If user canceled path will be null
                    System.out.println("User cancelled the action.");
                    return;
                }
                path = p;
                setPathLabel(path);
                hash.put(choice.getType(), new SettingsStruct(choice.getType(), path, checkbox
                        .getState()));
            }

        });
        add(button);

        folderl = new Label("Shared Folder:");
        folderl.setLocation(10, 85);
        folderl.setSize(100, 20);
        add(folderl);

        pathLabel = new Label(); //dependency
        // on
        // choice
        // being
        // created
        // first.
        pathLabel.setLocation(100 + XPAD + 5, 85);
        //textfeild.setEditable(false);
        pathLabel.setSize(STD_XSIZE - 100 - 3 * XPAD - 5, 20);
        setPathLabel(manager.getPathFromType(choice.getType()));
        add(pathLabel);

        filelistl = new Label("Shared Files (click \"Apply\" to see changes) :");
        filelistl.setLocation(10, 110);
        filelistl.setSize(STD_XSIZE - 2 * XPAD, 20);
        add(filelistl);

        flist = new List();
        flist.setLocation(10, 135);
        flist.setSize(STD_XSIZE - 2 * XPAD, STD_YSIZE - 150);
        add(flist);

        repaint();
        reset();
        restoreState();
    }

    private void setPathLabel(String newPath) {
        pathLabel.setText(newPath);
        for (int i = newPath.length(); MAX_PATH_LABEL_SIZE < pathLabel.getPreferredSize().width
                && i >= 0; i--) {
            pathLabel.setText(TIM(newPath, i));
        }
    }

    public void save() {
        Enumeration enum = hash.elements();
        while (enum.hasMoreElements()) {
            SettingsStruct s = (SettingsStruct) (enum.nextElement());
            manager.setPathFromType(s.type, s.path);
            manager.setShared(s.type, s.shared);
        }
        reset();//funky.
    }

    private static class SettingsStruct {
        String path;

        boolean shared;

        MysterType type;

        public SettingsStruct(MysterType type, String path, boolean shared) {
            this.type = type;
            this.path = path;
            this.shared = shared;
        }
    }

    public void reset() {
        hash.clear();
        loadStateFromPrefs();
        restoreState();
    }

    public void loadStateFromPrefs() {
        path = manager.getPathFromType(choice.getType());
        checkbox.setState(manager.isShared(choice.getType()));
    }

    public String getKey() {
        return "File Manager";
    }

    public Dimension getPreferredSize() {
        return new Dimension(STD_XSIZE, STD_YSIZE);
    }

    public void paint(Graphics g) {
        g.setColor(new Color(150, 150, 150));
        g.drawRect(5, 35, STD_XSIZE - 10, STD_YSIZE - 40);
        g.setColor(getBackground());
        g.fillRect(10, 34, 170, 3);
        g.setColor(Color.black);
        g.drawString("Setting for type: " + choice.getType(), 12, 39);
    }

    private void restoreState() {
        SettingsStruct ss = (SettingsStruct) (hash.get(choice.getType()));
        if (ss != null) {
            path = ss.path;
            checkbox.setState(ss.shared);
        } else {
            loadStateFromPrefs(); //if the state hasen't been changed then load
            // form prefs.
        }

        setPathLabel(path);
        String[] s = manager.getDirList(choice.getType());
        boolean isShared = manager.isShared(choice.getType());

        flist.removeAll();
        if (manager.getFileTypeList(choice.getType()).isIndexing()) {
            flist.add("<Indexing files...>");
            pokeTimer();
            return;
        } else {
            if (timer!=null)
                timer.cancelTimer(); //speed hack.
            
            timer = null;
        }

        if (s != null) {
            if (!isShared) {
                flist.add("<no files are being shared, sharing is disabled>");
            } else if (s.length == 0) {
                flist.add("<no files are being shared, there's no relevant files in this folder>");
            } else {
                if (s.length > 150) {
                    flist.add("<You are sharing " + s.length + " files (too many to list)>");
                } else {
                    for (int i = 0; i < s.length; i++) {
                        flist.add(s[i]);
                    }
                }
            }
        }
    }

    Timer timer = null;

    public void pokeTimer() {
        if (timer != null)
            return;
        if (manager.getFileTypeList(choice.getType()).isIndexing()) {
            timer = new Timer(new Runnable() {/*
                                               * (non-Javadoc)
                                               * 
                                               * @see java.lang.Runnable#run()
                                               */
                public void run() {
                    timer = null;
                    pokeTimer();
                }
            }, 100);
        } else {
            restoreState();
        }
    }

    /*
     * TIM = Trim in the Middle. This is a utility function that keeps strings
     * under numberOfChars characters and removes characters from the middle and
     * adding "..."
     */
    private String TIM(String input, int numberOfChars) {
        if (input.length() <= numberOfChars)
            return input;
        if (input.length() < 2)
            return "...";

        int amountToDelete = numberOfChars / 2;
        return input.substring(0, amountToDelete)
                + "..."
                + input.substring(input.length() - (numberOfChars - amountToDelete) - 1, input
                        .length());
    }

}