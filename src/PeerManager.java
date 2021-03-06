import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;


/**
 * PeerManager is a class designed to connect to Peers.
 * 	It runs two Phases:
 * 		 one that listens for new incoming connections to upload to
 * 		 one that checks for new available connections to download from
 * 
 * 	
 * */
public class PeerManager {

	ServerSocket server;
	int port;
	Peer aPeer;
	ArrayList<Peer> peerList;
	Tracker tracker;
	Thread serverCheck;
	Thread peerCheck;
	boolean downloading;
	/**The wait interval: set to 2 minutes*/
	final int INTERVAL = 10000;//120000;
	String flag;
	TorrentHandler th;
	
	
	public PeerManager(int portNumber, Tracker tracker, String flag){
		port = portNumber;
		this.tracker = tracker;
		this.flag = flag;
		downloading = false;
		peerList = new ArrayList<Peer>(3);
		th = this.tracker.getTorrentHandler();
		th.setPeerManager(this);
		
		peerCheck = new Thread(new PeerListener());
		peerCheck.start();
		
		try {
				server = new ServerSocket(port);
				serverCheck = new Thread(new ServerListener());
				serverCheck.start();
			} catch (IOException e) {
				System.err.println("PeerManager's server has run into an issue and failed to initilize");
			}

			
		
	}
	
	/**@return whether there are any peers downloading*/
	public boolean getDownloading(){return downloading;}
	
	/**@return port number*/
	public int getPort(){return port;}
	
	/**
	 * @param flag 
	 * @param the file to be saved to
	 * */
	public void download() {
		if(peerList.isEmpty()){
			//System.err.println("There are no peers to download from.");
			downloading = false;
		}
		else{
			//Extend
			for( Peer p: peerList){
				System.out.println("Downloading from: " + p.ip);
				p.download();
			}
			
			/*{
				Peer p = peerList.get(0);
				System.out.println("Downloading from: " + p.ip);
				downloading = true;
				p.start();
			}*/
			
			downloading = true;
		}
	}
	
	public void disconnect(){
		for( Peer p: peerList){
			p.disconnect();
			System.out.println("Disconnected from peer:" + p.ip);
		}
	}
	
	/***
	 * 
	 * Private classes that act as threads
	 *
	 */
	
	
	private class PeerListener implements Runnable{
		
		//This constructor calls super... just like an implicit one... so it does nothing
		public PeerListener(){super();}
		
		/**
		 * Check the tracker for peers that we can download the file from
		 * 	if a peer is already connected to us, skip that peer */
		public void run(){
			//Loop Forever
			while(true){
				System.out.println("Checking for new peers.");
				ArrayList<Peer> peers = tracker.getPeers();
				
				if(peers == null){
					System.err.println("There are no new peers.");
					try {
						Thread.sleep(INTERVAL);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;
				}
				
				//System.out.println("num peers is " + peers.size());
				
				for(Peer p : peers){
					//check that we don't add a peer who has been added already
					if(peerList.contains(p)){
						//System.out.println("Peer List contains:" + p);
						continue;
					}
					else if(p.ip.equals(flag)){
						if(p.connect()){
							add(p);
							System.out.println("Downloading Connection from: " + p.ip);
							p.download();
							
						}
					}
					//for Phase 2, we only connect to these peers
					else if(p.ip.equals("128.6.171.130") || p.ip.equals("128.6.171.131")){
						if(p.connect()){
							System.out.println("Downloading Connection from: " + p.ip);
							add(p); //This is a synchronized method
							p.download();
						}
						//else
							//System.out.println("Could not connect to: "+p.ip);
					}
				}
				
				try {
					Thread.sleep(INTERVAL);
				} catch (InterruptedException e) {
					System.err.println("peerListener: Sleep was interupted..."+
							" shouldn't actually matter that this happened");
				}
			}
		}
		
		
		/*To prevent memory leakage*/
		private synchronized void add(Peer p){
			//if(p instanceof Peer)
				peerList.add((Peer)p);
				p.start();
				System.out.println("Established connection to peer: " + p.ip);
		}
		
		private synchronized boolean contains(Object p){
			if(p instanceof Peer)
				return peerList.contains((Peer)p);
			return false;
		}
	}

	
	private class ServerListener implements Runnable{
		
		public ServerListener(){super();}
		
		/**
		 * This method constantly checks if there is a peer
		 * 	attempting to connect to the server
		 */
		public void run(){
			while(true){
				try {

					System.out.println("Checking for inbound Connections");
					aPeer = new Peer(server.accept(), th, tracker.getPeerId());

					//aPeer.new connect for incoming connects
					if(peerList.contains(aPeer))
						continue;
					if(aPeer.connect('i')){
						System.out.println("Uploading Connection made with:" + aPeer.ip);
						add(aPeer);
						//aPeer.start();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		/*To prevent memory leakage*/
		private synchronized void add(Object p){
			if(p instanceof Peer)
				peerList.add((Peer)p);
		}
	}


	public void notifyPeersPieceCompleted(int index) {
		byte[] m = Message.haveBuilder(index);
		for(Peer p : this.peerList){
			p.sendMessage(m);
		}
	}	
}