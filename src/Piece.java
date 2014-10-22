
public class Piece {
	
	private int index;
	private byte[] data;
	
	public Piece(int index, int length, int numPieces){
		this.index = index;
		data = new byte[length];
	}
	
	public void savePiece(int offset, byte[] data){
		if(data == null)
			return;
		for(int i = 0; i < data.length && offset + 1 < this.data.length; i++){
			this.data[offset + i] = data[i];
		}
	}
	
	public int getIndex(){
		return this.index;
	}
	
	public byte[] getData(){
		return this.data;
	}
	
	public boolean isValid(String hash){
		return false;	
	}

}
