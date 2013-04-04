package pop3.restriction;

import java.util.Calendar;

import mime.MimeInfoSimplified;

public class DateRestriction extends ModifierRestriction<Calendar> {

	private Calendar date;
	
	
	public DateRestriction(String modifier, Calendar date) {
		super(modifier);
		this.date = date;
	}
	
	@Override
	public boolean validateRestriction(MimeInfoSimplified data) {
		return super.validateRestriction(data.getDate(), date);
	}

}
