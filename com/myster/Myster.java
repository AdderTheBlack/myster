/*
 * 
 * Title: Myster Open Source Author: Andrew Trumper Description: Generic Myster Code
 * 
 * This code is under GPL
 * 
 * Copyright Andrew Trumper 2000-2004
 */

package com.myster;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.general.application.ApplicationSingleton;
import com.general.application.ApplicationSingletonListener;
import com.general.util.AnswerDialog;
import com.general.util.Util;
import com.myster.application.MysterGlobals;
import com.myster.bandwidth.BandwidthManager;
import com.myster.filemanager.FileTypeListManager;
import com.myster.pref.Preferences;
import com.myster.search.ui.SearchWindow;
import com.myster.server.ServerFacade;
import com.myster.server.ui.ServerStatsWindow;
import com.myster.tracker.IPListManagerSingleton;
import com.myster.util.I18n;

public class Myster {
    private static final String LOCK_FILE_NAME = ".lockFile";

    public static void main(String[] args) {
        final boolean isServer = (args.length > 0 && args[0].equals("-s"));

        System.out.println("java.vm.specification.version:"
                + System.getProperty("java.vm.specification.version"));
        System.out.println("java.vm.specification.vendor :"
                + System.getProperty("java.vm.specification.vendor"));
        System.out.println("java.vm.specification.name   :"
                + System.getProperty("java.vm.specification.name"));
        System.out
                .println("java.vm.version              :" + System.getProperty("java.vm.version"));
        System.out.println("java.vm.vendor               :" + System.getProperty("java.vm.vendor"));
        System.out.println("java.vm.name                 :" + System.getProperty("java.vm.name"));

        try {
            Class uiClass = Class.forName("javax.swing.UIManager");
            Method setLookAndFeel = uiClass.getMethod("setLookAndFeel",
                    new Class[] { String.class });
            Method getSystemLookAndFeelClassName = uiClass.getMethod(
                    "getSystemLookAndFeelClassName", new Class[] {});
            String lookAndFeelName = (String) getSystemLookAndFeelClassName.invoke(null, null);
            setLookAndFeel.invoke(null, new Object[] { lookAndFeelName });
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        //        try {
        //            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        //        } catch (ClassNotFoundException e) {
        //            e.printStackTrace();
        //        } catch (InstantiationException e) {
        //            e.printStackTrace();
        //        } catch (IllegalAccessException e) {
        //            e.printStackTrace();
        //        } catch (UnsupportedLookAndFeelException e) {
        //            e.printStackTrace();
        //        }
        ApplicationSingletonListener applicationSingletonListener = new ApplicationSingletonListener() {
            public void requestLaunch(String[] args) {
                SearchWindow search = new SearchWindow();
                search.show();
            }

            public void errored(Exception ignore) {

            }
        };

        ApplicationSingleton applicationSingleton = new ApplicationSingleton(new File(MysterGlobals
                .getCurrentDirectory(), LOCK_FILE_NAME), 10457, applicationSingletonListener, args);

        MysterGlobals.appSigleton = applicationSingleton;
        final long startTime = System.currentTimeMillis();
        try {
            if (!applicationSingleton.start())
                return;
        } catch (IOException e) {
            e.printStackTrace();
            Frame parent = AnswerDialog.getCenteredFrame();

            AnswerDialog
                    .simpleAlert(
                            parent,
                            "There seems to be another copy of Myster already running but I couldn't"
                                    + " contact it. If you're sharing the computer with other people, one of them"
                                    + " might be running Myster already or it might be that that Myster was not"
                                    + " started from the same place the previous copy was started. Restarting the "
                                    + " computer will make sure that the other Myster client gets quit.");
            parent.dispose(); //if this isn't here Myster won't quit.
            applicationSingleton.close();
            System.exit(0);
            return;
        }

        I18n.init();

        System.out.println("MAIN THREAD: Starting loader Thread..");

        try {

            Util.invokeAndWait(new Runnable() {
                public void run() {

                    try {
                        if (com.myster.type.TypeDescriptionList.getDefault().getEnabledTypes().length <= 0) {
                            AnswerDialog
                                    .simpleAlert("There are not enabled types. This screws up Myster. Please make sure"
                                            + " the typedescriptionlist.mml is in the right place and correctly"
                                            + " formated.");
                            MysterGlobals.quit();
                            return; //not reached
                        }
                    } catch (Exception ex) {
                        AnswerDialog.simpleAlert("Could not load the Type Description List: \n\n"
                                + ex);
                        MysterGlobals.quit();
                        return; //not reached
                    }

                    try {
                        com.myster.hash.HashManager.init();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    com.myster.hash.ui.HashManagerGUI.init();

                    ServerFacade.init();

                    ServerStatsWindow.getInstance().pack();

                    com.myster.message.MessageManager.init();

                    com.myster.ui.WindowManager.init();

                    Preferences.getInstance().addPanel(BandwidthManager.getPrefsPanel());

                    try {
                        (new com.myster.plugin.PluginLoader(new File(MysterGlobals
                                .getCurrentDirectory(), "plugins"))).loadPlugins();
                    } catch (Exception ex) {
                    }

                    com.myster.hash.ui.HashPreferences.init(); //no opp

                    com.myster.type.ui.TypeManagerPreferencesGUI.init();

                }
            });

            System.out.println("-------->>" + (System.currentTimeMillis() - startTime));
            if (isServer) {
            } else {
                Util.invokeAndWait(new Runnable() {
                    public void run() {
                        Preferences.initWindowLocations();
                        com.myster.client.ui.ClientWindow.initWindowLocations();
                        ServerStatsWindow.initWindowLocations();
                        com.myster.tracker.ui.TrackerWindow.initWindowLocations();
                        com.myster.hash.ui.HashManagerGUI.initGui();
                        Preferences.initGui();
                        SearchWindow.initWindowLocations();
                    }
                });
            }

            Thread.sleep(1);

            Util.invokeLater(new Runnable() {
                public void run() {
                    try {
                        com.myster.client.stream.MSPartialFile.restartDownloads();
                    } catch (IOException ex) {
                        System.out.println("Error in restarting downloads.");
                        ex.printStackTrace();
                    }
                }
            });

            com.myster.hash.HashManager.start();
            ServerFacade.assertServer();
            IPListManagerSingleton.getIPListManager();
            FileTypeListManager.getInstance();
        } catch (InterruptedException ex) {
            ex.printStackTrace(); //never reached.
        }
    } //Utils, globals etc.. //These variables are System wide variables //
}