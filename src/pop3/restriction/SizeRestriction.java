package pop3.restriction;

import mime.MimeInfoSimplified;

public class SizeRestriction extends ModifierRestriction<Integer> {

	private int size;

	public SizeRestriction(String modifier, int size) {
		super(modifier);
		this.size = size;
	}

	@Override
	public boolean validateRestriction(MimeInfoSimplified data) {
		return validateRestriction(Integer.valueOf(data.getMailSizeInBytes()),
				Integer.valueOf(size));
	}

}
