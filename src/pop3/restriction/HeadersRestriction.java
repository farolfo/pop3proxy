package pop3.restriction;

import mime.MimeInfoSimplified;

public class HeadersRestriction extends ModifierRestriction<String> {

	private String header;
	private String value;
	
	public HeadersRestriction(String modifier, String header, String value) {
		super(modifier);
		this.header = header.toLowerCase();
		this.value = value.toLowerCase();
	}
	
	@Override
	public boolean validateRestriction(MimeInfoSimplified data) {
		for ( String header : data.getHeaders() ) {
			if ( header.toLowerCase().startsWith(this.header) ) {
				String[] parts = header.split(":");
				return super.validateRestriction(parts[1].trim().toLowerCase(), value);
			}
		}
		return true;
	}

}
