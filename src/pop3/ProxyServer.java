package pop3;


import initialConf.AppConf;
import initialConf.jaxb.XMLParser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import pop3.Session.Command;
import pop3.Session.State;
import pop3.restriction.Restriction;

public class ProxyServer {

	private static final int TIMEOUT = 3000;

	private int destinationPort = 110;

	private String defaultServer;

	private Selector selector;
	private Map<String, User> usersMap;
	private ConfigurationProtocol configProtocol;
	private String transformerPath;
	private List<Restriction> restrictions;
	private Map<String, Stats> statsMap = new HashMap<String, Stats>();
	private int maxConnections = -1;

	public static void main(String[] args) {
		ProxyServer proxy = new ProxyServer();
		AppConf appConf = new XMLParser();
		if (!appConf.loadSettings()) {
			return;
		}
		proxy.usersMap = appConf.getUsersMap();
		proxy.transformerPath = appConf.getTransformerPath();
		proxy.restrictions = appConf.getGlobalRestrictions();
		proxy.defaultServer = appConf.getDefaultServer();
		System.out.println("Default server : " + proxy.defaultServer);
		appConf.loadRestrictedIPs();
		proxy.configProtocol = new ConfigurationProtocol(proxy);
		proxy.configProtocol.addAdministrators(appConf.getAdministratorsMap());
		int port = 3000;
		int configPort = 0xCACA;
		int destinationPort = 110;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-p")) {
				if (i + 1 == args.length) {
					System.out.println("Port number missing");
				} else {
					try {
						port = Integer.valueOf(args[i + 1]);
					} catch (NumberFormatException e) {
						System.out.println("Invalid port number");
						return;
					}
				}
			} else if (args[i].startsWith("-p")) {
				try {
					port = Integer.valueOf(args[i].substring(2));
				} catch (NumberFormatException e) {
					System.out.println("Invalid port number");
					return;
				}

			}
			if (args[i].equals("-d")) {
				if (i + 1 == args.length) {
					System.out.println("Port number missing");
				} else {
					try {
						destinationPort = Integer.valueOf(args[i + 1]);
					} catch (NumberFormatException e) {
						System.out.println("Invalid port number");
						return;
					}
				}
			} else if (args[i].startsWith("-d")) {
				try {
					destinationPort = Integer.valueOf(args[i].substring(2));
				} catch (NumberFormatException e) {
					System.out.println("Invalid port number");
					return;
				}

			}
			if (args[i].equals("-c")) {
				if (i + 1 == args.length) {
					System.out.println("Port number missing");
				} else {
					try {
						configPort = Integer.valueOf(args[i + 1]);
					} catch (NumberFormatException e) {
						System.out.println("Invalid port number");
						return;
					}
				}
			} else if (args[i].startsWith("-c")) {
				try {
					configPort = Integer.valueOf(args[i].substring(2));
				} catch (NumberFormatException e) {
					System.out.println("Invalid port number");
					return;
				}

			}
		}
		System.out.println("Running on port " + port);
		proxy.run(port, destinationPort, configPort);
	}

	public void run(int port, int destinationPort, int configPort) {
		ServerSocketChannel listnChannel = null;
		ServerSocketChannel configListnChannel = null;
		try {
			this.selector = Selector.open();
			this.destinationPort = destinationPort;
			System.out.println("Destination port: " + this.destinationPort);
			listnChannel = ServerSocketChannel.open();
			configListnChannel = ServerSocketChannel.open();

			listnChannel.socket().bind(new InetSocketAddress("0.0.0.0", port));
			listnChannel.configureBlocking(false);
			listnChannel.register(selector, SelectionKey.OP_ACCEPT);

			configListnChannel.socket().bind(
					new InetSocketAddress("0.0.0.0", configPort));
			configListnChannel.configureBlocking(false);
			configListnChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			System.out.println("Couldnt start, reason: " + e.getMessage());
			return;
		}

		while (true) {

			try {
				if (selector.select(TIMEOUT) == 0) {
					continue;
				}
			} catch (IOException e1) {

			}
			Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
			while (keyIter.hasNext()) {
				SelectionKey key = keyIter.next();
				try {
					if (key.isValid() && key.isAcceptable()) {
						if (key.channel().equals(listnChannel)) {
							SocketChannel clntChan = ((ServerSocketChannel) key
									.channel()).accept();
							clntChan.configureBlocking(false);
							SelectionKey clientKey = clntChan.register(
									selector, SelectionKey.OP_WRITE);
							Session session = new Session(clientKey, clntChan,
									false);
							clientKey.attach(session);
							if (RestrictedIPs.isBanned(clntChan.socket()
									.getInetAddress())) {
								session.setState(State.CONNECTION_ERROR);
								session.addToBuffer(("-ERR ROFL tu ip esta baneada\n")
										.getBytes());
							} else {
								session.addToBuffer(("+OK POP3 server Aloha\n")
										.getBytes());
							}
						} else if (key.channel().equals(configListnChannel)) {
							configProtocol.doAccept(selector, key);

						}
					}
					if (key.isValid() && key.isReadable()) {

						if (key.attachment() instanceof Session) {

							Session session = (Session) key.attachment();
							SelectionKey ansKey = (SelectionKey) session
									.getKey();
							SocketChannel channel = (SocketChannel) key
									.channel();

							long bytesRead = 0;

							ByteBuffer auxBuffer = ByteBuffer
									.allocate(Session.BUFFER_SIZE);
							if (!session.isFromServer()) {
								bytesRead = channel.read(auxBuffer);
							} else {
								if (session.getState().equals(State.FIRST)
										|| session.getState().equals(
												State.CONNECTION_ERROR)) {
									ByteBuffer trash = ByteBuffer
											.allocate(1024);
									bytesRead = channel.read(trash);
								} else {
									bytesRead = channel.read(auxBuffer);
								}
							}

							if (bytesRead == -1) {
								ansKey.channel().close();
								session.killThreads();
								((Session) session.getKey().attachment())
										.killThreads();
								channel.close();
								keyIter.remove();
								continue;
							}
							// else if (bytesRead > 0) {
							// System.out.println(new String(auxBuffer.array())
							// .substring(auxBuffer.arrayOffset(),
							// auxBuffer.position()));
							// }
							if (!session.isFromServer()) {
								Command command = session
										.processUser(auxBuffer);
								if (command != null) {
									processCommand(command, session, key);
								}
							} else {
								if (session.getState().equals(State.FIRST)) {
									session.addToBuffer(("USER "
											+ session.getUser().getUsername() + "\n")
											.getBytes());
									session.setState(State.AUTH);
								} else if (session.getState().equals(
										State.FIRST_TRANSFORM)
										|| session.getState().equals(
												State.TRANSFORM)) {
									auxBuffer.flip();
									if (session.getState().equals(
											State.FIRST_TRANSFORM)) {
										// Sacar la primera linea +OK...
										ByteBuffer okBuffer = ByteBuffer
												.allocate(Session.BUFFER_SIZE);
										Session ansSession = ((Session) session
												.getKey().attachment());
										try {
											byte b;
											while ((b = auxBuffer.get()) != '\n') {
												okBuffer.put(b);
											}
											okBuffer.put(b);
										} catch (BufferUnderflowException e) {
											ansSession
													.addToBuffer("-ERR El server no devolvio una respuesta valida\n"
															.getBytes());
											keyIter.remove();
											System.out
													.println("El server no devolvio una respuesta valida al pedido de "
															+ session
																	.getUser()
																	.getUsername());
											continue;
										}
										okBuffer.flip();
										if (okBuffer.get(0) == '-') {
											keyIter.remove();
											ansSession.addBuffer(okBuffer);
											System.out
													.println("El server respondio error al pedido de "
															+ session
																	.getUser()
																	.getUsername());
											continue;
										}
										System.out
												.println("El server respondio ok al pedido de "
														+ session.getUser()
																.getUsername());
										session.runTransformation(new String(
												okBuffer.array()).substring(0,
												okBuffer.limit()));
										session.setState(State.TRANSFORM);
									}
									boolean hasDot = checkLastLine(auxBuffer);
									System.out.println("lei" + bytesRead
											+ " bytes y " + hasDot);
									session.addToTransfromThread(auxBuffer);
									if (hasDot) {
										statsMap.get(
												session.getUser().getUsername())
												.emailRead();
										session.setState(State.TRANS);
										session.finishTransformation();
									}
								} else if (session.getState().equals(
										State.DELETING)
										|| session.getState().equals(
												State.FIRST_DELETING)) {
									auxBuffer.flip();
									if (session.getState().equals(
											State.FIRST_DELETING)) {
										// Sacar la primera linea +OK...
										ByteBuffer okBuffer = ByteBuffer
												.allocate(Session.BUFFER_SIZE);
										Session ansSession = ((Session) session
												.getKey().attachment());
										try {
											byte b;
											while ((b = auxBuffer.get()) != '\n') {
												okBuffer.put(b);
											}
											okBuffer.put(b);
										} catch (BufferUnderflowException e) {
											ansSession
													.addToBuffer("-ERR El server no devolvio una respuesta valida\n"
															.getBytes());
											keyIter.remove();
											System.out
													.println("El server no devolvio una respuesta valida al pedido de "
															+ session
																	.getUser()
																	.getUsername());
											continue;
										}
										okBuffer.flip();
										if (okBuffer.get(0) == '-') {
											ansSession.addBuffer(okBuffer);
											keyIter.remove();
											System.out
													.println("El server respondio error al pedido de "
															+ session
																	.getUser()
																	.getUsername());
											continue;
										}
										System.out
												.println("El server respondio ok al pedido de "
														+ session.getUser()
																.getUsername());
										statsMap.get(
												session.getUser().getUsername())
												.emailDeleted();
										// session.initializateMailParsing(restrictions);
										session.setState(State.DELETING);
									}
									boolean hasDot = checkLastLine(auxBuffer);
									session.addToMailParsingThread(auxBuffer);
									if (hasDot) {
										session.finishMailParsing();
										session.setState(State.TRANS);
									} else {
										if (session.mailThreadDead()) {
											session.setState(State.TRANS);
										}
									}
								} else if (!session.getState().equals(
										State.CONNECTION_ERROR)) {
									Session ansSession = ((Session) session
											.getKey().attachment());
									auxBuffer.flip();
									String ans = new String(auxBuffer.array())
											.substring(auxBuffer.arrayOffset(),
													auxBuffer.limit());
									ansSession.addBuffer(auxBuffer);
									if (session.getState().equals(
											State.RETRIEVING)) {
										if (ans.toLowerCase().startsWith("+ok")) {
											Stats stats = statsMap.get(session
													.getUser().getUsername());
											stats.emailRead();
											System.out
													.println("El server respondio ok al pedido de "
															+ session
																	.getUser()
																	.getUsername());
										} else if (ans.toLowerCase()
												.startsWith("-ERR")) {
											System.out
													.println("El server respondio error al pedido de "
															+ session
																	.getUser()
																	.getUsername());
										}
										session.setState(State.TRANS);
									} else if (session.getState().equals(
											State.NO_RESTRICTION_DELETING)) {
										if (ans.toLowerCase().startsWith("+ok")) {
											Stats stats = statsMap.get(session
													.getUser().getUsername());
											stats.emailDeleted();
											System.out
													.println("El server respondio ok al pedido de "
															+ session
																	.getUser()
																	.getUsername());
										} else if (ans.toLowerCase()
												.startsWith("-ERR")) {
											System.out
													.println("El server respondio error al pedido de "
															+ session
																	.getUser()
																	.getUsername());
										}
										session.setState(State.TRANS);
									} else if (session.getState().equals(
											State.PASS)) {
										if (ans.toLowerCase().startsWith("+ok")) {
											User.ConnectionErrors error = session
													.getUser().connect();
											if (error != null) {
												ansSession
														.setState(State.CONNECTION_ERROR);
												ansSession.clearBuffers();
												ansSession
														.addToBuffer(("-ERR "
																+ error.getMessage() + "\n")
																.getBytes());
												session.addToBuffer("QUIT\n"
														.getBytes());
												session.setState(State.CONNECTION_ERROR);
											} else {
												session.setState(State.TRANS);
												ansSession
														.setState(State.TRANS);
												Stats stats = statsMap
														.get(session.getUser()
																.getUsername());
												if (stats == null) {
													stats = new Stats();
													statsMap.put(session
															.getUser()
															.getUsername(),
															stats);
												}
												stats.loggedIn();
												System.out.println(session
														.getUser()
														.getUsername()
														+ " se logueo");
											}
										} else {
											session.setState(State.AUTH);
										}
									} else {
										if (auxBuffer.get(0) == '+') {
											System.out
													.println("El server respondio ok al pedido de "
															+ session
																	.getUser()
																	.getUsername());
										} else if (auxBuffer.get(0) == '-') {
											System.out
													.println("El server respondio error al pedido de "
															+ session
																	.getUser()
																	.getUsername());
										}
									}
								}
							}
						} else if (key.attachment() instanceof ConfigSession) {
							try {
								configProtocol.doRead(selector, key);
							} catch (IOException e) {
							}
						}
					}
					if (key.isValid() && key.isWritable()) {
						if (key.attachment() instanceof Session) {
							Session session = (Session) key.attachment();
							ByteBuffer buf = session.getWritingBuffer();
							if (buf == null) {
								key.interestOps(SelectionKey.OP_READ);
								keyIter.remove();
								continue;
							}
							SocketChannel clntChan = (SocketChannel) key
									.channel();
							int bytesWritten = clntChan.write(buf);

							if (session.getUser() != null
									&& statsMap.get(session.getUser()
											.getUsername()) != null) {
								statsMap.get(session.getUser().getUsername())
										.bytesTansferred(bytesWritten);
							}
							if (!buf.hasRemaining() && !session.canWrite()) {
								key.interestOps(SelectionKey.OP_READ);
							}
							if (session.getState().equals(
									State.CONNECTION_ERROR)
									|| session.getState().equals(State.QUIT)) {
								SocketChannel channel = (SocketChannel) key
										.channel();
								channel.close();
							}
						} else if (key.attachment() instanceof ConfigSession) {
							ConfigSession session = (ConfigSession) key.attachment();
							configProtocol.doWrite(selector, key);
							if ( session.closing() ) {
								key.channel().close();
							}
						}

					}
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Warning: " + e.getMessage());
					Session session = (Session) key.attachment();
					try {
						key.channel().close();
						session.getKey().channel().close();
						
					} catch (IOException e1) {
					}
				}
				keyIter.remove();
			}
		}
	}

	private boolean checkLastLine(ByteBuffer buffer) {
		return (buffer.get(0) == '.' && (buffer.get(1) == '\n' || (buffer
				.get(1) == '\r' && buffer.get(2) == '\n')))
				|| (buffer.get(buffer.limit() - 1) == '\n' && (buffer
						.get(buffer.limit() - 2) == '.' || (buffer.get(buffer
						.limit() - 2) == '\r' && buffer.get(buffer.limit() - 3) == '.')));
	}

	private void processCommand(Command command, Session session,
			SelectionKey key) throws IOException {
		if (session.getUser() != null) {
			if (!command.getCommandName().equals("pass")) {
				System.out.println(session.getUser().getUsername() + " envio "
						+ command.getCommand());
			}
		}
		if (session.getKey().equals(key)
				&& !command.getCommandName().equals("user")) {
			if (command.getCommandName().equals("quit")) {
				session.setState(State.QUIT);
				session.addToBuffer("+OK bye!\n".getBytes());
			} else {
				session.addToBuffer(("-ERR Unknown command "
						+ command.getCommandName() + "\n").getBytes());
			}
			return;
		}

		if (command.getCommandName().equals("user")) {
			String[] split = command.getCommand().split(" ");
			if (split.length < 2) {
				session.addToBuffer(("-ERR Invalid arguments" + "\n")
						.getBytes());
				return;
			}
			String userName = command.getCommand().split(" ")[1];
			if (userName.endsWith("\r")) {
				userName = userName.substring(0, userName.length() - 1);
			}
			User user;
			String server = defaultServer;
			if ((user = usersMap.get(userName)) != null
					&& user.getServer() != null) {
				// user configuration saved
				server = user.getServer();
			}

			if (session.getUser() != null) {
				session.getKey().channel().close();
			}

			try {
				SocketChannel senderSocket = SocketChannel
						.open(new InetSocketAddress(server, destinationPort));
				senderSocket.configureBlocking(false);

				SelectionKey senderSocketKey = senderSocket.register(selector,
						SelectionKey.OP_READ);
				Session ansSession = new Session(key,
						(SocketChannel) key.channel(), true);
				session.setKey(senderSocketKey);
				session.setChannel(senderSocket);
				// ansSession
				// .addToBuffer((command.getCommand() + "\n").getBytes());
				ansSession.setState(State.FIRST);
				senderSocketKey.attach(ansSession);

				if (usersMap.containsKey(userName)) {
					ansSession.setUser(usersMap.get(userName));
					session.setUser(usersMap.get(userName));
				} else {
					User u = new User(userName, server);
					u.setMaxConnections(maxConnections);
					ansSession.setUser(u);
					session.setUser(u);
					usersMap.put(userName, u);
				}
			} catch (Exception e) {
				session.addToBuffer("-ERR Couldn't connect\n".getBytes());
				return;
			}
		} else {
			Session ansSession = (Session) session.getKey().attachment();
			if (session.getState().equals(State.AUTH)
					&& command.getCommandName().equals("pass")) {
				ansSession.setState(State.PASS);
				ansSession
						.addToBuffer((command.getCommand() + "\n").getBytes());
			} else if (session.getState().equals(State.TRANS)
					&& command.getCommandName().equals("retr")) {
				if (transformerPath != null) {
					ansSession.setState(State.FIRST_TRANSFORM);
					ansSession.transformUsing(transformerPath);
					ansSession.addToBuffer((command.getCommand() + "\n")
							.getBytes());
				} else {
					ansSession.setState(State.RETRIEVING);
					ansSession.addToBuffer((command.getCommand() + "\n")
							.getBytes());
				}
			} else if (session.getState().equals(State.TRANS)
					&& command.getCommandName().equals("dele")) {
				User u = session.getUser();
				if (restrictions.isEmpty() && !u.hasRestrictions()) {
					ansSession.addToBuffer((command.getCommand() + "\n")
							.getBytes());
					ansSession.setState(State.NO_RESTRICTION_DELETING);
				} else {
					String[] delArgs = command.getCommand().split(" ");
					if (delArgs.length > 1) {
						ansSession.addToBuffer(("retr " + delArgs[1] + "\n")
								.getBytes());
						ansSession.setState(State.FIRST_DELETING);
						try {
							ansSession.initializateMailParsing(restrictions,
									Integer.valueOf(delArgs[1]));
						} catch (NumberFormatException e) {
							session.addToBuffer("-ERR Invalid arguments\n"
									.getBytes());
						}
					} else {
						ansSession.addToBuffer((command.getCommand() + "\n")
								.getBytes());
					}
				}
			} else {
				ansSession
						.addToBuffer((command.getCommand() + "\n").getBytes());
			}
		}
	}

	public Map<String, User> getUsersMap() {
		return usersMap;
	}

	public String getDefaultServer() {
		return defaultServer;
	}

	public void setDefaultServer(String defaultServer) {
		this.defaultServer = defaultServer;

	}

	public Stats getStats(String username) {
		return statsMap.get(username);
	}

	public void addGlobalRestriction(Restriction r) {
		restrictions.add(r);
	}

	public Stats getStats() {
		Stats stat = new Stats();
		Map<String, Integer> histogram = new HashMap<String, Integer>();
		for (Stats stats : statsMap.values()) {
			stat.setBytesTransfered(stats.getBytesTransfered()
					+ stat.getBytesTransfered());
			stat.setEmailsDeleted(stats.getEmailsDeleted()
					+ stat.getEmailsDeleted());
			stat.setEmailsRead(stats.getEmailsRead() + stat.getEmailsRead());
			stat.setTimesAccessed(stats.getTimesAccessed()
					+ stat.getTimesAccessed());
			for (Entry<String, Integer> entry : stats.getHistogram().entrySet()) {
				int previousAccesses = histogram.containsKey(entry.getKey()) ? histogram
						.get(entry.getKey()) : 0;
				histogram.put(entry.getKey(),
						previousAccesses + entry.getValue());
			}
			stat.setHistogram(histogram);
		}
		return stat;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

}
