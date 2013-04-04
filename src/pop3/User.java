package pop3;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import pop3.restriction.Restriction;

public class User {

	private String server;
	private List<Restriction> restrictions = new LinkedList<Restriction>();
	private int connectionCount;
	private int maxConnections = -1;
	private TimeInterval unallowedConnectionInterval = new TimeInterval();
	private String username;

	public User(String username, String server) {
		this(username);
		this.server = server;
	}

	public User(String username) {
		this.username = username;
	}

	public String getServer() {
		return server;
	}

	public enum ConnectionErrors {
		MAX_CONNECTIONS_EXCEEDED(
				"Excedio la cantidad maxima de conexiones"), NOT_ALLOWED_NOW(
				"No se puede conectar a esta hora");

		String str;

		ConnectionErrors(String str) {
			this.str = str;
		}

		String getMessage() {
			return str;
		}
	}

	public ConnectionErrors connect() {
		if (maxConnections != -1) {
			if (connectionCount >= maxConnections) {
				return ConnectionErrors.MAX_CONNECTIONS_EXCEEDED;
			}
		}
		if (unallowedConnectionInterval.belongs(Calendar.getInstance().get(
				Calendar.HOUR_OF_DAY))) {
			return ConnectionErrors.NOT_ALLOWED_NOW;
		}
		connectionCount++;
		return null;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public void setAllowedConnectionInterval(ArrayList<Integer> interval) {
		try {
			unallowedConnectionInterval.set(interval);
		} catch (IllegalArgumentException e) {

		}
	}

	public void addUnallowedConnectionInterval(int from, int to) {
		unallowedConnectionInterval.addInterval(from, to);
	}

	private class TimeInterval {
		List<Integer> interval = new ArrayList<Integer>();

		TimeInterval() {

		}

		void set(ArrayList<Integer> list) {
			interval.clear();
			if (interval.size() % 2 != 0) {
				throw new IllegalArgumentException(
						"Interval length must be even");
			}
			int i = 0;
			for (int x : interval) {
				this.interval.add(x);
				if (i % 2 != 0) {
					if (this.interval.get(i - 1) <= x) {
						throw new IllegalArgumentException();
					}
				}
			}
		}

		boolean belongs(int x) {
			for (int i = 0; i < interval.size() ; i += 2) {
				if (interval.get(i) <= x && x <= interval.get(i + 1)) {
					return true;
				}
			}
			return false;
		}

		void addInterval(int from, int to) {
			if (from <= to) {
				interval.add(from);
				interval.add(to);
			}
		}

	}

	public boolean hasRestrictions() {
		return !restrictions.isEmpty();
	}

	public List<Restriction> getRestrictions() {
		return restrictions;
	}

	public void setServer(String server) {
		this.server = server;

	}

	public void addRestriction(Restriction restriction) {
		restrictions.add(restriction);
	}
	
	public String getUsername() {
		return username;
	}

	public void clearAllRestrictions() {
		restrictions.clear();
		maxConnections = -1;
		unallowedConnectionInterval = new TimeInterval();
	}

}
