package pop3.restriction;

import mime.MimeInfoSimplified;

public class StructureRestriction implements Restriction {

	private Boolean attachments = null;
	
	public StructureRestriction(String condition) {
		if ( condition.equals("attachments") ) {
			attachments = true;
		} else if ( condition.equals("no attachments") ) {
			attachments = false;
		}
	}
	
	@Override
	public boolean validateRestriction(MimeInfoSimplified data) {
		if( attachments == null ){
			return true;
		}
		if ( (data.getAttachments().size() > 0 && attachments) || (data.getAttachments().size() == 0 && !attachments)) {
			return false;
		}
		return true;
	}
	

}
