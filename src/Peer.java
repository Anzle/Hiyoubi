import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class Peer {

	String id;
	String ip;
	int port;
	
	Socket socket;
	DataInputStream from_peer;
	DataOutputStream to_peer;
	Tracker tracker;
	private boolean peer_choking = false;
	private boolean peer_interested = false;
	private boolean am_choking = false;
	private boolean am_interested = false;
	
	boolean[] bitfield;
	boolean[] retrieved;
	
	ArrayList<Piece> pieces;
	private int currentPieceIndex = -1;
	private int currentPieceOffset = -1;
	
	public Peer(String id, String ip, int port, Tracker tracker) {
		// TODO Auto-generated constructor stub
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.tracker = tracker;
		this.bitfield = new boolean[this.tracker.torrentInfo.piece_hashes.length];
		this.retrieved = new boolean[this.tracker.torrentInfo.piece_hashes.length];
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
		try {
			while(this.socket.isConnected()){
				int len = this.from_peer.readInt();
				if(len > 0){
					byte id = this.from_peer.readByte();
					switch(id){
						case (byte) 0: //choke
							this.peer_choking = true;
							break;
						case (byte) 1: //unchoke
							this.peer_choking = false;
							requestNextPiece();
							break;
						case (byte) 2: //interested
							this.peer_interested = true;
							break;
						case (byte) 3: //not interested
							this.peer_interested = false;
							break;
						case (byte) 4: //have
							int piece = this.from_peer.readInt();
							if(piece >= 0 && piece < bitfield.length)
							bitfield[piece] = true;
							break;
						case (byte) 5: //bit field
							boolean[] payload = new boolean[this.tracker.torrentInfo.piece_hashes.length];
							for(int i = 0; i < payload.length; i++){
								payload[i] = this.from_peer.readBoolean();
							}
							this.bitfield = payload;
							break;
						case (byte) 6: //request
							int rindex = this.from_peer.readInt();
							int rbegin = this.from_peer.readInt();
							int rlength = this.from_peer.readInt();
							//Not required for phase 1, but we need to clear the buffer
							break;
						case (byte) 7: //piece
							int payloadLen = len - 8;
							int bindex = this.from_peer.readInt();
							int boffset = this.from_peer.readInt();
							byte[] data = new byte[payloadLen];
							this.from_peer.readFully(data);
							{
								Piece p = this.pieces.get(bindex);
								if(p != null){
									p.savePiece(boffset, data);
									requestNextPiece();
								}
							}
							break;
						case (byte) 8: //cancel
							int cindex = this.from_peer.readInt();
							int cbegin = this.from_peer.readInt();
							int clength = this.from_peer.readInt();
							//Not required for phase 1, but we need to clear the buffer
							break;
						default:
							break;
					}
				}
			}
			socket.close();
		} catch (IOException e) {
			System.err.println("Client " + this.ip + ":" + this.port + " disconnected. " + e.getMessage());
		}
	}
	
	private void requestNextBlock() {
		
	}
	
	private void requestNextPiece() throws IOException {
		if(this.bitfield != null && this.retrieved != null){
			if(this.bitfield.length != this.retrieved.length){
				System.err.println("Block lengths with peer " + this.ip + ":" + this.port + " differ. Closing connection.");
				this.socket.close();
				return;
			}
			
			for(int i = 0; i < bitfield.length; i++){
				if(bitfield[i] && !this.retrieved[i]){
					//TODO request piece i
					return;
				}
			}
			//no blocks left;
		}
	}

	/** 
	 * Perform the Handshake with the Peer
	 * The method creates the handshake to be sent, then
	 * Receives a response from the peer, validates it and returns. 
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
