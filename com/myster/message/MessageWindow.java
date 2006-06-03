package com.myster.message;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.LayoutManager;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.UnknownHostException;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.myster.net.MysterAddress;
import com.myster.tracker.IPListManagerSingleton;
import com.myster.tracker.MysterServer;
import com.myster.ui.MysterFrame;

public class MessageWindow extends MysterFrame {
    private Panel mainPanel;

    private HeaderPanel header;

    private GridBagLayout gblayout;

    private GridBagConstraints gbconstrains;

    private MessageWindowButtonBar bar;

    MessageTextArea quoteArea, messageArea;

    public static final boolean NEW_MESSAGE = true;

    public static final boolean FROM_SOMEONE_ELSE = false;

    public MessageWindow(InstantMessage message) {
        this(FROM_SOMEONE_ELSE, message.message, message.quote, message.address);
    }

    public MessageWindow(MysterAddress address, String quote) {
        this(NEW_MESSAGE, "", quote, address);
    }

    public MessageWindow(MysterAddress address) {
        this(NEW_MESSAGE, "", null, address);
    }

    public MessageWindow() {
        this(NEW_MESSAGE, "", null, null);
    }

    private MessageWindow(final boolean type, final String message, final String quote,
            final MysterAddress address) {
        setSize(320, 400);

        //Do interface setup:
        gblayout = new GridBagLayout();
        gbconstrains = new GridBagConstraints();
        gbconstrains.fill = GridBagConstraints.BOTH;
        gbconstrains.ipadx = 1;
        gbconstrains.ipady = 1;
        gbconstrains.insets = new Insets(5, 5, 5, 5);

        mainPanel = new Panel();
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setLayout(gblayout);

        header = new HeaderPanel(address, type);
        addComponent(header, 1, 1, 1, 1, 10, 0);

        bar = new MessageWindowButtonBar(type);

        if (quote != null) {
            quoteArea = new MessageTextArea(false, quote, "Quotation: ");
            addComponent(quoteArea, 2, 1, 1, 1, 10, 1);

            SeperatorLine line1 = new SeperatorLine();
            line1.setForeground(new Color(150, 150, 150));
            addComponent(line1, 3, 1, 1, 1, 10, 0);
        }

        if (message != null) {
            messageArea = new MessageTextArea(type, message, "Message: ");
            addComponent(messageArea, 4, 1, 1, 1, 10, 1);

            messageArea.addKeyListener(new KeyListener() {
                public void keyTyped(KeyEvent e) {
                    setSendMessageAllowed(((JTextArea) (e.getSource())).getText());
                }

                public void keyPressed(KeyEvent e) {
                }

                public void keyReleased(KeyEvent e) {
                    setSendMessageAllowed(((JTextArea) (e.getSource())).getText());
                }
            });

            setSendMessageAllowed(messageArea.getText());
        }

        addComponent(bar, 5, 1, 1, 1, 10, 0);

        add(mainPanel);

        setTitle(address == null ? "Instant Message" : "" + address);

        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                if (address != null)
                    messageArea.focusBaby();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }

            public void windowClosed(WindowEvent e) {
                //dispose();
            }
        });
    }

    private void closeThisWindow() {
        close();
        //dispose();
    }

    //This works on mainPanel and not the window itself!
    private void addComponent(Component c, int row, int column, int width, int height, int weightx,
            int weighty) {
        gbconstrains.gridx = column;
        gbconstrains.gridy = row;

        gbconstrains.gridwidth = width;
        gbconstrains.gridheight = height;

        gbconstrains.weightx = weightx;
        gbconstrains.weighty = weighty;

        gblayout.setConstraints(c, gbconstrains);

        mainPanel.add(c);

    }

    private String getMessage() {
        return (messageArea == null ? null : messageArea.getText());
    }

    private String getQuote() {
        return (quoteArea == null ? null : quoteArea.getText());
    }

    private void setSendMessageAllowed(String text) {
        bar.setAcceptEnable(!(text.equals(""))); //this is why doing OO GUIs
        // sucks.. Delegations..
    }

    private class MessageWindowButtonBar extends Panel {
        Button accept; //can be reply or send

        Button cancel; //can be dismiss or cancel

        private static final int X_BUTTON_SIZE = 100;

        private static final int Y_BUTTON_SIZE = 25;

        private static final int PADDING = 5;

        public MessageWindowButtonBar(final boolean type) {
            setLayout(new MyLayoutManager());

            accept = new Button(type == MessageWindow.NEW_MESSAGE ? "Send Message" : "Reply");
            accept.setSize(X_BUTTON_SIZE, Y_BUTTON_SIZE);
            if (type == NEW_MESSAGE) {
                accept.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        String msg = getMessage();
                        if ((msg == null) || (msg.equals("")))
                            return; //err (this should not be possible)

                        try {
                            MessageManager.sendInstantMessage(new InstantMessage(header
                                    .getAddress(), getMessage(), getQuote()));
                            closeThisWindow();
                        } catch (UnknownHostException ex) {
                            System.out
                                    .println("Could nto send message 'cause the address is invalid.");
                            com.general.util.AnswerDialog
                                    .simpleAlert(MessageWindow.this,
                                            "Could not send this message because the address is not valid.");
                        }
                    }
                });
            } else {
                accept.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            MessageWindow messageWindow = new MessageWindow(header.getAddress(),
                                    getMessage());
                            messageWindow.setBounds(MessageWindow.this.getBounds());
                            messageWindow.setVisible(true);
                            closeThisWindow();
                        } catch (UnknownHostException ex) {
                            //..! should never happen
                        }
                        //fire event to close this window.
                    }
                });
            }
            add(accept);

            cancel = new Button(type == MessageWindow.NEW_MESSAGE ? "Cancel" : "OK");
            cancel.setSize(X_BUTTON_SIZE, Y_BUTTON_SIZE);
            cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    closeThisWindow();
                }
            });
            add(cancel);
        }

        public Dimension getMinimumSize() {
            return new Dimension(3 * PADDING + 2 * X_BUTTON_SIZE, Y_BUTTON_SIZE + 2 * PADDING);
        }

        public Dimension getPreferredSize() {
            return new Dimension(3 * PADDING + 2 * X_BUTTON_SIZE, Y_BUTTON_SIZE + 2 * PADDING);
        }

        public void setAcceptEnable(boolean isEnabled) {
            accept.setEnabled(isEnabled);
        }

        //MyLayoutManager Basically dsoes nothing
        private class MyLayoutManager implements LayoutManager {
            public void addLayoutComponent(String s, Component c) {
            }

            public void layoutContainer(Container c) {
                accept.setLocation(getSize().width - X_BUTTON_SIZE - PADDING, PADDING);
                cancel.setLocation(getSize().width - 2 * X_BUTTON_SIZE - 2 * PADDING, PADDING);
            }

            public Dimension minimumLayoutSize(Container c) {
                return new Dimension(1, 1);
            }

            public Dimension preferredLayoutSize(Container c) {
                return new Dimension(1, 1);
            }

            public void removeLayoutComponent(Component c) {
            }
        }
    }

}

class HeaderPanel extends Panel {
    TextField addressField;

    Label toFrom;

    private GridBagLayout gblayout;

    private GridBagConstraints gbconstrains;

    MysterAddress address;

    public HeaderPanel(MysterAddress address, final boolean type) {
        //Do interface setup:
        gblayout = new GridBagLayout();
        gbconstrains = new GridBagConstraints();
        gbconstrains.fill = GridBagConstraints.BOTH;
        gbconstrains.ipadx = 1;
        gbconstrains.ipady = 1;
        setLayout(gblayout);

        this.address = address;

        toFrom = new Label(type == MessageWindow.NEW_MESSAGE ? "To: " : "From: ");
        addComponent(toFrom, 1, 1, 1, 1, 0, 1);

        addressField = new TextField(40);

        if (address == null) {

        } else {
            MysterServer server = IPListManagerSingleton.getIPListManager().getQuickServerStats(
                    address);

            String serverName;

            if (server == null) {
                serverName = address.toString();
            } else {
                serverName = (server.getServerIdentity().equals(address.toString()) ? address
                        .toString() : server.getServerIdentity() + " (" + address.toString() + ")");
            }

            addressField.setText(serverName);
            addressField.setEditable(false);
        }

        addComponent(addressField, 1, 2, 1, 1, 1, 1);
    }

    //TODO: Should not be called on Event Thread
    public MysterAddress getAddress() throws UnknownHostException {
        return (addressField.isEditable() ? new MysterAddress(addressField.getText()) : address);
    }

    //This works on mainPanel and not the window itself!
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
}

class MessageTextArea extends Panel {
    private JTextArea area;

    public MessageTextArea(boolean editable, String text, String labelText) {
        setLayout(new BorderLayout());

        area = new JTextArea("");
        area.setSize(400, 400);
        area.setWrapStyleWord(true);
        area.setAutoscrolls(true);
        area.setLineWrap(true);
        area.setEditable(editable);
        area.setText(text);

        JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().add(area);
        scrollPane.setDoubleBuffered(true);
        add(scrollPane, "Center");

        Label message = new Label(labelText);
        add(message, "North");
    }

    public void setText(String text) {
        area.setText(text);
    }

    public String getText() {
        return area.getText();
    }

    public void focusBaby() {
        requestFocus();
        area.requestFocus();
    }

    public void addKeyListener(KeyListener l) {
        area.addKeyListener(l);
    }
}

class SeperatorLine extends Panel {
    public static final int X_MIN = 1;

    public static final int Y_MIN = 1;

    public Dimension getPreferredSize() {
        return getSize();
    }

    public Dimension getMinimumSize() {
        return new Dimension(X_MIN, Y_MIN);
    }

    public void paint(Graphics g) {
        int x = getSize().width;

        if (x > 1)
            g.drawLine(0, 1, getSize().width, 1);
    }
}