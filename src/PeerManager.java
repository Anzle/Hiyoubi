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
	/**The wait interval: set to 2 minutes*/
	final int INTERVAL = 120000;
	
	
	public PeerManager(int portNumber){
		port = portNumber;
		peerList = new ArrayList<Peer>(3);
			try {server = new ServerSocket(port);
			} catch (IOException e) {
				System.err.println("PeerManager's server has run into an issue and failed to initilize");
			}
			serverCheck = new Thread(new ServerListener());
			serverCheck.start();
			peerCheck = new Thread(new PeerListener());
			peerCheck.start();
			
			
		
	}
	
	/**@return port number*/
	public int getPort(){return port;}
	//TODO Add in another thread to run. 
	private class PeerListener implements Runnable{
		
		//This constructer calls super... just like an implicit one... so it does nothing
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
					//make this a thread, make it sleep?
					return;
				}
				
				//System.out.println("num peers is " + peers.size());
				
				for(Peer p : peers){
					//check that we don't add a peer who has been added already
					if(peerList.contains(p))
						continue;
					
					//for Phase 2, we only connect to these peers
					if(p.ip.equals("128.6.171.130") || p.ip.equals("128.6.171.131")){
						if(p.connect()){
							add(p); //This is a synchronized method
							System.out.println("Established connection to peer: " + p.ip);
							//p.run(); ->begins the downloading process?
						}
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
		private synchronized void add(Object p){
			if(p instanceof Peer)
				peerList.add((Peer)p);
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
					aPeer = new Peer(server.accept(), tracker, tracker.getPeerId());
					//aPeer.new connect for incoming connects
					if(peerList.contains(aPeer))
						continue;
					if(aPeer.connect('i'))
						add(aPeer);
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
	
}