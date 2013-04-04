package pop3.restriction;

public abstract class ModifierRestriction<T> implements Restriction {

	private String modifier;

	protected ModifierRestriction(String modifier) {
		if (!Restriction.set.contains(modifier)) {
			throw new IllegalArgumentException();
		}
		this.modifier = modifier;
	}

	protected boolean validateRestriction(Comparable<T> field, T value) {
		if ((modifier.equals("=") && field.compareTo(value) == 0)
				|| (modifier.equals("!=") && field.compareTo(value) != 0)
				|| (modifier.equals("<") && field.compareTo(value) < 0)
				|| (modifier.equals(">") && field.compareTo(value) > 0)
				|| (modifier.equals("<=") && field.compareTo(value) <= 0)
				|| (modifier.equals(">=") && field.compareTo(value) >= 0)) {
			return false;
		}
		return true;
	}
}
