/* 
	Main.java

	Title:			Server Stats Window Test App
	Author:			Andrew Trumper
	Description:	An app to test the server stats window
*/

package com.myster.server.ui;


import java.awt.*;
import com.general.tab.*;
import java.awt.image.*;
import com.general.mclist.*;
import com.myster.server.event.*;
import com.myster.server.stream.*;
import com.myster.server.ServerFacade;
import com.myster.filemanager.FileTypeListManager;
import com.myster.util.MysterThread;
import com.general.util.LinkedList;
import com.general.util.Util;

public class StatsInfoPanel extends Panel{
	Label numsearchlabel, listoflastten;
	CountLabel numsearch;
	
	Label searchperhourlabel;
	CountLabel searchperhour;
	
	XItemList lastten;
	
	Label numofdllabel;
	CountLabel numofld;
	
	
	
	Label numofSSRLabel;
	CountLabel numofSSR; 	//server stats
	
	
	Label numofTTLabel;
	CountLabel numofTT;  	//top ten
	
	
	Label numofFILabel;
	CountLabel numofFI;	//files shared
	
	Label numMatchesLabel;
	CountLabel numMaches;
	
	
	Label transferedLabel;
	ByteCounter transfered;
	
	ServerEventManager server;
	
	SearchPerHour searches=null;

	public StatsInfoPanel() {

		setBackground(new Color(240,240,240));
		server=ServerFacade.getServerDispatcher();
		server.addConnectionManagerListener(new ConnectionHandler());
		
		//Load stuff
		init();
		
		searches=new SearchPerHour();
		searches.start();
	}
	
	private void init() {
		setLayout(null);
		numsearchlabel=new Label("Number of Searches:");
		numsearchlabel.setSize(150, 25);
		numsearchlabel.setLocation(20, 30);
		add(numsearchlabel);
		
		numsearch=new CountLabel("0");
		numsearch.setSize(50, 25);
		numsearch.setLocation(200, 30);
		add(numsearch);
		
		listoflastten=new Label("Last Ten Search Strings");
		listoflastten.setSize(250, 25);
		listoflastten.setLocation(300, 30);
		add(listoflastten);
		
		lastten=new XItemList(10);
		lastten.setLocation(300, 60);
		add(lastten);
		lastten.setSize(300-20, 100);
		//lastten.runTester();
		
		searchperhourlabel=new Label("Searches in the last hour:");
		searchperhourlabel.setSize(150,25);
		searchperhourlabel.setLocation(20, 60);
		add(searchperhourlabel);
		
		searchperhour=new CountLabel("0");
		searchperhour.setSize(50,25);
		searchperhour.setLocation(200, 60);
		add(searchperhour);
		
		numMatchesLabel=new Label("Search Matches:");
		numMatchesLabel.setSize(150, 25);
		numMatchesLabel.setLocation(20, 90);
		add(numMatchesLabel);
		
		numMaches=new CountLabel("0");
		numMaches.setSize(50, 25);
		numMaches.setLocation(200, 90);
		add(numMaches);
		
		numofdllabel=new Label("Number Of Downloads:");
		numofdllabel.setSize(150, 25);
		numofdllabel.setLocation(20, 200);
		add(numofdllabel);
		
		numofld=new CountLabel("0");
		numofld.setSize(50, 25);
		numofld.setLocation(200, 200);
		add(numofld);
		
		numofTTLabel=new Label("Number Of Top Ten Requests:");
		numofTTLabel.setSize(175, 25);
		numofTTLabel.setLocation(20, 230);
		add(numofTTLabel);
		
		numofTT=new CountLabel("0");
		numofTT.setSize(50, 25);
		numofTT.setLocation(200, 230);
		add(numofTT);
		
		numofFILabel=new Label("File Info Requests:");
		numofFILabel.setSize(175,25);
		numofFILabel.setLocation(20, 260);
		add(numofFILabel);
		
		numofFI=new CountLabel("0");
		numofFI.setSize(50, 25);
		numofFI.setLocation(200, 260);
		add(numofFI);
		
		numofSSRLabel=new Label("Server Stats Requests:");
		numofSSRLabel.setSize(175, 25);
		numofSSRLabel.setLocation(20, 290);
		add(numofSSRLabel);
		
		numofSSR=new CountLabel("0");
		numofSSR.setSize(50, 25);
		numofSSR.setLocation(200, 290);
		add(numofSSR);
		
		transferedLabel=new Label("Amount Transfered:");
		transferedLabel.setSize(175, 25);
		transferedLabel.setLocation(20, 320);
		add(transferedLabel);
		
		transfered=new ByteCounter();
		transfered.setValue(0);
		transfered.setSize(50, 25);
		transfered.setLocation(200, 320);
		add(transfered);
	}
	
	private Image doubleBuffer;		//adds double buffering
	public void update(Graphics g) {
		if (doubleBuffer==null) {
			doubleBuffer=createImage(600,400);
		}
		Graphics graphics=doubleBuffer.getGraphics();
		paint(graphics);
		g.drawImage(doubleBuffer, 0, 0, this);
	}
	
	
	public void paint(Graphics g) {
		FontMetrics metrics=getFontMetrics(getFont());
		
		String msg1="Search Stats";
		
		g.setColor(new Color(150,150,150));
		g.drawRect(10,10,580, 170);
		g.setColor(getBackground());
		g.fillRect(15,9, metrics.stringWidth(msg1)+10, 2);
		
		g.setColor(getForeground());
		g.drawString(msg1, 20, 15);
		
	}
	
	private class ConnectionHandler implements ConnectionManagerListener {
		public void sectionEventConnect(ConnectionManagerEvent e) {
			if (e.getSection()==RequestSearchThread.NUMBER) {
				numsearch.increment();
				searches.addSearch(e.getTimeStamp());
				((ServerSearchDispatcher)(e.getSectionObject())).addServerSearchListener(new SearchHandler());
			} else if (e.getSection()==FileSenderThread.NUMBER) {
				numofld.increment();
				((FileSenderThread.ServerTransfer)(e.getSectionObject())).getDispatcher().addServerDownloadListener(new DownloadHandler());
			} else if (e.getSection()==IPLister.NUMBER) {
				numofTT.increment();
			} else if (e.getSection()==HandshakeThread.NUMBER) {
				numofSSR.increment();
			} else if (e.getSection()==FileInfoLister.NUMBER) {
				numofFI.increment();
			}
			
		}
		public void sectionEventDisconnect(ConnectionManagerEvent e) {

		}
	}
	
	private class SearchHandler extends ServerSearchListener {
	
		public void searchRequested(ServerSearchEvent e) {
			lastten.add(e.getSearchString()+" ("+e.getType()+")");
		}
		
		public void searchResult(ServerSearchEvent e) {
			numMaches.increment();
		}
	}
	
	private class SearchPerHour extends MysterThread {
		boolean flag=true;
		
		LinkedList list=new LinkedList();
		
		public void run() {
			do {
				searchperhour.setValue(calculateSearchesPerHour());
				try {
					sleep(5000);
				} catch (InterruptedException ex) {
					//.. nothing
				}
			} while (flag);
		}
		
		public int calculateSearchesPerHour() {
			if (list.getTail()==null) return 0;
			while (1000*60*60<(System.currentTimeMillis()-((Long)(list.getTail())).longValue())) {
				list.removeFromTail();
				if (list.getTail()==null) return 0;
			}
			return list.getSize();
		}
		
		public void addSearch(long time) {
			list.addToHead(new Long(time));
		}
		
		public void end() {
			flag=false;
			interrupt();
			try {join();} catch (InterruptedException ex) {}
		}
	}
	
	private class DownloadHandler extends ServerDownloadListener {
		public void downloadStarted(ServerDownloadEvent e) {}
		public void blockSent(ServerDownloadEvent e) {}
		public void downloadFinished(ServerDownloadEvent e) {
			transfered.setValue(transfered.getValue()+e.dataSoFar());
		}
	}
	
	private class ByteCounter extends Label {
		long value=0;
		
		public long getValue() {
			return value;
		}
		
		public synchronized void setValue(long i) {
			value=i;
			setUpdateLabel();
		}
		
		private synchronized void setUpdateLabel() {
			setText(Util.getStringFromBytes(value));
		}
	}
}