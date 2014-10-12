
public class Message {

	public static byte[] handshake(byte[] peerId, byte[]info_hash){
		//Create the handshake
		//Set the first bits equal to "BitTorrent protocol" as specified by BT protocol
				byte[] handshake = new byte[68];
				handshake[0] = 19;
				handshake[1] = 'B';
				handshake[2] = 'i';
				handshake[3] = 't';
				handshake[4] = 'T';
				handshake[5] = 'o';
				handshake[6] = 'r'; 
				handshake[7] = 'r';
				handshake[8] = 'e';
				handshake[9] = 'n'; 
				handshake[10] = 't';
				handshake[11] = ' ';
				handshake[12] = 'p';
				handshake[13] = 'r';
				handshake[14] = 'o';
				handshake[15] = 't';
				handshake[16] = 'o';
				handshake[17] = 'c';
				handshake[18] = 'o';
				handshake[19] = 'l';    

				//Set the next 8 bytes as '0' byte paddings
				for(int i = 0; i < 8; i++){
					handshake[19 + i + 1] = 0;
				}
				//Set the next bytes equal to the SHA-1 from the torrent file
				for(int i = 0; i < 20; i++){
					handshake[28 + i] = info_hash[i];
				}
				//Set the next bytes equal to the PeerID
				for(int i = 0; i < peerId.length; i++){
					handshake[48 + i] = peerId[i];
				}
				for(int i=0; i<handshake.length;i++)
				System.out.print(handshake[i]);
				System.out.println("");
				return handshake;
	}
	
}
