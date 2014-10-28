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
	
	public PeerManager(int portNumber){
		port = portNumber;
		peerList = new ArrayList<Peer>(3);
			try {server = new ServerSocket(port);
			} catch (IOException e) {
				System.err.println("PeerManager's server has run into an issue and failed to initilize");
			}
			serverCheck = new Thread(new ServerListener());
			serverCheck.start();
			
		
	}
	//TODO Add in another thread to run. 
	
	/**
	 * Check the tracker for peers that we can download the file from
	 * 	if a peer is already connected to us, skip that peer */
	public void downloadFromPeers(){
		//System.out.println("Getting new peers...");
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
					peerList.add(p);
					System.out.println("We have connection to peer: " + p.ip);
					//p.run(); ->begins the downloading process?
				}
			}
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
					aPeer = new Peer(server.accept(), tracker);
					//aPeer.new connect for incoming connects
					if(peerList.contains(aPeer))
						continue;
					if(aPeer.connect('i'))
						peerList.add(aPeer);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
}