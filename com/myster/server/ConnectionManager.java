
/* 

	Title:			Myster Open Source
	Author:			Andrew Trumper
	Description:	Generic Myster Code
	
	This code is under GPL

Copyright Andrew Trumper 2000-2001
*/

package com.myster.server;

import java.net.*;
import java.io.*;
import java.net.Socket;
import com.general.util.*;
import com.myster.server.stream.*;
import com.myster.filemanager.*;
import com.myster.server.event.*;
import java.util.Hashtable;
import com.myster.net.MysterAddress;
import com.myster.util.MysterThread;
import com.myster.transferqueue.TransferQueue;

/**
*	This class is responsible fore dealing with a conneciton with a client.
*	Basically it detects the type of service the client desires and imploys the appropriate protocal object.
*
*/

public class ConnectionManager extends MysterThread {
	final int BUFFERSIZE=512;
	Socket socket;
	ServerEventManager eventSender;
	BlockingQueue socketQueue;
	
	private TransferQueue transferQueue;
	
	private Hashtable connectionSections=new Hashtable();
	
	private ConnectionContext context;
	
	private static volatile int threadCounter=0;

	
	public ConnectionManager(BlockingQueue q, ServerEventManager eventSender, TransferQueue transferQueue, Hashtable connectionSections) {
		super("Server Thread "+(++threadCounter));
		
		socketQueue=q;
		this.transferQueue=transferQueue;
		this.eventSender=eventSender;
		this.connectionSections=connectionSections;
	}
	
	public void run() {
		while(true) {
			try {
				doConnection();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}
	
	private void doConnection() {
		try {
			socket=(Socket)(socketQueue.get());
		} catch (InterruptedException ex) {
			//should never happen
			return;//exit quickly in case it's being called by System.exit();
		}
		
		eventSender.fireOEvent(new OperatorEvent(OperatorEvent.CONNECT, new MysterAddress(socket.getInetAddress())));

		int sectioncounter=0;
		try {
		
			context=new ConnectionContext();
			context.socket=new com.myster.client.stream.TCPSocket(socket);
			context.transferQueue=transferQueue;
			context.serverAddress=new MysterAddress(socket.getInetAddress());
			
			DataInputStream i=new DataInputStream(socket.getInputStream());	//opens the connection

			int protocalcode;
			setPriority(Thread.MIN_PRIORITY);
			
			do {
				try {
					Thread.currentThread().yield();
					protocalcode=i.readInt();						//reads the type of conneciton requested
				} catch (Exception ex) {
					Thread.currentThread().yield();
					return;
				}
				
				sectioncounter++; //to detect if it was a ping.
				
				
				
				//Figures out which object to invoke for the connection type:
				//NOTE: THEY SAY RUN() NOT START()!!!!!!!!!!!!!!!!!!!!!!!!!
				MysterAddress remoteip = new MysterAddress(socket.getInetAddress().getHostAddress());

				switch (protocalcode) {
					case 1:
						{DataOutputStream out=new DataOutputStream(socket.getOutputStream());
						out.write(1);}			//Tells the other end that the command is good bad!
						break;
					case 2:
						{DataOutputStream out=new DataOutputStream(socket.getOutputStream());
						out.write(1);}			//Tells the other end that the command is good bad!
						return;
					default:
						ConnectionSection section=(ConnectionSection)(connectionSections.get(new Integer(protocalcode)));
						if (section==null) {
							System.out.println("!!!System detects unknown protocol number : "+protocalcode);
							{DataOutputStream out=new DataOutputStream(socket.getOutputStream());
							out.write(0);}			//Tells the other end that the command is bad!
						} else {
							doSection(section, remoteip, context);
						} 
				}				
			} while(true);
		} catch (IOException ex) {
			
		} finally {
			
			if (sectioncounter==0) eventSender.fireOEvent(new OperatorEvent(OperatorEvent.PING, new MysterAddress(socket.getInetAddress())));
			
			eventSender.fireOEvent(new OperatorEvent(OperatorEvent.DISCONNECT, new MysterAddress(socket.getInetAddress())));
			
			close(socket);
		}
		
	}
	
	private void fireConnectEvent(ConnectionSection d, MysterAddress remoteAddress, Object o) {
		eventSender.fireCEvent(new ConnectionManagerEvent(ConnectionManagerEvent.SECTIONCONNECT, remoteAddress, d.getSectionNumber(), o));
	}
	
	private void fireDisconnectEvent(ConnectionSection d, MysterAddress remoteAddress, Object o) {
		eventSender.fireCEvent(new ConnectionManagerEvent(ConnectionManagerEvent.SECTIONDISCONNECT, remoteAddress, d.getSectionNumber(), o));
	}
	
	private void doSection(ConnectionSection d, MysterAddress remoteIP, ConnectionContext context) throws IOException {
		Object o=d.getSectionObject();
		context.sectionObject=o;
		fireConnectEvent(d, remoteIP, o);
		try {
			d.doSection(context);
		} finally {
			fireDisconnectEvent(d, remoteIP, o);
		}
	}
	
	private void waitMils(long w) {
		try {
			sleep(w);
		} catch (InterruptedException ex) {
		
		}
	}
	
	private static void close(Socket s) {
		try {s.close();} catch (Exception ex){}
	}
	
}