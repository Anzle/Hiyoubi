import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;


public class Peer {

	String id;
	String ip;
	int port;
	
	Socket socket;
	DataInputStream from_peer;
	DataOutputStream to_peer;
	Tracker tracker;
	
	public Peer(String id, String ip, int port, Tracker tracker) {
		// TODO Auto-generated constructor stub
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.tracker = tracker;
	}

	public boolean connect(){
		//connect the connection to the peer
		try {
			socket = new Socket(ip, port);
			from_peer = new DataInputStream(socket.getInputStream());
			to_peer = new DataOutputStream(socket.getOutputStream());
			
			if(socket == null || from_peer==null||to_peer == null)
				System.err.println("Unable to peoperly set up socket. AKA: Your host is gone");
		} catch (UnknownHostException e) {
			System.err.println(e.getMessage() + "AKA: Your host is gone");
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
			return false;
		}
		//handshake with the peer
	
			try {
				handshake();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		
		//if both are good return true
		return true;
	}

	public void download() {
		
		//download the file
		
	}
	
	public boolean handshake() throws Throwable{
		byte[] shake = Message.handshake(tracker.getPeerId(), tracker.getInfoHash());
		byte[] responce = new byte[68];
		if(sendMessage(shake))
			from_peer.readFully(responce);
		else
			return false;
		
		if(responce == null)
			return false;
		
		return true;
	}
	
	/**
	 * Sends a magical Columbidae to deliver our message to the connected Peer
	 * If the bird is shot out of flight, print to the error stream
	 * 
	 * @param message
	 *            The message to be sent via pigeon
	 */
	public boolean sendMessage(byte[] message) {
		try {
			to_peer.write(message);
			to_peer.flush();
		} catch (IOException e) {
			System.err.println("Error sending message: " + message.toString() +
					"/nto peer located at: " + ip);
			return false;
		}
		return true;
	}
	
	public byte[] recieveMessage(){
		byte[] responce = new byte[68];
		try {
			this.from_peer.readFully(responce);
		} catch (IOException e) {
			System.err.println("Error reading messagefrom peer located at: " + ip);
			return null;
		}
		return responce;
		
	}
	
	
	
}
