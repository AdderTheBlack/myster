/* 

	Title:			Myster Open Source
	Author:			Andrew Trumper
	Description:	Generic Myster Code
	
	This code is under GPL

Copyright Andrew Trumper 2000-2001
*/

package com.myster.server.stream;

import java.net.*;
import java.io.*;
import java.awt.*;
import com.myster.tracker.IPListManagerSingleton;
import java.awt.Dimension;
import com.general.util.*;
import com.myster.filemanager.*;
import Myster;
import com.myster.server.event.*;
import com.myster.server.DownloadInfo;
import com.general.events.EventDispatcher;
import com.myster.server.DownloadQueue;
import com.myster.server.QueuedTransfer;
import com.myster.server.ConnectionContext;
import com.myster.net.MysterAddress;
import com.myster.pref.Preferences;
import com.myster.client.stream.StandardSuite;
import com.myster.net.MysterSocketFactory;

public class FileSenderThread extends ServerThread {
	//public constants
	public static final int NUMBER=80;
	
	public FileSenderThread() {
	
	}
	
	public int getSectionNumber() {
		return NUMBER;
	}
	
	public Object getSectionObject() {
		return new ServerTransfer(FileTypeListManager.getInstance());
	}
	
	public void section(ConnectionContext context) throws IOException {
		ServerTransfer transfer=(ServerTransfer)(context.sectionObject);
		
		try {
			transfer.init(context.socket);
			
			
			if (kickFreeloaders()) {
				try {
					//if (!context.serverAddress.getIP().equals("127.0.0.1")) {
						StandardSuite.disconnectWithoutException(MysterSocketFactory.makeStreamConnection(context.serverAddress));
					//}
				} catch (Exception ex) { //if host is not reachable it will end up here.
					transfer.freeloaderComplain();
					throw new IOException("Client is a leech."); //bye bye..
				}
			}
			
			if (context.downloadQueue.addDownloadToQueue(transfer.getQueuedTransfer())) {		
				try {
					transfer.waitUntilDone();
				} catch (InterruptedException ex) {
					throw new IOException("Interrupted IO.");
				}
			} else {
				throw new IOException("Server downloads are overloaded."); //bye bye.. Server is over loaded.
			}
		} catch (IOException ex) {
			transfer.cleanUp(); //does usefull things like fires an event to say download is dead.
			throw ex;
		}
	}
	
	static FreeLoaderPref p;
	public static synchronized FreeLoaderPref getPrefPanel() {
		if (p==null) p=new FreeLoaderPref();
		return p;
	} 
	
	private static String freeloadKey="ServerFreeloaderKey/";
	private static boolean kickFreeloaders() {
		boolean b_temp=false;
		
		try {
			b_temp=Boolean.valueOf(Preferences.getInstance().get(freeloadKey)).booleanValue();
		} catch (NumberFormatException ex) {
			//nothing
		} catch (NullPointerException ex) {
			//nothing
		}
		return b_temp;
	}
	
	private static void setKickFreeloaders(boolean b) {
		Preferences.getInstance().put(freeloadKey, ""+b);
	}	

	public static class FreeLoaderPref extends Panel {	
		private final Checkbox freeloaderCheckbox;
		
		public FreeLoaderPref() {
			setLayout(new FlowLayout());
		
			freeloaderCheckbox=new Checkbox("Kick Freeloaders");
			add(freeloaderCheckbox);
		}
		
		public void save() {
			setKickFreeloaders(freeloaderCheckbox.getState());
		}
		
		public void reset() {
			freeloaderCheckbox.setState(kickFreeloaders());
		}
		
		public Dimension getPreferredSize() {
			return new Dimension(100, 1);
		}
	}
	
	
	public static class ServerTransfer {
		//Events
		ServerDownloadDispatcher dispatcher=new ServerDownloadDispatcher(new Stats());
		
		//Server stats
		long bytessent=0;
		String filename="?";
		String filetype="?";
		long filelength=0;
		long starttime=1;
		String remoteIP="?";
		long initialOffset=0;
		
		//Threading
		Semaphore sem=new Semaphore(0);
		
		//?
		boolean endflag=false;
		
		//io
		private File file;
		private Socket socket;
		private DataInputStream in, fin;
		private DataOutputStream out, fout;
		
		//Managers
		private FileTypeListManager typelist;
		
		//private constants
		private static final String IMAGE_DIRECTORY=new String("Images/");
		private static final int BUFFERSIZE=8192;
		private static final int BURSTSIZE=512*1024;	
			
		public ServerTransfer (FileTypeListManager t) {
			typelist=t;
			starttime=System.currentTimeMillis();
		}
		
		public ServerDownloadDispatcher getDispatcher() {
			return dispatcher;//hurray!
		}
		
		public QueuedTransfer getQueuedTransfer() {
			return new DQueuePrivateClass();
		}
		
		public void freeloaderComplain() throws IOException {
			byte[] queuedImage=new byte[4096];
			int tempint;
			InputStream qin=this.getClass().getResourceAsStream("firewall.gif");
			
			//loading image...
			do {
				tempint=qin.read(queuedImage, sizeOfImage,4096-sizeOfImage);
				if (tempint>0) sizeOfImage+=tempint;
			} while (tempint!=-1);
			
			//mapping errors.
			if (sizeOfImage==-1) sizeOfImage=-1;
			if (sizeOfImage==4096) sizeOfImage=-1;

			out.writeInt(6669);
			out.writeByte('i');
			out.writeLong(sizeOfImage);
			out.write(queuedImage, 0, sizeOfImage);
		}
		
		
		byte[] queuedImage;
		int sizeOfImage=0;
		private void refresh(int position) throws IOException {
			if (endflag) throw new IOException("Thread is dead.");
			fireEvent(ServerDownloadEvent.QUEUED, position);
			try {
				if (queuedImage==null) { //if not loaded then load.
					queuedImage=new byte[4096];
					int tempint;
					InputStream qin=this.getClass().getResourceAsStream("queued.gif");
					
					//loading image...
					do {
						tempint=qin.read(queuedImage, sizeOfImage,4096-sizeOfImage);
						if (tempint>0) sizeOfImage+=tempint;
					} while (tempint!=-1);
					
					//mapping errors.
					if (sizeOfImage==-1) sizeOfImage=-1;
					if (sizeOfImage==4096) sizeOfImage=-1;
				}
				
				sendQueue(position);
			
				out.writeInt(6669);
				out.writeByte('i');
				out.writeLong(sizeOfImage);
				out.write(queuedImage, 0, sizeOfImage);
	
			} catch (IOException ex) {
				throw ex;
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				throw ex;
			}
		}
	
		private void waitUntilDone() throws InterruptedException {
			sem.getLock();
		}
	
		
		
		private void init(Socket socket) throws IOException {
			this.socket=socket; //io
			in=new DataInputStream(socket.getInputStream());
			out=new DataOutputStream(socket.getOutputStream());
		
			remoteIP=socket.getInetAddress().getHostAddress();	//stats
			
			starttime=System.currentTimeMillis();
			
			byte[] b=new byte[4];
			in.read(b,0,4);
			
			filetype=(new String(b));			//stats
			filename=new String(in.readUTF());	//stats
			file=typelist.getFile(b, filename);	//io
			filelength=file.length();			//stats
			initialOffset=in.readLong();		//initial offset for restarting file transfers half way done!
			bytessent=initialOffset;			//hereafter refered to as....
			
			if (file==null||(file.length()-initialOffset)<0) {		//File does not exist or if initialoffset is larger than the file!
				out.writeInt(0);
				//loop..
				endflag=true; //<-- signals CM that thread should continue. Bit of a hack.
				cleanUp();

			}
			else {
				out.writeInt(1);
				out.writeLong(file.length()-initialOffset);			//Sends size of file that remains to be sent.
			}
			

		}
		
		
		/**
		 * 	Protcal->
		 *	Get TYPE and FILENAME and initial offset (long)
		 *	Send 1 or 0 (Int);
		 *	if 1 send LONG of filelength
		 *	send char (data type 'd' for data 'g' for graphic 'u' for URL)
		 *  send lenght of data being sent
		 *  send that data... etc.. until all of length of data (or 'd') has been sent.	
		 *
		 * eg: get ("MPG3" -> "song.mp3" -> 0)
		 *     send (3000000 -> 'i' -> 20000 -> data -> 'u' -> 13 -> <data> -> 'd' -> 1000000 -> <data>
		 * -> 'i' -> 21000 -> <data> -> 'd' -> 200000 -> <data> -> <done>.
		 */
		
		private void startDownload() {
			try {
				fireEvent(ServerDownloadEvent.STARTED, -1); //-1 == queued.
				if (endflag) throw new IOException("toss and catch");
				sendFile(file, out);
				
				
				try {
					in.close();
					out.close();
					socket.close();
				} catch (Exception ex) {
					//again.. I can't do anything if it fails...
					//But would still like to try and close them if the first one fails.
				}
			} catch (Exception ex) {
				try {
					socket.close();
				} catch (Exception exp) {
					//again.. I can't do anything if it fails...
					//But would still like to try and close them if the first one fails.
				}
			} finally {
				cleanUp();
			}
		}
		
		private boolean duplicate=false;
		private void cleanUp() {
			if (duplicate) return;
			fireEvent(ServerDownloadEvent.FINISHED, (char)-1);
			sem.signal();
			duplicate=true; //just so this is not called twice.
			endflag=true;
		}
		
		private void cleanUpFromError() {
			try {
				socket.close();
			} catch (Exception exp) {
				//again.. I can't do anything if it fails...
				//But would still like to try and close them if the first one fails.
			}
		}
	

		/**
		 * Sends the file..
		 */
		private void sendFile(File f, DataOutputStream out) throws Exception{
			//Opens connection to file...
			
			try {
				fin=new DataInputStream(new FileInputStream(f));
				long temp=fin.skip(bytessent);	//bytes sent should be 0 but may be not since initial offset is set to bytes sent.
				if (temp!=bytessent) throw new Exception("Skip() method not working right. found bug in API.AGHH");
			} catch (Exception ex) {
				throw ex;
			}

			starttime=System.currentTimeMillis();
			do {
				sendImage();
			} while (sendDataPacket()==BURSTSIZE);
		}
		
		
	
		//code 'd'
		private int sendDataPacket() {
			long bytesremaining=(BURSTSIZE<(filelength-bytessent)) ? BURSTSIZE : filelength-bytessent;
			try {
				out.writeInt(6669);
				out.writeByte('d');
				out.writeLong(bytesremaining);
			} catch (Exception ex) {return -1;}
			
		
			byte[] buffer=new byte[BUFFERSIZE];
			
			for (int j=0; j<(bytesremaining/BUFFERSIZE); j++) {
				if (readWrite(fin, BUFFERSIZE, buffer)==-1) return -1;
			}
			
			if (readWrite(fin,(int)(bytesremaining%BUFFERSIZE), buffer)==-1) return -1;
			//System.gc();
			return (int)bytesremaining;
		}
		
		
		//code 'u'
		private void sendURL() {
		
		}
	
		//code 'm'
		private void sendMessage(String m) throws IOException {
			byte[] bytes=m.getBytes();
			long length=bytes.length;
			out.writeInt(6669);
			out.write('m');
			out.writeLong(length);
			out.write(bytes);
		}
		
		//code 'q'
		private void sendQueue(int i) throws IOException {
			out.writeInt(6669);
			out.writeByte('q');
			out.writeLong(4); //an int is 4 bytes.
			out.writeInt(i);
		}
	
		//code 'i'
		private void sendImage() {
			DataInputStream in;
			File file;
			
			//get directory
			String list[];
			list=(new File(IMAGE_DIRECTORY)).list();
			
			if (list==null) {
				return;
			}
			
			
			/*
			This code gets a file from the directory at random. If it's not a graphic
			(ie: not .jpg or .gif) it cycles through every item in the directory until it
			find one. Note: This isnt totaly random, but it's good enoght most of the time.
			
			If it can't find a graphic... it eventually causes an exception and sends another data block
			*/
			RInt rint=new RInt(list.length-1);
			file=new File(IMAGE_DIRECTORY+list[rint.setValue((int)(list.length*Math.random()))]);
		
			
			for(int i=0; i<list.length; i++) {
				file=new File(IMAGE_DIRECTORY+list[rint.inc()]);
				if (file.getName().endsWith(".jpg")||file.getName().endsWith(".gif")) break;
				else {file=null;}
			}
			
			if (file==null) {
				return;
			}
			
	
	
			try {
				in=new DataInputStream(new FileInputStream(file));
			} catch (Exception ex) {
				ex.printStackTrace();
				return;
			}
			
			byte[] bytearray=new byte[(int)file.length()];
			
			//serveroutput.say("File length is "+file.length());
			
			try {
				in.read(bytearray, 0, (int)file.length());
			} catch (IOException ex) {}
			
			
			//OUTPUT::::::::
			try {
				out.writeInt(6669);
				out.write('i');
				out.writeLong(bytearray.length);
				out.write(bytearray);
	
			} catch (IOException ex) {}
			
			try {
				in.close();
			} catch (IOException ex) {}
			
		}
		
		
		private int readWrite(DataInputStream in, int size, byte[] buffer) {
			if (size==0) return 0;
			try {
				in.read(buffer,0,size);
				out.write(buffer,0,size);
				bytessent+=size;
			} catch (Exception ex) {return -1;}
			return size;
		}
		
		private int read(BufferedInputStream i, byte array[]) {
			try {
				return i.read(array, 0, BUFFERSIZE);
			} catch (Exception ex) {
				return -1;
			}
		}
		
		public MysterAddress getRemoteIP() {
			return new MysterAddress(socket.getInetAddress()); //!
		}
		
		private void disconnect() {
			endflag=true;
			cleanUpFromError();
			cleanUp();
		}
		
		private void fireEvent(int id, int c) {
			dispatcher.fireEvent(new ServerDownloadEvent(id, remoteIP, NUMBER,filename, filetype, c, bytessent-initialOffset, filelength));
		}

	
	
		private class Stats implements DownloadInfo {
			public double getTransferRate(){
				try {
					return (bytessent-initialOffset)/((System.currentTimeMillis()-starttime)/1000);
				} catch (Exception ex) {}
				return 0;
			}
			
			
			public long getStartTime() {
				return starttime;
			}
			
			public long getAmountDownloaded() {
				return bytessent;
			}
			
			public long getInititalOffset() {
				return initialOffset;
			}
			
			public String getFileName() {
				return filename;
			}
			
			public String getFileType() {
				return filetype;
			}
			
			public long getFileSize() {
				return filelength;
			}
			
			public MysterAddress getRemoteIP() {
				return FileSenderThread.ServerTransfer.this.getRemoteIP();
			}
			
			public void disconnectClient() {
				FileSenderThread.ServerTransfer.this.disconnect();
			}
			
			public boolean isDone() {
				return FileSenderThread.ServerTransfer.this.endflag;
			}
		}
		
			
		private class DQueuePrivateClass implements QueuedTransfer {
		
			public void refresh(int i) throws IOException {
				FileSenderThread.ServerTransfer.this.refresh(i);
			}
			
			public void disconnect() {
				FileSenderThread.ServerTransfer.this.disconnect();
			}
			
			public void startDownload() {
				FileSenderThread.ServerTransfer.this.startDownload();
			}
			
			public boolean isDone() {
				return FileSenderThread.ServerTransfer.this.endflag;
			}
		}
	}


}