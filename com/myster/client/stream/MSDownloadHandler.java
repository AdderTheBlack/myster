package com.myster.client.stream;

import java.util.Vector;
import java.util.Hashtable;
import java.io.File;

import com.general.util.AnswerDialog;

import com.myster.util.FileProgressWindow;


public class MSDownloadHandler extends MSDownloadListener {
	private FileProgressWindow 		progress;
	private int 					macBarCounter;
	private Vector					freeBars;
	private Hashtable				segmentListeners;
	private File					fileBeingDownloadedTo;
	private ProgressBannerManager 	progressBannerManager;
	
	public MSDownloadHandler(FileProgressWindow progress, File fileBeingDownloadedTo) {
		this.progress 	= progress;
		
		macBarCounter		= 1; // the first bar is used for overall progress
		freeBars			= new Vector();
		segmentListeners 	= new Hashtable();
		this.fileBeingDownloadedTo =	fileBeingDownloadedTo;
		
		this.progressBannerManager = new ProgressBannerManager(progress);
	}
	
	public void startDownload(MultiSourceEvent event) {
		progress.setText("MSDownload is starting...");
		progress.startBlock(0, 0, event.getMultiSourceDownload().getLength());
	}
	
	public void progress(MultiSourceEvent event) {
		progress.setValue(event.getMultiSourceDownload().getProgress());
	}
	
	public void startSegmentDownloader(MSSegmentEvent event) {
		SegmentDownloaderHandler handler = new SegmentDownloaderHandler(progressBannerManager, progress, getAppropriateBarNumber());
	
		segmentListeners.put(event.getSegmentDownloader(), handler);
	
		event.getSegmentDownloader().addListener(handler);
	}
	
	public void endSegmentDownloader(MSSegmentEvent event) {
		SegmentDownloaderHandler handler = (SegmentDownloaderHandler)(segmentListeners.remove(event.getSegmentDownloader()));
		
		if (handler == null) throw new RuntimeException("Could not find a segment downloader to match a segment download that has ended");
	
		returnBarNumber(handler.getBarNumber());
	}
	
	public void endDownload(MultiSourceEvent event) {
		progress.setText("Download Stopped");



	}
	
	public void doneDownload(MultiSourceEvent event) {
		progress.setText("Download Finished");
		progress.done();
		
		final String FILE_ENDING = ".i";
	
		File theFile = fileBeingDownloadedTo; //!
	
		if (! theFile.getName().endsWith(FILE_ENDING)) {
			AnswerDialog.simpleAlert(progress, "Could not rename file \""+theFile.getName()+"\" because it does not end with "+FILE_ENDING+".");
			return;
		}
	
		String path = theFile.getAbsolutePath();
		File someFile = someFile = new File(path.substring(0, path.length()-(FILE_ENDING.length()))); //-2 is for .i
		
		if (someFile.exists()) {
			AnswerDialog.simpleAlert(progress, "Could not rename file from \""+theFile.getName()+"\" to \""+someFile.getName()+"\" because a file by that name already exists.");
			return;
		}
		
		if (!theFile.renameTo(someFile)) {
			AnswerDialog.simpleAlert(progress, "Could not rename file from \""+theFile.getName()+"\" to \""+someFile.getName()+"\" because an unspecified error occured.");
			return;
		}
	
	}
	
	/**
	*	This routine returns the number of the bar that should be used Next.
	*
	*	It also demonstrates the worst about Java syntax and API calls.
	*/
	private int getAppropriateBarNumber() {
		if (freeBars.size() == 0) {
			progress.setProgressBarNumber(macBarCounter+1);
			
			return macBarCounter++;
		}
		
		int minimum = ((Integer)(freeBars.elementAt(0))).intValue(); //no templates
		int min_index = 0;
		for (int i = 0; i< freeBars.size(); i++) {
			int temp_int = ((Integer)(freeBars.elementAt(i))).intValue(); //no autoboxing
			
			if (temp_int<minimum) {
				minimum = temp_int;
				min_index = i;
			}
		}
		
		int temp_int = ((Integer)(freeBars.elementAt(min_index))).intValue();
		
		freeBars.removeElementAt(min_index); //remove element doens't return what it's removing
		
		return temp_int;
	}
	
	/**
	*	returns a "bar" number to the re-distribution heap
	*/
	private void returnBarNumber(int barNumber) {
		freeBars.addElement(new Integer(barNumber));
	}
}

class SegmentDownloaderHandler extends SegmentDownloaderListener {
	final int bar;
	final FileProgressWindow progress;
	
	final ProgressBannerManager progressBannerManager;
	
	public SegmentDownloaderHandler(ProgressBannerManager progressBannerManager, FileProgressWindow progress, int bar) {
		this.bar = bar;
		this.progress = progress;
		
		this.progressBannerManager = progressBannerManager;
	}
	
	public void connected(SegmentDownloaderEvent e) {
		progress.setValue(0, bar);
	}
	
	public void queued(SegmentDownloaderEvent e) {
		progress.setText("You are in queue position "+e.getQueuePosition(), bar);
	}
	
	public void startSegment(SegmentDownloaderEvent e) {	
		progress.startBlock(bar, e.getOffset(), e.getOffset()+e.getLength());
		progress.setPreviouslyDownloaded(e.getOffset(), bar);
		progress.setValue(e.getOffset(), bar);
		progress.setText("Downloading from "+e.getMysterFileStub().getMysterAddress(), bar);
	}
	
	public void downloadedBlock(SegmentDownloaderEvent e) {
		progress.setValue(e.getProgress()+e.getOffset(), bar);
	}
	
	public void endSegment(SegmentDownloaderEvent e) {
		progress.setValue(progress.getMax(bar), bar);
	}
	
	public void endConnection(SegmentDownloaderEvent e) {
		progress.setValue(0, bar);
		progress.setText("This spot is Idle..", bar);
	}
	
	public int getBarNumber() {
		return bar;
	}
	
	
	////////Meta Data Managers
	byte[] image;
	String url;
	public void downloadedMetaData(SegmentMetaDataEvent e)  {
		switch (e.getType()) {
			case 'i':
				flushBanner();
				
				image = e.getCopyOfData();
				
				break;
			case 'u':
				byte[] temp_buffer = e.getCopyOfData();
				
				if (temp_buffer.length > ((int)(0xFFFF))) break;
				
				byte[] final_buffer = new byte[temp_buffer.length + 2];
				
				final_buffer[0] = (byte) ((temp_buffer.length >> 8) & 0xFF);
				final_buffer[1] = (byte) ((temp_buffer.length) & 0xFF);
				
				for (int i = 0; i < temp_buffer.length; i++) {
					final_buffer[i + 2] = temp_buffer[i];
				}
				
				java.io.DataInputStream in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(final_buffer));
				
				try {
					url = in.readUTF();
				} catch (java.io.IOException ex) {
					//nothing
					//means UTF was corrupt
				}
				
				flushBanner();
				break;
			default:
				//do nothing
				break;
		}
	}
	
	private void flushBanner() {
		if (image == null) return;
	
		progressBannerManager.addNewBannerToQueue(new Banner(image, url));
		
		image = null;
		url = null;
	}
}

class ProgressBannerManager implements Runnable {
	public final static int TIME_TO_WAIT = 1000*30;
	
	FileProgressWindow progress;
	
	com.general.util.LinkedList queue;
	RotatingVector oldBanners;
	
	boolean isEndFlag = false;
	boolean isInit;
	
	public ProgressBannerManager (FileProgressWindow progress) {
		this.progress 	= progress;
		this.queue 		= new com.general.util.LinkedList();
		this.oldBanners = new RotatingVector();
	}
	
	public synchronized void addNewBannerToQueue(Banner banner) {
		queue.addToTail(banner);
		
		if (! isInit) {
			isInit = true;
			run();
		}
	}
	
	private Banner getNextBannerInQueue() {
		return (Banner)(queue.removeFromHead());
	}
	
	private void setBanner(Banner banner) {
		if (banner.image != null) progress.makeImage(banner.image); //also sets image
		if (banner.url != null) progress.setURL(banner.url);
	}
	
	public synchronized void run() {
		Banner banner = getNextBannerInQueue();
		
		if (banner != null) {
			oldBanners.addElement(banner);
		} else {
			banner = oldBanners.getNextBanner();
		}
		
		if (banner != null) {
			setBanner(banner);
		}
		
		shedualTimer();
	}
	
	public void shedualTimer() {
		if ((isEndFlag) || (! progress.isVisible())) return;
		
		com.general.util.Timer timer = new com.general.util.Timer(this, TIME_TO_WAIT);
	}
	
	public void end() {
		//isEndFlag = true; //oops.. leave for now...
	}
}

class RotatingVector extends Vector {
	int currentBanner = 0;
	
	public synchronized Banner getNextBanner() {
		if (size()<1) return null;
		
		if (currentBanner >= size()) {
			currentBanner = 0 ;
		}
		
		return (Banner)(elementAt(currentBanner ++)); // xxx++ is very handy
	}
}

class Banner {
	public final byte[] image;
	public final String url;
	
	public Banner(byte[] image, String url) {
		this.image 	= image;
		this.url 	= url;
	}
}