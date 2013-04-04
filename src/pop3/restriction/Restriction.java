package pop3.restriction;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import mime.MimeInfoSimplified;

public interface Restriction {
	
	public static final Set<String> set = new HashSet<String>(Arrays.asList(new String[] {"<", ">", "<=", ">=", "=", "!="}));
	public boolean validateRestriction(MimeInfoSimplified data);
}
