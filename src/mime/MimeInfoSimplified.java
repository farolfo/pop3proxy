package mime;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class MimeInfoSimplified {
	private int mailSizeInBytes = 0;
	private Calendar date;
	private String from;
	private List<AttachmentType> attachments = new LinkedList<AttachmentType>();
	private List<String> headers = new LinkedList<String>();
	
	
	public void incSize(int length) {
		mailSizeInBytes += length;
	}
	public void pushAttachment(AttachmentType attach) {
		attachments.add(attach);
	}
	public void setFrom(String from) {
		this.from = from;
	}
	
	public int getMailSizeInBytes(){
		return mailSizeInBytes;
	}
	
	public String getFrom(){
		return from;
	}
	public Calendar getDate() {
		return date;
	}
	public void setDate(Calendar date) {
		this.date = date;
	}
	
	public List<AttachmentType> getAttachments() {
		return attachments;
	}
	
	public void addHeader(String header) {
		headers.add(header);
	}
	
	public List<String> getHeaders() {
		return headers;
	}
}
