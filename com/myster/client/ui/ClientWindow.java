/*
 * 
 * Title: Myster Open Source Author: Andrew Trumper Description: Generic Myster
 * Code
 * 
 * This code is under GPL
 * 
 * Copyright Andrew Trumper 2000-2001
 */

package com.myster.client.ui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.UnknownHostException;

import com.general.mclist.GenericMCListItem;
import com.general.mclist.MCList;
import com.general.mclist.Sortable;
import com.general.mclist.SortableString;
import com.general.util.AnswerDialog;
import com.general.util.KeyValue;
import com.general.util.MessageField;
import com.general.util.StandardWindowBehavior;
import com.myster.net.MysterAddress;
import com.myster.tracker.IPListManagerSingleton;
import com.myster.tracker.MysterServer;
import com.myster.type.MysterType;
import com.myster.ui.MysterFrame;
import com.myster.util.Sayable;

public class ClientWindow extends MysterFrame implements Sayable {
    private GridBagLayout gblayout;

    private GridBagConstraints gbconstrains;

    private Button connect;

    private TextField IP;

    private MCList fileTypeList;

    private MCList fileList;

    private FileInfoPane pane;

    private String currentip;

    private Button instant;

    private MessageField msg;

    private static final int XDEFAULT = 600;

    private static final int YDEFAULT = 400;

    private static final int SBXDEFAULT = 72; //send button X default

    private static final int GYDEFAULT = 50; //Generic Y default

    private static int counter = 0;

    private static final String WINDOW_KEEPER_KEY = "Myster's Client Windows";

    private static final String CLIENT_WINDOW_TITLE_PREFIX = "Direct Connection ";

    public static void initWindowLocations() {
        Rectangle[] rectangles = com.myster.ui.WindowLocationKeeper.getLastLocs(WINDOW_KEEPER_KEY);

        for (int i = 0; i < rectangles.length; i++) {
            ClientWindow window = new ClientWindow();
            window.setBounds(rectangles[i]);
        }
    }

    public ClientWindow() {
        super("Direct Connection " + (++counter));

        makeClientWindow();

        //windowKeeper.addFrame(this);
    }

    public ClientWindow(String ip) {
        super("Direct Connection " + (++counter));
        makeClientWindow();
        IP.setText(ip);
        //connect.dispatchEvent(new KeyEvent(connect, KeyEvent.KEY_PRESSED,
        // System.currentTimeMillis(), 0, KeyEvent.VK_ENTER,
        // (char)KeyEvent.VK_ENTER));
        //connect.dispatchEvent(new KeyEvent(connect, KeyEvent.KEY_RELEASED,
        // System.currentTimeMillis(), 0, KeyEvent.VK_ENTER,
        // (char)KeyEvent.VK_ENTER));
        connect.dispatchEvent(new ActionEvent(connect, ActionEvent.ACTION_PERFORMED,
                "Connect Button"));
    }

    private void makeClientWindow() {
        setBackground(new Color(240, 240, 240));

        //Do interface setup:
        gblayout = new GridBagLayout();
        setLayout(gblayout);
        gbconstrains = new GridBagConstraints();
        gbconstrains.fill = GridBagConstraints.BOTH;
        gbconstrains.insets = new Insets(5, 5, 5, 5);
        gbconstrains.ipadx = 1;
        gbconstrains.ipady = 1;

        pane = new FileInfoPane();
        pane.setSize(XDEFAULT / 3, YDEFAULT - 40);

        connect = new Button("Connect");
        connect.setSize(SBXDEFAULT, GYDEFAULT);

        IP = new TextField("Enter an IP here");
        IP.setEditable(true);

        fileTypeList = new MCList(1, true, this);
        fileTypeList.sortBy(-1);
        fileTypeList.setColumnName(0, "Type");

        fileList = new MCList(1, true, this);
        fileList.sortBy(-1);
        fileList.setColumnName(0, "Files");
        //fileList.setColumnWidth(0, 300);

        msg = new MessageField("Idle...");
        msg.setEditable(false);
        msg.setSize(XDEFAULT, GYDEFAULT);

        instant = new Button("Instant Message");
        instant.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    com.myster.net.MysterAddress address = new com.myster.net.MysterAddress(IP
                            .getText());
                    com.myster.message.MessageWindow window = new com.myster.message.MessageWindow(
                            address);
                    window.setVisible(true);
                } catch (java.net.UnknownHostException ex) {
                    (new AnswerDialog(ClientWindow.this, "The address " + IP.getText()
                            + " does not apear to be a valid internet address.")).answer();
                }
            }
        });

        //reshape(0, 0, XDEFAULT, YDEFAULT);

        addComponent(connect, 0, 0, 1, 1, 1, 0);
        addComponent(IP, 0, 1, 2, 1, 6, 0);
        addComponent(instant, 0, 3, 1, 1, 5, 0);
        addComponent(fileTypeList.getPane(), 1, 0, 1, 1, 1, 99);
        addComponent(fileList.getPane(), 1, 1, 2, 1, 6, 99);
        addComponent(pane, 1, 3, 1, 1, 5, 99);
        addComponent(msg, 2, 0, 4, 1, 99, 0);

        setResizable(true);
        setSize(XDEFAULT, YDEFAULT);
        show(true);

        //filelisting.addActionListener(???);
        ConnectButtonEvent connectButtonEvent = new ConnectButtonEvent(this);
        connect.addActionListener(connectButtonEvent);
        IP.addActionListener(connectButtonEvent);

        fileTypeList.addMCListEventListener(new FileTypeSelectListener(this));
        fileList.addMCListEventListener(new FileListAction(this));
        fileList.addMCListEventListener(new FileStatsAction(this));

        addWindowListener(new StandardWindowBehavior());

    }

    private void addComponent(Component c, int row, int column, int width, int height, int weightx,
            int weighty) {
        gbconstrains.gridx = column;
        gbconstrains.gridy = row;

        gbconstrains.gridwidth = width;
        gbconstrains.gridheight = height;

        gbconstrains.weightx = weightx;
        gbconstrains.weighty = weighty;

        gblayout.setConstraints(c, gbconstrains);

        add(c);

    }

    public void addItemToTypeList(String s) {
        fileTypeList.addItem(new GenericMCListItem(new Sortable[] { new SortableString(s) }, s));
    }

    public void addItemsToFileList(String[] files) {
        GenericMCListItem[] items = new GenericMCListItem[files.length];

        for (int i = 0; i < items.length; i++)
            items[i] = new GenericMCListItem(new Sortable[] { new SortableString(files[i]) },
                    files[i]);

        fileList.addItem(items);
    }

    public void clearFileList() {
        fileList.clearAll();
        pane.clear();
    }

    public void clearFileStats() {
        pane.clear();
    }

    public void refreshIP() {
        currentip = IP.getText();
        MysterServer server = null;
        try {
            server = IPListManagerSingleton.getIPListManager().getQuickServerStats(
                    new MysterAddress(currentip));
        } catch (UnknownHostException e) {}

        setTitle(CLIENT_WINDOW_TITLE_PREFIX + "to \"" + (server == null ? currentip : server.getServerIdentity()) +"\"");
    }

    //To be in an interface??
    public String getCurrentIP() {
        return currentip;
    }

    public MysterType getCurrentType() {
        int selectedIndex = fileTypeList.getSelectedIndex();

        if (selectedIndex != -1)
            return new MysterType(((String) (fileTypeList.getItem(selectedIndex))).getBytes());

        return new MysterType(new byte[] { (byte) 'M', (byte) 'P', (byte) 'G', (byte) '3' });
    }

    public String getCurrentFile() {
        int selectedIndex = fileList.getSelectedIndex();

        if (selectedIndex == -1)
            return "";

        return (String) fileList.getItem(selectedIndex);
    }

    public void clearAll() {
        fileTypeList.clearAll();
        fileList.clearAll();
        pane.clear();
    }

    public void say(String s) {
        msg.say(s);
    }

    public MessageField getMessageField() {
        return msg;
    }

    //end ?

    public void showFileStats(KeyValue k) {
        pane.display(k);
    }
}