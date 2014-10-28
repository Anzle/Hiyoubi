import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import GivenTools.BencodingException;
import GivenTools.TorrentInfo;

public class RUBTClient {

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("THERE WAS AN ERROR WITH THE INPUTS");
			return;
		}

		String tfile = args[0]; // .torrent file to be loaded
		String sfile = args[1]; // name of the file to save the data to

		// the following is a check to make sure the command line arguments were
		// stored correctly
		System.out.println("tfile: " + tfile);
		System.out.println("sfile: " + sfile);

		File file = new File(tfile);
		long fsize = -1;
		byte[] tbytes = null;
		InputStream fstream;
		
		try
		{
			fstream = new FileInputStream(file);
			fsize = file.length();

			// Initialize the byte array for the file's data
			tbytes = new byte[(int) fsize];

			int point = 0;
			int done = 0;

			// Read from the file
			while (point < tbytes.length
					&& (done = fstream.read(tbytes, point,
							tbytes.length - point)) >= 0)
			{
				point += done;
			}

			fstream.close();
			TorrentInfo torInfo = new TorrentInfo(tbytes);
			System.out.println("Init tracker...");
			Tracker tracker = new Tracker(torInfo);
			
			Peer peer = null;
			
			while(true){
				System.out.println("Getting new peers...");
				ArrayList<Peer> peers = tracker.getPeers();
				
				if(peers == null){
					System.err.println("Error retrieving peers!");
					return;
				}
				
				System.out.println("num peers is " + peers.size());
				
				for(Peer p : peers){
					/*Line for Phase 2*/
					if(p.ip.equals("128.6.171.130") || p.ip.equals("128.6.171.131"))
					{
						if(p.connect()){
							peer = p;
							break;
						}
					}
				}
				if(peer != null){
					break;
				}
				System.out.println("no good peers");
			}
			
			if(peer!= null){
				System.out.println("We have connection to peer: " + peer.ip);
				peer.download(sfile);
			}
			
		} catch (FileNotFoundException e)
		{
			System.err.println(e.getMessage());
			return;
		} catch (IOException e)
		{
			System.err.println(e.getMessage());
			return;
		} catch (BencodingException e) {
			System.err.println(e.getMessage());
			return;
		}

	}

}
