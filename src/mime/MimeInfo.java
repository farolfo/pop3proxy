package mime;

import java.util.List;

public class MimeInfo {
	private HeaderInfo headerInfo;
	private List<MimeMultiPart> mimeParts;
	private String header = "";
	
	public void addHeader(String header) {
		this.header += header;
	}
	
	public String getHeader() {
		return header;
	}
	
	public MimeInfo(HeaderInfo headerInfo, List<MimeMultiPart> mimeParts) {
		this.headerInfo = headerInfo;
		this.mimeParts = mimeParts;
	}

	public List<MimeMultiPart> getMimeParts() {
		return mimeParts;
	}

	public HeaderInfo getHeaderInfo() {
		return headerInfo;
	}


	
	

}
