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
		if (!handshake())
			System.err.println("The handshake with: " + ip + " failed.");
		// if both are good return true
		return true;
	}

	public void download() {
		System.out.println("Starting download with peer " + this.ip);
		byte[] m = Message.interested();
		this.sendMessage(m);
		try {
			while (this.socket.isConnected()) {
				int len = this.from_peer.readInt();
				System.out.println("read init!");
				if (len > 0) {
					byte id = this.from_peer.readByte();
					System.out.println("messageid: " + ((int) (id)));
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
						System.out.println("bitfield recieved");
						System.out.println("building request");
						requestNextBlock();
						break;
					case (byte) 6: // request
						System.out.println("request");
						int rindex = this.from_peer.readInt();
						int rbegin = this.from_peer.readInt();
						int rlength = this.from_peer.readInt();
						// Not required for phase 1, but we need to clear the
						// buffer
						break;
					case (byte) 7: // piece
						System.out.println("loading...");
						int payloadLen = len - 9;
						int bindex = this.from_peer.readInt();
						int boffset = this.from_peer.readInt();
						byte[] data = new byte[payloadLen];
						this.from_peer.readFully(data);
						System.out.println("piece " + bindex + "-" + boffset);
						{
							Piece p = this.pieces.get(bindex);
							if (p != null) {
								p.saveBlock(boffset, data);
								this.currentPieceOffset += boffset;
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
			System.err.println("Client " + this.ip + ":" + this.port + " disconnected. " + e.getMessage());
		}
	}

	private void requestNextBlock() throws IOException {
		Piece piece = this.pieces.get(this.currentPieceIndex);
		if (piece != null && !piece.isFull()) {

			if (this.currentPieceIndex > 0) {
				int blen = calculateBlockSize(this.currentPieceIndex, this.currentPieceOffset);

				byte[] m = Message.blockRequestBuilder(piece.getIndex(), this.currentPieceOffset, blen);
				this.sendMessage(m);
				System.out.println("requested block " + this.currentPieceIndex + "-" + this.currentPieceOffset);
				return;
			}
		}

		if (this.bitfield != null && this.retrieved != null) {
			int num_left = 0;
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

					this.pieces.put(currentPieceIndex,new Piece(currentPieceIndex, pieceLength));
					byte[] m = Message.blockRequestBuilder(this.currentPieceIndex, this.currentPieceOffset, calculateBlockSize(this.currentPieceIndex, this.currentPieceOffset));
					this.sendMessage(m);
					System.out.println("requested block " + this.currentPieceIndex + "-" + this.currentPieceOffset);
					return;
				}
				if (!this.retrieved[i])
					num_left++;
			}
			// pieces are all retrieved or none available from peer

			if (num_left > 0) {
				System.err.println("No more pieces avaialble from current peer");
			}
			System.out.println("Oops");

		}else
			System.out.println("null bitfield?");
	}

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
