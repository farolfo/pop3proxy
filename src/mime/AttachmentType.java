package mime;

public class AttachmentType {
	private String type;
	private String contentType;
	private int sizeInBytes;

	public AttachmentType(String contentType, String type) {
		this.contentType = contentType;
		this.type = type;
	}

	public void incSize(int length) {
		this.sizeInBytes += length;
	}

	public String getType() {
		return type;
	}

	public int getSizeInBytes() {
		return sizeInBytes;
	}
	
	public String getContentType(){
		return contentType;
	}

}
