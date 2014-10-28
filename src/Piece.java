import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Piece {
	
	private int index;
	private byte[] data;
	private int datawritten = 0;
	
	public Piece(int index, int length){
		this.index = index;
		data = new byte[length];
	}
	
	public void saveBlock(int offset, byte[] data){
		if(data == null)
			return;
		
		for(int i = 0; i < data.length && offset + i < this.data.length; i++){
			this.data[offset + i] = data[i];
		}
		
		this.datawritten += data.length;
	}
	
	public int getIndex(){
		return this.index;
	}
	
	public byte[] getData(){
		return this.data;
	}
	
	public boolean isFull(){
		//System.out.println(this.data.length + " == " + this.datawritten);
		return this.data.length == this.datawritten ;
	}
	
	public boolean isValid(byte[] hash){
		try {
			MessageDigest hasher = MessageDigest.getInstance("SHA");
			byte[] result = hasher.digest(this.data);
			hasher.reset();
			if(result.length != hash.length){
				System.err.println("Hash check for piece " + this.index + " failed with hash length mismatch.");
				return false;
			}
			//System.out.println("hash-"+ (new String(hash)));
			//System.out.println("newh-"+ (new String(result)));
			for(int i = 0; i < result.length; i++){
				if(result[i] != hash[i]){
					System.err.println("Hash check for piece " + this.index + " failed at byte " + i + ".");
					this.data = new byte[this.data.length];
					this.datawritten = 0;
					return false;
				}
			}
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Unable to check hash of piece. Invalid Algorithm");
		} 
		return true;
	}
	
	public int getLength(){
		return this.data.length;
	}

}
