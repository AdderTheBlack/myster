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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

import com.general.util.KeyValue;

public class FileInfoPane extends JPanel {
    KeyValue keyvalue;

    private static final int VOFFSET = 25;

    private static final int HOFFSET = 3;

    public FileInfoPane() {
        keyvalue = new KeyValue();
    }

    public void display(KeyValue k) {
        keyvalue = k;
        repaint();
    }

    public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        Rectangle clipBounds = g.getClipBounds();
        g.fillRect((int) clipBounds.getX(), (int) clipBounds.getY(), (int) clipBounds.getWidth(),
                (int) clipBounds.getHeight());
        FontMetrics metric = getFontMetrics(getFont());
        int vertical = metric.getHeight() + 3;
        g.setColor(Color.black);
        for (int i = 0; i < keyvalue.length(); i++) {
            if (((String) (keyvalue.keyAt(i))).equals("size")) { // hack to
                // show
                // size as
                // bytes string
                // like
                // XXXbytes or
                // XXXMB
                try {
                    g.drawString(""
                            + (String) (keyvalue.keyAt(i))
                            + " : "
                            + com.general.util.Util.getStringFromBytes(Long
                                    .parseLong((String) keyvalue.valueAt(i))), HOFFSET, VOFFSET + i
                            * vertical);
                } catch (NumberFormatException ex) {
                    g.drawString("" + (String) (keyvalue.keyAt(i)) + " : "
                            + (String) keyvalue.valueAt(i), HOFFSET, VOFFSET + i * vertical);
                }
            } else {
                g.drawString("" + (String) (keyvalue.keyAt(i)) + " : "
                        + (String) keyvalue.valueAt(i), HOFFSET, VOFFSET + i * vertical);
            }
        }
        // Util.getStringFromBytes(Long.valueOf((String)keyvalue.valueAt(i)).longValue())

    }

    public KeyValue getData() {
        return keyvalue;
    }

    public void clear() {
        keyvalue = new KeyValue();
        repaint();
    }

    public Dimension getMinimumSize() {
        return new Dimension(0, 0);
    }

    public Dimension getPreferredSize() {
        return new Dimension(10, 10);
    }
}