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

	byte[] handshake_header;

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
		
		//if both are good return true
		return true;
	}

	public void download() {
		
		//download the file
		
	}
}
