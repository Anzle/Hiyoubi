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
			
		} catch (UnknownHostException e) {
			System.err.println(e.getMessage() + "AKA: Your host is gone");
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
			return false;
		}
		//handshake with the peer
		if(!handshake())
			System.err.println("The handshake with: " + ip + " failed.");
		//if both are good return true
		return true;
	}

	public void download() {
		
		//download the file
		
	}
	
	/** 
	 * Perform the Handshake with the Peer
	 * The method creates the handshake to be sent, then
	 * recieves a responce from the peer, validates it and returns. 
	 * */
	public boolean handshake(){
		byte[] shake = Message.handshake(tracker.getPeerId(), tracker.getInfoHash());
		byte[] responce;
		if(sendMessage(shake))
			responce = recieveMessage();
		else
			return false;
		
		if(responce == null)
			return false;

		return Message.validateHandshake(responce, shake, id.getBytes());
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
			//http://docs.oracle.com/javase/tutorial/essential/io/datastreams.html
			//We will do nothing with this! The EOF is how it knows to stop
		}
		return responce;
		
	}
	
	
	
}
