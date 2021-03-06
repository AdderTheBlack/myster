/*
 * 
 * Title: Myster Open Source Author: Andrew Trumper Description: Generic Myster
 * Code
 * 
 * This code is under GPL
 * 
 * Copyright Andrew Trumper 2000-2001
 */

package com.myster.tracker;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Vector;

import com.general.util.BlockingQueue;
import com.general.util.RInt;
import com.myster.client.datagram.PingEvent;
import com.myster.client.datagram.PingEventListener;
import com.myster.client.stream.StandardSuite;
import com.myster.net.MysterAddress;
import com.myster.search.IPQueue;
import com.myster.type.MysterType;
import com.myster.type.TypeDescription;
import com.myster.type.TypeDescriptionList;
import com.myster.util.MysterThread;

/**
 * This class is the interface to Myster's tracker. Every single interaction
 * with the tracker module inside Myster currently goes through here. The
 * tracker is the part of Myster that keeps track of the list of servers that
 * Myster knows about. Basically it maintains the list of the top XXX number of
 * servers on the network for a given file type. All the servers kept by the
 * tracker have associated misc. statistics about themselves kept. These
 * statistics are kept current n a best effort basis. These statistics are used
 * to generate a "rank". This "rank" determines if the server is to be kept
 * about in memory on one of the server lists.
 * <p>
 * To access this object from Myster code use the singleton :
 * com.myster.tracker.IPListManagerSingleton
 * 
 * @see com.myster.tracker.IPListManagerSingleton
 * 
 * @author Andrew Trumper
 */
public class IPListManager { //aka tracker
    public static final int LISTSIZE = 100; //Size of any given list..

    private static final String[] lastresort = { "bigmacs.homeip.net", "mysternetworks.homeip.net",
            "mysternetworks.dyndns.org", "myster.homeip.net" };

    private IPList[] list;

    private TypeDescription[] tdlist;

    private BlockingQueue blockingQueue = new BlockingQueue();

    private AddIP[] adderWorkers = new AddIP[2];

    IPListManager() {
        blockingQueue.setRejectDuplicates(true);

        tdlist = TypeDescriptionList.getDefault().getEnabledTypes();

        list = new IPList[tdlist.length];
        for (int i = 0; i < list.length; i++) {
            assertIndex(i); //loads all lists.
        }

        for (int i = 0; i < adderWorkers.length; i++) {
            adderWorkers[i] = new AddIP();
            adderWorkers[i].start();
        }

        (new IPWalker()).start();
    }

    private Callback pingEventListener = new Callback();

    /**
     * This routine is used to suggest an ip for the tracker to add to its
     * server lists. The suggested ip will not show up on the tracker's lists
     * until it has had its statistics queried. This can take a while. THIS
     * ROUTINE IS NONE BLOCKING so the caller doens't have to worry about a
     * lengthy delay while the server is queried for its statistics.
     * 
     * @param ip
     *            The MysterAddress of the server you want to add.
     */
    public void addIP(MysterAddress ip) {
        try {
            com.myster.client.datagram.UDPPingClient.ping(ip, pingEventListener); //temporary..
            // should be
            // inside tracker...
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private class Callback extends PingEventListener {
        public void pingReply(PingEvent e) {
            if (e.isTimeout())
                return; //dead ip.
            MysterAddress ip = e.getAddress();
            MysterIPPool pool = MysterIPPool.getInstance();
            MysterServer mysterServer;

            mysterServer = pool.getMysterIPLevelOne(ip);

            //Error conditions first.
            if (mysterServer != null) {
                addIPBlocking(mysterServer); //if not truly new then don't
                // make a new thread.
            } else if (blockingQueue.length() > 100) {
                System.out.println("->   !!!!!!!!!!!!!!!!!!!!!!AddIP queue is at Max length.");
            } else {
                blockingQueue.add(ip); //if it's truly new then make a new
                // thread to passively add the ip
            }
        }
    }

    /**
     * Calls getTop(type, 10).
     * 
     * @param type
     * @return
     */
    public synchronized MysterServer[] getTopTen(MysterType type) {
        return getTop(type, 10);
    }

    /**
     * Returns a list of Myster servers ordered by rank. Returns only server
     * currently thought to be available (up). If there are not enough UP
     * Servers or whatever, the rest of the array is filled with null!
     * 
     * @param type
     *            to return servers for
     * @param x
     *            number of servers to try and return
     * @return an array of MysterServer objects ordered by rank and possibly
     *         containing nulls.
     */
    public synchronized MysterServer[] getTop(MysterType type, int x) {
        IPList iplist;
        iplist = getListFromType(type);
        if (iplist == null)
            return null;
        return iplist.getTop(x);

    }

    /**
     * Asks the cache if it knows of this MysterServer and gets stats if it does
     * else returns null. Does not do any io. Returns quickly.
     * 
     * @param address
     * @return Myster server at that address or null if the tracker doesn't have
     *         any record of a server at that address
     */
    public synchronized MysterServer getQuickServerStats(MysterAddress address) { //returns
        return MysterIPPool.getInstance().getMysterIPLevelOne(address);
    }

    /**
     * Gets MysterServer from cache if it is available else creates in with an
     * IO operation else throws IOException is server is down.
     * <p>
     * This routine does an io operation if the server is not found in the cache -
     * that is it tries to connect to the specified address and download the
     * stats if the server stats aren't already known.
     * 
     * @param address
     * @return The MysterServer object for this address.
     * @throws IOException
     */
    public synchronized MysterServer getServerStats(MysterAddress address) throws IOException { //might
        // block
        // for
        // a
        // long
        // time.
        return MysterIPPool.getInstance().getMysterServer(address);
    }

    /**
     * Returns vector of ALL MysterAddress object in order of rank for that
     * type.
     * 
     * @param type
     * @return Vector of MysterAddresses in the order of rank.
     */
    public synchronized Vector getAll(MysterType type) {
        IPList iplist;
        iplist = getListFromType(type);
        if (iplist == null)
            return null;
        return iplist.getAll();
    }

    /**
     * Returns an array of string objects representing a set of servers that
     * could be available for bootstrapping onto the Myster network.
     * 
     * @return an array of string objects representing internet addresses
     *         (ip:port or domain name:port format)
     */
    public String[] getOnRamps() {
        String[] temp = new String[lastresort.length];
        System.arraycopy(lastresort, 0, temp, 0, lastresort.length);
        return temp;
    }

    private synchronized IPList getListFromIndex(int index) {
        assertIndex(index);
        return list[index];
    }

    /**
     * This routine is here so that the ADDIP Thread can add an com.myster to
     * all lists and the ADDIP Function can add an ip assuming that the IP
     * exists already.
     * 
     * @param ip
     *            to add
     */

    private void addIPBlocking(MysterServer ip) {
        for (int i = 0; i < tdlist.length; i++) {
            assertIndex(i);
            list[i].addIP(ip);
        }
    }

    /**
     * This function looks returns a IPList for the type passed if such a list
     * exists. If no such list exists it returns null.
     * 
     * @param type
     *            of list to fetch
     * @return the IPList for the type or null if no list exists for that typ.
     */

    private IPList getListFromType(MysterType type) {
        int index;
        index = getIndex(type);

        if (index == -1)
            return null;

        assertIndex(index); //to make sure the list if loaded.

        if (list[index].getType().equals(type))
            return list[index];

        return null;
    }

    /**
     * For dynamic loading Note.. this dynamic loading is thread safe!
     */
    private synchronized void assertIndex(int index) {
        if (list[index] == null) {
            list[index] = createNewList(index);
            System.out.println("Loaded List " + list[index].getType());
        }
    }

    /**
     * Returns the index in the list of IPLists for the type passed.
     * 
     * @param type
     * @return the index in the list array for this type or -1 if there is not
     *         list for this type.
     */
    private synchronized int getIndex(MysterType type) {
        for (int i = 0; i < tdlist.length; i++) {
            if (tdlist[i].getType().equals(type))
                return i;
        }
        return -1;
    }

    /**
     * Returns an IPList for the type in the tdlist variable for that index.
     * This is a stupid routine.
     * 
     * @param index
     * @return
     */
    private synchronized IPList createNewList(int index) {
        return (new IPList(tdlist[index].getType()));
    }

    /**
     * What follows is basically the tracker as stated in the DOCS . Currently
     * Myster polls the i-net.. it should only crawl when something is
     * triggered... Like when a search is done.. ideally, the IPs discovered
     * during a crawl should be fed back to the "tracker" portion. The downside
     * is servers do no crawling.. (bad)
     * 
     * @author Andrew Trumper
     */

    private class IPWalker extends MysterThread {

        /*
         * Protocol for handshake is Send 101, his # of file, his speed.
         */
        public IPWalker() {
            super("IPWalker thread");
        }

        public void run() {

            System.out.println("Starting walker thread");
            setPriority(Thread.MIN_PRIORITY); //slightly better than a daemon
            // thread.
            RInt rcounter = new RInt(tdlist.length - 1);
            try {
                sleep(10 * 1000);
            } catch (InterruptedException ex) {
            }
            MysterServer[] iplist = getListFromIndex(rcounter.getVal()).getTop(10);
            try {
                sleep(10 * 60 * 1000);
            } catch (InterruptedException ex) {
            } //wait 10 minutes for the list to calm down
            //if this trick is omitted, the list spends ages sorting through a
            // load of ips that aren't up.
            while (true) {
                System.out.println("CRAWLER THREAD: Starting new automatic crawl for new IPS");
                iplist = getListFromIndex(rcounter.getVal()).getTop(10);
                IPQueue ipqueue = new IPQueue();

                int max = 0;

                String[] onramps = getOnRamps();
                for (int i = 0; i < onramps.length; i++) {
                    try {
                        ipqueue.addIP(new MysterAddress(onramps[i]));
                    } catch (UnknownHostException ex) {
                        System.out.println("One of the array of threw an error : " + onramps[i]);
                    }
                    max++;
                }

                for (int i = 0; i < iplist.length && iplist[i] != null; i++) {
                    ipqueue.addIP(iplist[i].getAddress());
                    max++;
                }

                int i = 0;
                for (MysterAddress ip = ipqueue.getNextIP(); ip != null; ip = ipqueue.getNextIP()) {
                    try {
                        if (i <= max + 50)
                            addIPs(ip, ipqueue, getListFromIndex(rcounter.getVal()).getType());
                    } catch (IOException ex) {
                        //nothing.
                    }
                    if (i >= max)
                        addIP(ip); //..
                    i++;
                }

                System.out.println("CRAWLER THREAD: Going to sleep for a while.. Good night. Zzz.");
                try {
                    sleep(30 * 1000 * 60);
                } catch (InterruptedException ex) {
                } //300 000ms = 5 mins.
                rcounter.inc();
            }
        }

        private void addIPs(MysterAddress ip, IPQueue ipQueue, MysterType type) throws IOException {
            Vector ipList = StandardSuite.getTopServers(ip, type);

            for (int i = 0; i < ipList.size(); i++) {
                try {
                    ipQueue.addIP(new MysterAddress((String) (ipList.elementAt(i))));
                } catch (UnknownHostException ex) {
                }
            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.myster.util.MysterThread#end()
         */
        public void end() {
            throw new RuntimeException("Not implemented.");
        }

    }

    private static volatile int counter = 0;

    /**
     * This is a thread object representing the thread(s) that do the io
     * required to add IPAddresses to the tracker in a non-blocking manner.
     * 
     * @author Andrew Trumper
     */
    private class AddIP extends MysterThread {

        public AddIP() {
            super("AddIP Thread");
        }

        public void run() {
            MysterAddress ip = null;
            for (;;) {
                try {
                    ip = (MysterAddress) (blockingQueue.get());//BlockingQueue

                    counter++;
                    System.out.println("Number of servers still queued : -> "
                            + blockingQueue.length());

                    MysterServer mysterserver = null;
                    try {
                        mysterserver = MysterIPPool.getInstance().getMysterServer(ip);
                        if (mysterserver == null)
                            continue;
                    } catch (IOException ex) {
                        continue;
                    }

                    addIPBlocking(mysterserver);

                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {

                    counter--;
                }
            }
            //Statement not reached (until quiting)
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.myster.util.MysterThread#end()
         */
        public void end() {
            throw new RuntimeException("Not implemented.");
        }
    }
}

