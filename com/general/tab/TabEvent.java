/* 
	Main.java

	Title:			Server Stats Window Test App
	Author:			Andrew Trumper
	Description:	An app to test the server stats window
*/

package com.general.tab;


import java.awt.*;
import java.util.*;
import java.awt.event.*;


public class TabEvent extends EventObject  {
	private int tabid=-1;
	private Object tabObject=null; //null = -1
	
	public TabEvent(TabPanel parent, int tabid) {
		super(parent); //doggon super parent!
		this.tabid=tabid;
		this.tabObject=tabObject;
	}
	
	public int getTabID() {
		return tabid;
	}
}