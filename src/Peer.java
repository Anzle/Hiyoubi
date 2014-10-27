import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class Peer {

	private static final int MAXLENGTH = 16384;
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

	HashMap<Integer,Piece> pieces;
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
		this.pieces = new HashMap<Integer,Piece>();
	}

	public boolean connect() {
		// connect the connection to the peer
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
		// handshake with the peer
		if (!handshake()){
			System.err.println("The handshake with: " + ip + " failed.");
			return false;
		}
		// if both are good return true
		return true;
	}

	public void download() {
		System.out.println("Starting download with peer " + this.ip + "send intrested");
		System.out.println(this.tracker.torrentInfo.piece_hashes.length);
		byte[] m = Message.interested();
		this.sendMessage(m);
		try {
			while (this.socket.isConnected()) {
				int len = this.from_peer.readInt();
				//System.out.println("read init!");
				if (len > 0) {
					byte id = this.from_peer.readByte();
					//System.out.println("messageid: " + ((int) (id)));
					switch (id) {
					case (byte) 0: // choke
						this.peer_choking = true;
						System.out.println("choked");
						break;
					case (byte) 1: // unchoke
						this.peer_choking = false;
						System.out.println("un choked");
						requestNextBlock();
						break;
					case (byte) 2: // interested
						this.peer_interested = true;
						System.out.println("interested");
						break;
					case (byte) 3: // not interested
						this.peer_interested = false;
						System.out.println("not interested");
						break;
					case (byte) 4: // have
						int piece = this.from_peer.readInt();
						System.out.println("have " + piece);
						if (piece >= 0 && piece < bitfield.length)
							bitfield[piece] = true;
						break;
					case (byte) 5: // bit field
						byte[] payload = new byte[len - 1];
						this.from_peer.readFully(payload);
						boolean[] bitfield = new boolean[payload.length * 8];
						int boolIndex = 0;
						for (int byteIndex = 0; byteIndex < payload.length; ++byteIndex) {
							for (int bitIndex = 7; bitIndex >= 0; --bitIndex) {
								if (boolIndex >= payload.length * 8) {
									// Bad to return within a loop, but it's the easiest way
									continue;
								}

								bitfield[boolIndex++] = (payload[byteIndex] >> bitIndex & 0x01) == 1 ? true: false;
							}
						}
						this.bitfield = bitfield;
						//System.out.println("bitfield recieved");
						//System.out.println("building request");
						this.requestNextBlock();
						break;
					case (byte) 6: // request
						//System.out.println("request");
						int rindex = this.from_peer.readInt();
						int rbegin = this.from_peer.readInt();
						int rlength = this.from_peer.readInt();
						// Not required for phase 1, but we need to clear the
						// buffer
						break;
					case (byte) 7: // piece
						int payloadLen = len - 9;
						//System.out.println("loading ("+payloadLen+")...");
						int bindex = this.from_peer.readInt();
						int boffset = this.from_peer.readInt();
						byte[] data = new byte[payloadLen];
						for(int i = 0; i < payloadLen; i++){
							//System.out.println(i);
							data[i] = this.from_peer.readByte();
						}
						System.out.println("piece " + bindex + "-" + boffset);
						{
							Piece p = this.getPiece(bindex);
							if (p != null) {
								//System.out.println("Saving...");
								p.saveBlock(boffset, data);
								this.currentPieceOffset += payloadLen;
								
								this.requestNextBlock();
							}else{
								System.err.println("Piece not found!");
							}
						}
						break;
					case (byte) 8: // cancel
						System.out.println("cancel");
						int cindex = this.from_peer.readInt();
						int cbegin = this.from_peer.readInt();
						int clength = this.from_peer.readInt();
						// Not required for phase 1, but we need to clear the
						// buffer
						break;
					default:
						System.out.println("message invalid");
						break;
					}
				}else{
					if(len == 0)
						System.out.println("keep alive");
					else
						System.out.println("message length: " + len);
				}
			}
			socket.close();
		} catch (IOException e) {
			System.err.println("Peer " + this.ip + ":" + this.port + " disconnected. " + e.getMessage());
		}
	}
	
	private Piece getPiece(int index){
		Piece p = this.pieces.get(index);
		if(p == null){
			int pieceLength = this.tracker.torrentInfo.piece_length;
			if (this.currentPieceIndex == this.tracker.torrentInfo.piece_hashes.length - 1) {
				pieceLength = this.tracker.torrentInfo.file_length % this.tracker.torrentInfo.piece_length;
				if (pieceLength == 0) {
					pieceLength = this.tracker.torrentInfo.piece_length;
				}
			}
			p = new Piece(index, pieceLength);
			this.pieces.put(index,p);
		}
		return p;
	}

	private void requestNextBlock() throws IOException {
		Piece piece = this.getPiece(this.currentPieceIndex);
		if(piece != null && piece.isFull()){
			if(piece.isValid(this.tracker.torrentInfo.piece_hashes[piece.getIndex()].array()))
				this.retrieved[piece.getIndex()] = true;
			else{
				System.out.println("hash failed");
				return;
			}
		}else if (piece != null && this.currentPieceIndex >= 0) {
			int blen = calculateBlockSize(this.currentPieceIndex, this.currentPieceOffset);
			if(blen > 0){
				byte[] m = Message.blockRequestBuilder(piece.getIndex(), this.currentPieceOffset, blen);
				this.sendMessage(m);
				System.out.println("requested block " + this.currentPieceIndex + "-" + this.currentPieceOffset);
				return;
			}
		}

		if (this.bitfield != null && this.retrieved != null) {
			for (int i = 0; i < bitfield.length; i++) {
				if (bitfield[i] && !this.retrieved[i]) {
					this.currentPieceIndex = i;
					this.currentPieceOffset = 0;

					int pieceLength = this.tracker.torrentInfo.piece_length;
					if (this.currentPieceIndex == this.tracker.torrentInfo.piece_hashes.length - 1) {
						pieceLength = this.tracker.torrentInfo.file_length % this.tracker.torrentInfo.piece_length;
						if (pieceLength == 0) {
							pieceLength = this.tracker.torrentInfo.piece_length;
						}
					}
					System.out.println(pieceLength);
					if(this.getPiece(this.currentPieceIndex) == null)
						this.pieces.put(currentPieceIndex,new Piece(currentPieceIndex, pieceLength));
					byte[] m = Message.blockRequestBuilder(this.currentPieceIndex, this.currentPieceOffset, calculateBlockSize(this.currentPieceIndex, this.currentPieceOffset));
					this.sendMessage(m);
					System.out.println("requested block " + this.currentPieceIndex + "-" + this.currentPieceOffset);
//------------------------There was a return here, I removed it
					return;
				}
			}
			
			

		}else
			System.out.println("null bitfield?");
	}

	/**
	 * Calculate the size of the block length we desire by
	 * Checking if this is the last piece
	 * 	then if the size must be shortened from the norm (less than a normal block size left)
	 * 
	 * @return MAXLENGTH if block size is normal
	 * @return blocklength < MAXLENGTH if this is the last block and if it is small
	 * */
	private int calculateBlockSize(int index, int offset) {
		int pieceLength = this.tracker.torrentInfo.piece_length;
		if (index == this.tracker.torrentInfo.piece_hashes.length - 1) {
			pieceLength = this.tracker.torrentInfo.file_length % this.tracker.torrentInfo.piece_length;
			if (pieceLength == 0) {
				pieceLength = this.tracker.torrentInfo.piece_length;
			}
		}

		int blockLength = Peer.MAXLENGTH;
		if (pieceLength - offset < blockLength)
			blockLength = pieceLength - offset;
		return blockLength;
	}

	/**
	 * Perform the Handshake with the Peer The method creates the handshake to
	 * be sent, then Receives a response from the peer, validates it and
	 * returns.
	 * */
	public boolean handshake() {
		byte[] shake = Message.handshake(tracker.getPeerId(), tracker.getInfoHash());
		byte[] responce;
		if (sendMessage(shake))
			responce = recieveMessage();
		else
			return false;

		if (responce == null)
			return false;

		return Message.validateHandshake(responce, shake, id.getBytes());
	}

	/**
	 * Sends a magical Columbidae(a bird) to deliver our message to the connected Peer
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
	/**
	 * Receives a magical Columbidae(a pigeon) who brings messages from the connected Peer
	 * If the bird has a blank message... we don't really care. 
	 * 
	 * @return the complete message brought by the pigeon
	 */
	public byte[] recieveMessage() {
		byte[] responce = new byte[68];
		try {
			this.from_peer.readFully(responce);
		} catch (IOException e) {
			// http://docs.oracle.com/javase/tutorial/essential/io/datastreams.html
			// We will do nothing with this! The EOF is how it knows to stop
		}
		return responce;

	}

}
