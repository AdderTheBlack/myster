package com.myster.bandwidth;

import java.util.Vector;

import com.general.events.EventListener;
import com.general.events.GenericEvent;
import com.general.events.SyncEventDispatcher;
import com.general.util.Semaphore;
import com.general.util.Timer;
import com.general.util.MrWrap;
import com.myster.pref.ui.PreferencesPanel;
import com.myster.mml.RobustMML;
import com.myster.pref.Preferences;

import java.awt.*;
import java.awt.event.*;

/**
*	The bandwidth manager is a set of static funcitons that allow multiple dowload/upload threads to
*	make sure their total bandwidth utilisation doesn't exeed a preset amount.
*/

public class BandwidthManager {
	static SomeStruct data=new SomeStruct();

	public static final int requestBytesIncoming(int maxBytes) {
		return maxBytes;
	}
	
	
	public static final int requestBytesOutgoing(int maxBytes) {
		if (!data.outgoingIsEnabled) return maxBytes;
		
		return data.outgoingImpl.requestBytes(maxBytes);
	}
	
	
	//////////GUI
	public static PreferencesPanel getPrefsPanel() {
		return new BandwithPrefsPanel();
	}
	
	
	
	//////////PREFS
	private static final String OUTGOING_ENABLED="/Outgoing Enabled";
	private static final String INGOMMING_ENABLED="/Incomming Enabled";
	private static final String KEY_IN_PREFS="BANDWIDTH PREFS";
	private static final String OUTGOING_MAX="/Outgoing Max";
	private static final String INGOMMING_MAX="/Incomming Max";
	private static final String TRUE_S="TRUE";
	private static final String FALSE_S="FALSE";
	

	public static synchronized boolean isOutgoingEnabled() 	{ return data.prefMML.query(OUTGOING_ENABLED).equals(TRUE_S); }
	public static synchronized boolean isIncommingEnabled() 	{ return data.prefMML.query(INGOMMING_ENABLED).equals(TRUE_S); }
	
	public static synchronized boolean setOutgoingEnabled(boolean enabled) { return data.setOutgoingEnabled(enabled); }
	public static synchronized boolean setIncommingEnabled(boolean enabled) { return data.setIncommingEnabled(enabled); }
	
	public static synchronized int getOutgoingMax() {return data.getOutgoingMax();}
	public static synchronized int getIncommingMax() {return data.getIncommingMax();}

	public static synchronized int setOutgoingMax(int max) { return data.setOutgoingMax(max); }
	public static synchronized int setIncommngMax(int max) { return data.setIncommngMax(max); }


	private static class SomeStruct { //hack..
		public boolean outgoingIsEnabled=false;
		public Bandwidth outgoingImpl=new BandwidthImpl();
		
		public RobustMML prefMML;
	
		public SomeStruct() { 
			prefMML=(RobustMML)(Preferences.getInstance().getAsMML(BandwidthManager.KEY_IN_PREFS));
			
			if (prefMML==null) prefMML=new RobustMML();
			
			outgoingImpl.setRate(getOutgoingMax());
			outgoingIsEnabled=isOutgoingEnabled();
		}
		
		
		public synchronized boolean isOutgoingEnabled() 	{ return prefMML.query(OUTGOING_ENABLED).equals(TRUE_S); }
		public synchronized boolean isIncommingEnabled() 	{ return prefMML.query(INGOMMING_ENABLED).equals(TRUE_S); }
		
		public synchronized boolean setOutgoingEnabled(boolean enabled) { 
			outgoingIsEnabled=enabled;
			return setBoolInPrefs(OUTGOING_ENABLED, enabled);
		}
		
		public synchronized boolean setIncommingEnabled(boolean enabled) { 
			return setBoolInPrefs(INGOMMING_ENABLED, enabled);
		}
		
		private synchronized boolean setBoolInPrefs(String path, boolean bool) {
			prefMML.put(path, (bool?TRUE_S:FALSE_S));
			return bool;
		}
		
		public synchronized int getOutgoingMax() {return getIntFromPrefs(OUTGOING_MAX, 10);}
		public synchronized int getIncommingMax() {return getIntFromPrefs(INGOMMING_MAX, 10);}
		
		private synchronized int getIntFromPrefs(String path, int defaultNum) {
			try {
				return Integer.parseInt(prefMML.query(path));
			} catch (NumberFormatException ex) {
				return defaultNum;
			}
		}
		
		public synchronized int setOutgoingMax(int max) { 
			if (max<2) max=2;
			
			prefMML.put(OUTGOING_MAX, ""+max);
			Preferences.getInstance().put(KEY_IN_PREFS, prefMML);
			outgoingImpl.setRate(max);
			
			return max;
		}
		
		public synchronized int setIncommngMax(int max) {
			if (max<2) max=2;
			
			prefMML.put(INGOMMING_MAX, ""+max);
			Preferences.getInstance().put(KEY_IN_PREFS, prefMML);
			
			return max;
		}
	}
}




class BlockedThread {
	public static volatile double rate=10.24;
	Vector threads;
	double bytesLeft;
	Thread thread;
	
	BlockedThread(int bytesLeft, Vector threads, double rate) {
		this.bytesLeft=bytesLeft;
		thread=Thread.currentThread();
		this.threads=threads;
		this.rate=rate;
	}
	
	private void subStractBytes(int bToSub) {
		double bToSubD=bToSub;
		bytesLeft-=bToSubD;
	}
	
	public synchronized void reSleep() {

			if (thread!=null) thread.interrupt();
		
	}
	
	public synchronized void sleepNow() {
		try {
			for (;;) {
				double thisRate;int sleepAmount;long startTime;

					 thisRate=((double)(threads.size()))/rate;
					 sleepAmount=(int)(bytesLeft*thisRate);
					 startTime=System.currentTimeMillis();
					if (sleepAmount<=0) {
						thread=null;
						try {wait(1);} catch (InterruptedException ex) {}
						return;
					}
				
				
				try {
					wait(sleepAmount);
				} catch (InterruptedException ex) {
						double timeSlept=(System.currentTimeMillis()-startTime);
						subStractBytes((int)(timeSlept/thisRate)); //avoid divide by 0
					continue;
				}
				return;
			}
		} finally {
			thread=null;
		}
	}
}



class BandwithPrefsPanel extends PreferencesPanel {
	public static final int STD_XSIZE=450;
	public static final int STD_YSIZE=300;
	
	Panel explanationPanel=new MessagePanel(
		"Using the bandwidth preference pannel you can set the maximum rate "+
		"at which Myster will send and recieve data. This setting is usefull "+
		"if you want to run a Myster server while using the internet but find "+
		"that it slows down your internet connection.");
	
	Checkbox enableOutgoing;
	Checkbox enableIncomming;
	
	Label outgoingSpeedLabel;
	Label incommingSpeedLabel;
	
	TextField incommingBytesField;
	TextField outgoingBytesField;
	
	Label outgoingUnitsLabel;
	Label incommingUnitsLabel;
	
	public BandwithPrefsPanel() {
		setLayout(null);
		explanationPanel.setLocation(0,0);
		explanationPanel.setSize(STD_XSIZE,STD_YSIZE/3);
		add(explanationPanel);
		
		int nextOff=STD_YSIZE/3;
		nextOff+=10;
	
		enableOutgoing=new Checkbox("Enable Outgoing Throttling");
		enableOutgoing.setLocation(10, nextOff);
		enableOutgoing.setSize(200, 20);
		enableOutgoing.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean state=false;
				if (e.getStateChange()==ItemEvent.SELECTED) {
					state=true;
				}
				
				setOutgoingEnable(state);
			}
		});
		add(enableOutgoing);
		
		nextOff+=25;
		
		outgoingSpeedLabel=new Label("Limit speed to: ");
		outgoingSpeedLabel.setLocation(15, nextOff);
		outgoingSpeedLabel.setSize(150, 20);
		add(outgoingSpeedLabel);
		
		outgoingBytesField=new TextField("10");
		outgoingBytesField.setLocation(15+150, nextOff);
		outgoingBytesField.setSize(50, 20);
		add(outgoingBytesField);
		
		outgoingUnitsLabel=new Label("Kilo BYTES / second");
		outgoingUnitsLabel.setLocation(15+50+150, nextOff);
		outgoingUnitsLabel.setSize(200, 20);
		add(outgoingUnitsLabel);
		
		nextOff+=40;
		
		enableIncomming=new Checkbox("Enable Incomming Throttling");
		enableIncomming.setLocation(10, nextOff);
		enableIncomming.setSize(200, 20);
		enableIncomming.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean state=false;
				if (e.getStateChange()==ItemEvent.SELECTED) {
					state=true;
				}
				
				setIncommingEnable(state);
			}
		});
		enableIncomming.setEnabled(false);
		add(enableIncomming);
		
		nextOff+=25;
		
		incommingSpeedLabel=new Label("Limit speed to: ");
		incommingSpeedLabel.setLocation(15, nextOff);
		incommingSpeedLabel.setSize(150, 20);
		incommingSpeedLabel.setEnabled(false);
		add(incommingSpeedLabel);
		
		incommingBytesField=new TextField("10");
		incommingBytesField.setLocation(15+150, nextOff);
		incommingBytesField.setSize(50, 20);
		incommingBytesField.setEnabled(false);
		add(incommingBytesField);
		
		incommingUnitsLabel=new Label("Kilo BYTES / second");
		incommingUnitsLabel.setLocation(15+50+150, nextOff);
		incommingUnitsLabel.setSize(200, 20);
		incommingUnitsLabel.setEnabled(false);
		add(incommingUnitsLabel);
	}

	public void save() {	//save changes
		BandwidthManager.setOutgoingEnabled(enableOutgoing.getState());
		try {
			BandwidthManager.setOutgoingMax(Integer.parseInt(outgoingBytesField.getText()));
		} catch (NumberFormatException ex) {}
	}
	
	public void reset()	{ 	//discard changes and reset values to their defaults.
		setOutgoingEnable(BandwidthManager.isOutgoingEnabled());
		outgoingBytesField.setText(""+BandwidthManager.getOutgoingMax());
	}
	
	public String getKey() { return "Bandwidth"; }//gets the key structure for the place in the pref panel
	
	public java.awt.Dimension getPreferredSize() {
		return new java.awt.Dimension(STD_XSIZE,STD_YSIZE);
	}
	
	private void setOutgoingEnable(boolean bool) {
		enableOutgoing.setState(bool);
		
		outgoingSpeedLabel.setEnabled(bool);
		outgoingBytesField.setEnabled(bool);
		outgoingUnitsLabel.setEnabled(bool);
	}
	
	private void setIncommingEnable(boolean bool) {
		enableIncomming.setState(bool);
		
		incommingSpeedLabel.setEnabled(bool);
		incommingBytesField.setEnabled(bool);
		incommingUnitsLabel.setEnabled(bool);
	}
}

class MessagePanel extends Panel { //candidate for re-use
	int height;
	int ascent;
	int descent;
	
	FontMetrics metrics;
	
	String message;
	
	Vector messageVector=new Vector(20);
	
	public MessagePanel(String message) {
		this.message=message;
	}
	
	public java.awt.Dimension getPreferredSize() {
		return getSize();
	}

	private void doMessageSetup() {
		metrics=getFontMetrics(getFont());
		
		height=metrics.getHeight();
		ascent=metrics.getAscent();
		descent=metrics.getDescent();

		
		MrWrap wrapper=new MrWrap(message, 380, metrics);
		for (int i=0; i<wrapper.numberOfElements(); i++) {
			messageVector.addElement(wrapper.nextElement());
		}
	}
	
	public void paint(Graphics g) {
		if (metrics==null) doMessageSetup();
		g.setColor(Color.black);
		for (int i=0; i<messageVector.size(); i++) {
			g.drawString(messageVector.elementAt(i).toString(), 10, 5+height*(i)+ascent);
		}
	}
}

class BandwidthImpl implements Bandwidth {
	Vector transfers = new Vector(50);
	double rate=10;
	
	public synchronized void reSleepAll() {
		for (int i=0; i< transfers.size(); i++) {
			BlockedThread t=(BlockedThread)(transfers.elementAt(i));
			t.reSleep();
		}
	}
	
	
	//NOTE: I RE-WROTE THE BELOW 03/01/04 It works well but I need to get rid of the VECTOR <-
	public final int requestBytes(int maxBytes) {
		BlockedThread b=new BlockedThread(maxBytes,transfers, rate);
		
		synchronized (this) {
			transfers.addElement(b);
			reSleepAll();
		}
		
		b.sleepNow();
		
		synchronized (this) {
			transfers.removeElement(b);
			reSleepAll();
		}
		return maxBytes;
	}
	
	public final synchronized void setRate(double rate) {
		this.rate=(rate*1024)/1000; //rate was in k/s but not now.
		System.out.println("rate is now :"+rate);
	}
}

interface Bandwidth {
	public int requestBytes(int maxBytes);
	public void setRate(double rate);
}



/*
class AlreadyImplementedException extends Exception {
	public AlreadyImplementedException(String s) {
		super(s);
	}
}
*/	
