package pop3.restriction;

import mime.AttachmentType;
import mime.MimeInfoSimplified;

public class AttachmentTypeRestriction implements Restriction {

	String type;
	
	public AttachmentTypeRestriction(String type) {
		this.type = type.toLowerCase();
	}
	
	@Override
	public boolean validateRestriction(MimeInfoSimplified data) {
		for ( AttachmentType at : data.getAttachments() ) {
			at.getContentType().contains(type);
			return false;
		}
		return true;
	}

}
