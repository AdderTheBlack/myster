package com.myster.ui;

import java.awt.Rectangle;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.Toolkit;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.NoSuchElementException;

import com.myster.pref.*;



/**
*	Creates a window keeper object for your class of window. A window keeper
*	takes upon itself the responsibility of managing the location preferences for your
*	window type. Simply create a window keeper for you class of window and add
*	every new frame of that class. Window keeper will keep track of that window's
*	location and visibility. In order to read back this information simply call
*	getLastLocs(..) for your window type. Window keeper will return the locations
*	for every window of that type that was on screen last time Myster was quit.
*	It will even makes sure the previous window locs are
*	all on screen when the routine is called (ie: not too far to the left or right).
*
*/

public class WindowLocationKeeper {


	private String key;
	private volatile int counter=0;

	public WindowLocationKeeper(String key) {
		init();
		if (key.indexOf("/")!=-1) throw new RuntimeException("Key cannot contain a \"/\"!");
		this.key="/"+key+"/";
	}
	
	
	/**
	*	Adds a Frame object to be *tracked*. If the frame is visible it's location is automatically
	*	stored in the prefs else it won't be.
	*/
	public void addFrame(Frame frame) {
		final int privateID=counter++;
		
		if (frame.isVisible()) {
			saveLocation(frame, privateID);
		}
		
		frame.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				if (((Component)(e.getSource())).isVisible()) saveLocation(((Component)(e.getSource())), privateID);
			}

			public void componentMoved(ComponentEvent e) {
				if (((Component)(e.getSource())).isVisible()) saveLocation(((Component)(e.getSource())), privateID);
			}

			public void componentShown(ComponentEvent e) {
				saveLocation(((Component)(e.getSource())), privateID);
			}

			public void componentHidden(ComponentEvent e) {
				prefs.remove(key+privateID);
				Preferences.getInstance().put(PREF_KEY,prefs);
			}

		});
	}
	 
	private void saveLocation(Component c, int id) {
		prefs.put(key+id, rect2String(c.getBounds()));

		Preferences.getInstance().put(PREF_KEY,prefs);
	}
	










/////////// STATIC SUB SYSTEM

	private static final String PREF_KEY="Window Locations and Sizes/";
	
	private static PreferencesMML prefs=new PreferencesMML();
	private static PreferencesMML oldPrefs;

	private static boolean initFlag=false;

	public static synchronized void init() {
		if (initFlag) return; //dont init twice.
		
		initFlag=true;
		oldPrefs=new PreferencesMML(Preferences.getInstance().getAsMML(PREF_KEY, new PreferencesMML()).copyMML());
		System.out.println(""+Preferences.getInstance().getAsMML(PREF_KEY));
	}
	
	public static boolean fitsOnScreen(Rectangle rect) {
		Rectangle screenBorders=new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
		return (rect.x>0 
				&& rect.y>0 
				&& (rect.x+50)<screenBorders.width 
				&& rect.y+50<screenBorders.height);
	}

	public static synchronized Rectangle[] getLastLocs(String key) {
		init();
		//System.out.println(oldPrefs.toString());
		key="/"+key+"/";
		
		Vector keyList=oldPrefs.list(key);
		
		if (keyList==null) return new Rectangle[]{}; //aka Rectangle[0];
		
		Rectangle[] rectangles=new Rectangle[keyList.size()];
		
		for (int i=0; i<keyList.size(); i++) {
			rectangles[i]=string2Rect(oldPrefs.get(key+(String)(keyList.elementAt(i)), "0,0,400,400"));
			if (!fitsOnScreen(rectangles[i])) rectangles[i].setLocation(50,50);
			//System.out.println(key+(String)(keyList.elementAt(i)));
		}
		
		
		
		return rectangles;
	}
	
	private static synchronized Rectangle string2Rect(String s) {
		int x,y,width,height;
		
		StringTokenizer tokenizer=new StringTokenizer(s,SEPERATOR, false);
		
		try {
			x		= Integer.parseInt(tokenizer.nextToken());
			y		= Integer.parseInt(tokenizer.nextToken());
			width	= Integer.parseInt(tokenizer.nextToken());
			height	= Integer.parseInt(tokenizer.nextToken());
		} catch (NoSuchElementException ex) {
			return new Rectangle(0,0,100,100);
		} catch (NumberFormatException ex) {
			return new Rectangle(0,0,100,100);
		}
		
		return new Rectangle(x,y,width,height);
	}

	private static final String SEPERATOR=",";
	
	private static String rect2String(Rectangle rect) { //here for code reuse
		return ""+rect.x+SEPERATOR+rect.y+SEPERATOR+rect.width+SEPERATOR+rect.height;
	}
}