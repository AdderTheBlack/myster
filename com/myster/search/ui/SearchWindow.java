/*
 * 
 * Title: Myster Open Source Author: Andrew Trumper Description: Generic Myster
 * Code
 * 
 * This code is under GPL
 * 
 * Copyright Andrew Trumper 2000-2001
 */

package com.myster.search.ui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;

import com.general.mclist.MCList;
import com.general.mclist.MCListEvent;
import com.general.mclist.MCListEventAdapter;
import com.general.mclist.MCListItemInterface;
import com.general.util.MessageField;
import com.general.util.StandardWindowBehavior;
import com.general.util.TextSpinner;
import com.myster.search.SearchResult;
import com.myster.search.SearchResultListener;
import com.myster.type.MysterType;
import com.myster.ui.MysterFrame;
import com.myster.ui.WindowLocationKeeper;
import com.myster.util.Sayable;
import com.myster.util.TypeChoice;

public class SearchWindow extends MysterFrame implements SearchResultListener,
        Sayable {
    GridBagLayout gblayout;

    GridBagConstraints gbconstrains;

    Button searchbutton;

    MCList filelist;

    TextField textentry;

    Label label;

    TypeChoice choice;

    MessageField msg;

    TextSpinner spinner = new TextSpinner();

    ClientHandleObject metaDateHandler;

    private final int XDEFAULT = 640;

    private final int YDEFAULT = 400;

    private static final String PREF_LOCATION_KEY = "Search Window";

    private static int counter = 0;

    public SearchWindow() {
        super("Search Window " + (++counter));

        setBackground(new Color(240, 240, 240));

        //Do interface setup:
        gblayout = new GridBagLayout();
        setLayout(gblayout);
        gbconstrains = new GridBagConstraints();
        gbconstrains.fill = GridBagConstraints.BOTH;
        gbconstrains.insets = new Insets(5, 5, 5, 5);
        gbconstrains.ipadx = 1;
        gbconstrains.ipady = 1;

        searchbutton = new Button("Search") {
            public Dimension getPreferredSize() {
                return new Dimension(Math.max(75,
                        super.getPreferredSize().width), super
                        .getPreferredSize().height); //hack to stop the button
                                                     // label from causing
                                                     // layout oddities.
            }

            public Dimension getMinimumSize() {
                return new Dimension(
                        Math.max(75, super.getMinimumSize().width), super
                                .getMinimumSize().height);
            }
        };

        searchbutton.setSize(50, 25);

        textentry = new TextField("", 40);
        textentry.setEditable(true);

        //connect.dispatchEvent(new KeyEvent(connect, KeyEvent.KEY_PRESSED,
        // System.currentTimeMillis(), 0, KeyEvent.VK_ENTER,
        // (char)KeyEvent.VK_ENTER));

        label = new Label("Type:");

        choice = new TypeChoice();

        filelist = new MCList(1, true, this);
        filelist.getPane().setSize(XDEFAULT, YDEFAULT);

        msg = new MessageField("Idle...");
        msg.setEditable(false);
        msg.setSize(100, 20);

        //reshape(0, 0, XDEFAULT, YDEFAULT);

        addComponent(textentry, 0, 1, 1, 1, 0, 0);
        addComponent(searchbutton, 0, 3, 1, 1, 0, 0);
        //addComponent(label ,0,0,1,1,0,0);
        addComponent(choice, 0, 2, 1, 1, 0, 0);
        addComponent(filelist.getPane(), 2, 0, 5, 1, 1, 1);
        addComponent(msg, 3, 0, 5, 1, 1, 0);
        addComponent(new Panel(), 0, 4, 1, 1, 1, 0);

        setResizable(true);
        setSize(XDEFAULT, YDEFAULT);

        //setIconImage(Util.loadImage("img.jpg", this));

        SearchButtonEvent searchEventObject = new SearchButtonEvent(this,
                searchbutton);
        searchbutton.addActionListener(searchEventObject);
        /*
         * searchbutton.addActionListener(new java.awt.event.ActionListener() {
         * public void actionPerformed(java.awt.event.ActionEvent event) {
         * System.out.println("You clicked the button"); } });
         */
        textentry.addActionListener(searchEventObject); //not only for buttons
                                                        // anymore.

        filelist.addMCListEventListener(new MCListEventAdapter() {
            public synchronized void doubleClick(MCListEvent a) {
                MCList list = a.getParent();
                downloadFile(list.getItem(list.getSelectedIndex()));
            }
        });

        addWindowListener(new StandardWindowBehavior());

        //bucket=new SearchResultBucket(BUCKETSIZE);

        filelist.setColumnName(0, "Search Results appear here");
        filelist.setColumnWidth(0, 400);

        keeper.addFrame(this);

        setVisible(true); // !

        textentry.setSelectionStart(0);
        textentry.setSelectionEnd(textentry.getText().length());

    }

    static WindowLocationKeeper keeper;//cheat to save scrolling. put at top
                                       // later.

    public static void initWindowLocations() {
        Rectangle[] rectangles = WindowLocationKeeper
                .getLastLocs(PREF_LOCATION_KEY);

        keeper = new WindowLocationKeeper(PREF_LOCATION_KEY);

        for (int i = 0; i < rectangles.length; i++) {
            SearchWindow window = new SearchWindow();
            window.setBounds(rectangles[i]);
        }

        if (rectangles.length == 0) {
            SearchWindow window = new SearchWindow();
        }

    }

    public void addComponent(Component component, int row, int column, int width,
            int height, int weightx, int weighty) {
        gbconstrains.gridx = column;
        gbconstrains.gridy = row;

        gbconstrains.gridwidth = width;
        gbconstrains.gridheight = height;

        gbconstrains.weightx = weightx;
        gbconstrains.weighty = weighty;

        gblayout.setConstraints(component, gbconstrains);

        super.add(component);

    }

    public void startSearch() {
        msg.say("Clearing File List...");
        filelist.clearAll();
        recolumnize();
        //bucket=new SearchResultBucket(BUCKETSIZE);
    }

    public void searchOver() {
        msg.say("Search done. " + filelist.length() + " file"
                + (filelist.length() == 0 ? "" : "s") + " found...");
    }

    public void recolumnize() {
        metaDateHandler = ClientInfoFactoryUtilities.getHandler(getType());
        int max = metaDateHandler.getNumberOfColumns();
        filelist.setNumberOfColumns(max);

        for (int i = 0; i < max; i++) {
            filelist.setColumnName(i, metaDateHandler.getHeader(i));
            filelist.setColumnWidth(i, metaDateHandler.getHeaderSize(i));
        }
    }

    public boolean addSearchResults(SearchResult[] resultArray) {
        MCListItemInterface[] m = new MCListItemInterface[resultArray.length];

        for (int i = 0; i < resultArray.length; i++) {
            m[i] = metaDateHandler.getMCListItem(resultArray[i]);
        }

        filelist.addItem(m);
        return true;
    }

    public void searchStats(SearchResult s) {
        //? dunno.
    }

    public String getSearchString() {
        return textentry.getText();
    }

    public MysterType getType() {
        return choice.getType();
    }

    public void downloadFile(Object s) {
        ((SearchResult) (s)).download();
    }

    public void paint(Graphics g) {
        filelist.repaint(); //neede dbecause when an item is updated this
                            // object's repaint() methoods is called. The
                            // repaint() needs to be apssed on to the list.
    }

    /*
     * public void openAClientWindow(String s) { ClientWindow w=new
     * ClientWindow(bucket.getValue(s).getIP()); }
     */

    public void say(String s) {
        //System.out.println(s);
        msg.say("" + s);
    }
}