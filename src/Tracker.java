import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import GivenTools.Bencoder2;
import GivenTools.BencodingException;
import GivenTools.TorrentInfo;

public class Tracker {

	TorrentInfo torrentInfo;
	private final char[] HEXCHARS = "0123456789ABCDEF".toCharArray();

	public Tracker(TorrentInfo torrentInfo) {
		this.torrentInfo = torrentInfo;
	}

	private String queryServer() throws IOException {

		String ih_str = "";
		byte[] info_hash = this.torrentInfo.info_hash.array();

		for (int i = 0; i < info_hash.length; i++) {
			if ((info_hash[i] & 0x80) == 0x80) { // if the byte data has the
													// most
													// significant byte set
													// (e.g. it
													// is negative)
				ih_str += "%" + this.HEXCHARS[(info_hash[i] & 0xF0) >>> 4]
						+ this.HEXCHARS[info_hash[i] & 0x0F];
			} else {
				try { // If the byte is a valid ascii character, use URLEncoder
					ih_str += URLEncoder.encode(new String(
							new byte[] { info_hash[i] }), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					System.out.println("URL formation error:" + e.getMessage());
				}
			}
		}

		// String query = "announce?info_hash=" + ih_str + "&peer_id=" +
		// host.getPeerID() + "&port=" + host.getPort() + "&left=" +
		// torinfo.file_length + "&uploaded=0&downloaded=0";

		String query = "";

		URL urlobj;

		byte[] tracker_response = null;

		urlobj = new URL(this.torrentInfo.announce_url, query);
		HttpURLConnection uconnect = (HttpURLConnection) urlobj
				.openConnection();
		uconnect.setRequestMethod("GET");

		BufferedReader in = new BufferedReader(new InputStreamReader(
				uconnect.getInputStream()));

		StringBuffer response = new StringBuffer();

		String inline = "";
		while ((inline = in.readLine()) != null) {

			tracker_response = inline.getBytes();

			// System.out.println(inline);// prints stuff
			response.append(inline);

		}

		return response.toString();
	}

	public ArrayList<Peer> getPeers() {
		String response = "";
		try {
			response = queryServer();
		} catch (IOException e) {
			System.err.print(e.getMessage());
			return null;
		}
		HashMap results = null;
		try {
			Object o = Bencoder2.decode(response.getBytes());

			if (o instanceof HashMap) {
				results = (HashMap) o;
			}
		} catch (BencodingException e) {
			System.err.print(e.getMessage());
			return null;
		}

		if (results == null)
			return null;

		ByteBuffer b = ByteBuffer.wrap("peers".getBytes());

		ByteBuffer peer_ip_key = ByteBuffer.wrap("ip".getBytes());
		ByteBuffer peer_port_key = ByteBuffer.wrap("port".getBytes());
		ByteBuffer peer_id_key = ByteBuffer.wrap("id".getBytes());

		Object peer_list_o = results.get(b);

		if (!(peer_list_o instanceof ArrayList)) {
			return null;
		}

		ArrayList peer_list = (ArrayList) peer_list_o;

		for (Object peer_info_o : peer_list[1]) {

			if (!(peer_info_o instanceof HashMap)) {
				continue;
			}

			HashMap peer_info = (HashMap) peer_info_o;

			Object port_o = peer_info.get(peer_port_key);
			Object ip_o = peer_info.get(peer_port_key);
			Object id_o = peer_info.get(peer_port_key);

		}

		return null;

	}
}
