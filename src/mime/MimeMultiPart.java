package mime;

import java.util.LinkedList;
import java.util.List;

public class MimeMultiPart {

	String type;
	String boundary;
	List<MimePart> parts = new LinkedList<MimePart>();
	private String header = "";
	private String footer = "";

	public List<MimePart> getParts() {
		return parts;
	}

	public void addHeader(String header) {
		this.header += header;
	}

	public void addFooter(String footer) {
		this.footer += footer;
	}

	public String getHeader() {
		return header;
	}

	public String getFooter() {
		return footer;
	}
}
