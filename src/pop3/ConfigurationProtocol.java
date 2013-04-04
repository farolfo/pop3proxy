package pop3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import pop3.restriction.AttachmentTypeRestriction;
import pop3.restriction.DateRestriction;
import pop3.restriction.FromRestriction;
import pop3.restriction.HeadersRestriction;
import pop3.restriction.Restriction;
import pop3.restriction.SizeRestriction;
import pop3.restriction.StructureRestriction;

public class ConfigurationProtocol {

	ProxyServer proxy;
	Map<String, String> administrators;

	public ConfigurationProtocol(ProxyServer proxy) {
		this.proxy = proxy;
		this.administrators = new HashMap<String, String>();
	}

	public void doAccept(Selector selector, SelectionKey key)
			throws IOException {
		SocketChannel clntChan = ((ServerSocketChannel) key.channel()).accept();
		clntChan.configureBlocking(false);
		clntChan.register(selector, SelectionKey.OP_READ, new ConfigSession());
	}

	public void doRead(Selector selector, SelectionKey key) throws IOException {
		ByteBuffer buf = ((ConfigSession) key.attachment()).getBuffer();
		SocketChannel channel = (SocketChannel) key.channel();
		int bytesRead = channel.read(buf);
		if (bytesRead == -1) {
			channel.close();
		} else if (bytesRead > 0) {
			buf.flip();
			byte[] byteArray = new byte[buf.remaining()];
			buf.get(byteArray);
			String command = new String(byteArray);
			buf.clear();
			buf.put(executeCommand(parseCommand(command), key).getBytes());
			key.interestOps(SelectionKey.OP_WRITE);
		}
	}

	public void doWrite(Selector selector, SelectionKey key) throws IOException {
		ByteBuffer buf = ((ConfigSession) key.attachment()).getBuffer();
		SocketChannel channel = (SocketChannel) key.channel();
		buf.flip();
		channel.write(buf);
		if (!buf.hasRemaining()) {
			key.interestOps(SelectionKey.OP_READ);
		}
		buf.compact();
	}

	private String[] parseCommand(String command) {
		command = command.substring(0, command.indexOf('\n')).replaceAll(
				"[\t ]+", " ");
		return command.split(" ");
	}

	private String executeCommand(String[] args, SelectionKey key) {
		if (args.length == 0) {
			return "-ERR Unknown command\n";
		}
		String command = args[0].toUpperCase();
		args = Arrays.copyOfRange(args, 1, args.length);
		ConfigSession session = (ConfigSession) key.attachment();
		if (command.equals("AUTH")) {
			if (session.canExecute("auth")) {
				return executeAuth(args, session);
			}
		} else if (command.equals("SERVER")) {
			if (session.canExecute("server")) {
				return executeServer(args);
			}
		} else if (command.equals("DSERVER")) {
			if (session.canExecute("dserver")) {
				return executeDefaultServer(args);
			}
		} else if (command.equals("RESTRICT")) {
			if (session.canExecute("restrict")) {
				return executeRestrict(args);
			}
		} else if (command.equals("RESTRICTIP")) {
			if (session.canExecute("restrictip")) {
				return executeRestrictIp(args);
			}
		} else if (command.equals("STAT")) {
			if (session.canExecute("stat")) {
				return executeStat(args);
			}
		} else if (command.equals("UNRESTRICTIP")) {
			if (session.canExecute("unrestrictip")) {
				return executeUnrestrictip(args);
			}
		} else if (command.equals("CLOSE")) {
			if (session.canExecute("close")) {
				return executeClose(args, key);
			}
		}
		return "-ERR Unknown command\n";
	}

	private String executeUnrestrictip(String[] args) {
		if (args.length < 1) {
			return "-ERR Invalid arguments\n";
		}
		if (args.length > 1) {
			if (!RestrictedIPs.unbanIPBySubnet(args[0], args[1])) {
				return "-ERR Couldnt unblock that subnet\n";
			}
		} else {
			if (args[0].equals("*")) {
				RestrictedIPs.unbanAll();
			} else if (args[0].indexOf('/') != -1) {
				if (!RestrictedIPs.unbanIPBySubnet(args[0])) {
					return "-ERR Couldnt unblock that subnet\n";
				}
			} else {
				if (!RestrictedIPs.unbanIPByAddress(args[0])) {
					return "-ERR Couldnt unblock that ip\n";
				}
			}
		}
		return "+OK " + args[0] + " unblocked\n";
	}

	private String executeClose(String[] args, SelectionKey key) {
		ConfigSession session = (ConfigSession) key.attachment();
		session.closing(true);
		return "+OK See ya!\n";
	}

	private String executeStat(String[] args) {
		if (args.length != 0) {
			Stats stats = proxy.getStats(args[0]);
			int bytesTransfered = 0;
			int emailsRead = 0;
			int emailsDeleted = 0;
			StringBuilder builder = new StringBuilder();
			StringBuilder histogram = new StringBuilder();
			if (stats != null) {
				bytesTransfered = stats.getBytesTransfered();
				emailsRead = stats.getEmailsRead();
				emailsDeleted = stats.getEmailsDeleted();
				for (Calendar c : stats.getAccessLog()) {
					builder.append(c.get(Calendar.DAY_OF_MONTH) + "/"
							+ c.get(Calendar.MONTH) + "/"
							+ c.get(Calendar.YEAR) + " "
							+ c.get(Calendar.HOUR_OF_DAY) + ":"
							+ c.get(Calendar.MINUTE) + ":"
							+ c.get(Calendar.SECOND) + "\n");
				}

				for (Entry<String, Integer> entry : stats.getHistogram()
						.entrySet()) {
					histogram.append(entry.getKey() + ": " + entry.getValue()
							+ "\n");
				}
			} else {
				builder.append("\n");
				histogram.append("\n");
			}

			return "+OK Stats follow\nAccess dates:\n" + builder.toString()
					+ "Bytes transferred: " + bytesTransfered
					+ "\nMails read: " + emailsRead + "\nMails deleted: "
					+ emailsDeleted + "\nHistogram:\n" + histogram + ".\n";
		} else {
			Stats stats = proxy.getStats();
			StringBuilder histogram = new StringBuilder();
			for (Entry<String, Integer> entry : stats.getHistogram().entrySet()) {
				histogram.append(entry.getKey() + ": " + entry.getValue()
						+ "\n");
			}
			return "+OK Stats follow\nAmount of accesses: "
					+ stats.getTimesAccessed() + "\nBytes transferred: "
					+ stats.getBytesTransfered() + "\nMails read: "
					+ stats.getEmailsRead() + "\nMails deleted: "
					+ stats.getEmailsDeleted() + "\nHistogram:\n" + histogram
					+ ".\n";
		}

	}

	private String executeRestrictIp(String[] args) {
		if (args.length < 1) {
			return "-ERR Invalid arguments\n";
		}
		if (args.length > 1) {
			if (!RestrictedIPs.banIPBySubnet(args[0], args[1])) {
				return "-ERR Couldnt block that subnet\n";
			}
		} else {
			if (args[0].indexOf('/') != -1) {
				if (!RestrictedIPs.banIPBySubnet(args[0])) {
					return "-ERR Couldnt block that subnet\n";
				}
			} else {
				if (!RestrictedIPs.banIPByAddress(args[0])) {
					return "-ERR Couldnt block that ip\n";
				}
			}
		}
		return "+OK " + args[0] + " blocked\n";
	}

	private String executeRestrict(String[] args) {
		if (args.length < 3) {
			return "-ERR Invalid arguments\n";
		}
		String username = args[0], type = args[1].toUpperCase();
		args = Arrays.copyOfRange(args, 2, args.length);
		if (type.equals("CLEAR")) {
			if (proxy.getUsersMap().containsKey(username)) {
				User u = proxy.getUsersMap().get(username);
				u.clearAllRestrictions();
				return "+OK Restrcciones reseteadas";
			}
			return "-ERR El usuario no tenia restricciones seteadas";
		}
		if (type.equals("TIME")) {
			if (args.length < 2) {
				return "-ERR Invalid arguments\n";
			}
			int from = -1, to = -1;
			try {
				from = Integer.valueOf(args[0]);
				to = Integer.valueOf(args[1]);
			} catch (Exception e) {
				return "-ERR Invalid arguments\n";
			}
			return executeTimeRestrict(username, from, to);
		}
		if (type.equals("LOGIN")) {
			if (args.length < 1) {
				return "-ERR Invalid arguments\n";
			}
			int timesPerDay = Integer.valueOf(args[0]);
			return executeLoginRestrict(username, timesPerDay);
		}
		if (type.equals("DELETE")) {
			if (args.length < 2) {
				return "-ERR Invalid arguments\n";
			}
			String delType = args[0].toUpperCase();
			return executeDeleteRestrict(username, delType,
					Arrays.copyOfRange(args, 1, args.length));
		}
		return "-ERR Unknown type\n";
	}

	private String executeDeleteRestrict(String username, String delType,
			String[] condition) {
		if (condition.length < 1) {
			return "-ERR Invalid arguments";
		}
		if (delType.equals("DATE")) {
			return setDateRestriction(username, condition);
		}
		if (delType.equals("FROM")) {
			return setFromRestriction(username, condition);
		}
		if (delType.equals("HEADER")) {
			return setHeaderRestriction(username, condition);
		}
		if (delType.equals("CTYPE")) {
			return setContentTypeRestriction(username, condition);
		}
		if (delType.equals("SIZE")) {
			return setSizeRestriction(username, condition);
		}
		if (delType.equals("STRUCT")) {
			return setStructureRestriction(username, condition);
		}
		return "-ERR Invalid type\n";
	}

	private String setDateRestriction(String username, String[] condition) {
		if (condition.length < 2) {
			return "-ERR Invalid arguments\n";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(sdf.parse(condition[1]));
			return addRestriction(username,
					new DateRestriction(condition[0], c));
		} catch (Exception e) {
			return "-ERR Invalid arguments\n";
		}
	}

	private String setFromRestriction(String username, String[] condition) {

		if (condition.length < 2) {
			return "-ERR Invalid arguments\n";
		}
		boolean exactMatch = Boolean.valueOf(condition[1]);
		return addRestriction(username, new FromRestriction(condition[0],
				exactMatch));

	}

	private String addRestriction(String username, Restriction restriction) {
		if (username.equals("*")) {
			proxy.addGlobalRestriction(restriction);
			return "+OK Restriction added\n";
		}
		User u = proxy.getUsersMap().get(username);
		if (u == null) {
			u = new User(username, proxy.getDefaultServer());
			u.setMaxConnections(proxy.getMaxConnections());
			proxy.getUsersMap().put(username, u);
		}
		u.addRestriction(restriction);
		return "+OK Restriction added\n";
	}

	private String setHeaderRestriction(String username, String[] condition) {
		if (condition.length < 3) {
			return "-ERR Invalid arguments\n";
		}
		try {
			return addRestriction(username, new HeadersRestriction(
					condition[1], condition[0], condition[2]));
		} catch (Exception e) {
			return "-ERR Invalid arguments\n";
		}
	}

	private String setContentTypeRestriction(String username, String[] condition) {
		if (condition.length < 1) {
			return "-ERR Invalid arguments\n";
		}
		try {
			return addRestriction(username, new AttachmentTypeRestriction(
					condition[0]));
		} catch (NumberFormatException e) {
			return "-ERR Invalid arguments\n";
		}
	}

	private String setSizeRestriction(String username, String[] condition) {
		if (condition.length < 2) {
			return "-ERR Invalid arguments\n";
		}
		try {
			return addRestriction(username, new SizeRestriction(condition[0],
					Integer.valueOf(condition[1])));
		} catch (Exception e) {
			return "-ERR Invalid arguments\n";
		}
	}

	private String setStructureRestriction(String username, String[] condition) {
		if (condition.length < 1) {
			return "-ERR Invalid arguments\n";
		}
		String conditionAux = "";
		for (int i = 0; i < condition.length; i++) {
			conditionAux += condition[i];
		}
		try {
			return addRestriction(username, new StructureRestriction(
					conditionAux));
		} catch (NumberFormatException e) {
			return "-ERR Invalid arguments\n";
		}
	}

	private String executeLoginRestrict(String username, int timesPerDay) {
		if (username.equals("*")) {
			proxy.setMaxConnections(timesPerDay);
			for (User u : proxy.getUsersMap().values()) {
				u.setMaxConnections(timesPerDay);
			}
			return "+OK Noone can connect more than " + timesPerDay
					+ " times now\n";
		}
		User u;
		if (proxy.getUsersMap().containsKey(username)) {
			u = proxy.getUsersMap().get(username);
		} else {
			u = new User(username, proxy.getDefaultServer());
		}
		proxy.getUsersMap().put(username, u);
		u.setMaxConnections(timesPerDay);
		return "+OK!\n";
	}

	private String executeTimeRestrict(String username, int from, int to) {
		User u;
		if (proxy.getUsersMap().containsKey(username)) {
			u = proxy.getUsersMap().get(username);
		} else {
			u = new User(username, proxy.getDefaultServer());
			u.setMaxConnections(proxy.getMaxConnections());
		}
		proxy.getUsersMap().put(username, u);
		u.addUnallowedConnectionInterval(from, to);
		return "+OK!\n";
	}

	private String executeDefaultServer(String[] args) {
		if (args.length < 1) {
			return "-ERR Invalid arguments\n";
		}
		setDefaultServer(args[0]);
		return "+OK Default server changed to " + args[0];
	}

	private void setDefaultServer(String string) {
		proxy.setDefaultServer(string);
	}

	private String executeServer(String[] args) {
		if (args.length < 1) {
			return "-ERR Invalid arguments\n";
		}
		String username = args[0];
		if (username.equals("*")) {
			for (String user : proxy.getUsersMap().keySet()) {
				setServer(user, args.length == 1 ? null : args[1]);
			}
			if (args.length != 1) {
				setDefaultServer(args[1]);
			}
		} else {
			setServer(username, args.length == 1 ? null : args[1]);
		}
		return "+OK " + username + "'s server changed to "
				+ (args.length == 1 ? "localhost" : args[1] + "\n");
	}

	private void setServer(String username, String server) {
		User u = proxy.getUsersMap().get(username);
		if (u == null) {
			u = new User(username, server);
			u.setMaxConnections(proxy.getMaxConnections());
			proxy.getUsersMap().put(username, u);
		}
		u.setServer(server);

	}

	private String executeAuth(String[] args, ConfigSession session) {
		if (args.length < 2) {
			return "-ERR Invalid arguments\n";
		}
		String username = args[0], password = args[1];
		if (authenticate(username, password)) {
			session.authenticated();
			return "+OK Welcome professor\n";
		}
		return "-ERR Incorrect username or password\n";
	}

	private boolean authenticate(String username, String password) {
		if (administrators.get(username) != null
				&& administrators.get(username).equals(password)) {
			return true;
		}
		return false;
	}

	public void addAdministrators(Map<String, String> administratorsMap) {
		administrators.putAll(administratorsMap);
	}
}
