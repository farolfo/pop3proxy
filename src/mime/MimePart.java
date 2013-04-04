package mime;

public class MimePart {

	String transferEncoding;
	String body = "";
	String type;
	String contentDisposition;
	private String header = "";
	private String footer = "";

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getType() {
		return type;
	}

	public void addHeader(String header) {
		this.header += header;
	}

	public void addFooter(String header) {
		this.footer += footer;
	}

	public String getFooter() {
		return footer;
	}

	public String getHeader() {
		return header;
	}

}
