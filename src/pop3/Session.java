package pop3;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import pop3.restriction.Restriction;

public class Session {

	public static final int BUFFER_SIZE = 1024;

	// private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
	private SelectionKey key;
	private SocketChannel channel;
	private boolean fromServer;
	private State state = State.AUTH;
	private CharBuffer auxBuffer = CharBuffer.allocate(BUFFER_SIZE);
	private User user;
	private Queue<ByteBuffer> bufferQueue = new LinkedList<ByteBuffer>();
	private TransformThread thread;
	private MailParsingThread mailThread;

	public Session(SocketChannel channel, boolean fromServer) {
		this(null, channel, fromServer);
	}

	public Session(SelectionKey key, SocketChannel channel, boolean fromServer) {
		this.key = key;
		this.channel = channel;
		this.fromServer = fromServer;
	}

	// public ByteBuffer getBuffer() {
	// return buffer;
	// }

	public boolean canWrite() {
		return !bufferQueue.isEmpty();
	}

	public ByteBuffer getWritingBuffer() {
		ByteBuffer b = null;
		synchronized (bufferQueue) {
			b = bufferQueue.poll();
		}
		return b;
	}

	// public void setBuffer(ByteBuffer buffer) {
	// this.buffer = buffer;
	// }

	public SelectionKey getKey() {
		return key;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}

	public SocketChannel getChannel() {
		return channel;
	}

	public void setChannel(SocketChannel channel) {
		this.channel = channel;
	}

	public boolean isFromServer() {
		return fromServer;
	}

	public void setFromServer(boolean fromServer) {
		this.fromServer = fromServer;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public enum State {
		AUTH, TRANS, UPDATE, FIRST, PASS, CONNECTION_ERROR, TRANSFORM, FIRST_TRANSFORM, DELETING, FIRST_DELETING, QUIT, RETRIEVING, NO_RESTRICTION_DELETING;
	}

	public class Command {

		private String commandName;
		private String command;

		Command(String commandName, String command) {
			this.commandName = commandName.toLowerCase();
			this.command = command;
		}

		public String getCommand() {
			return command;
		}

		public String getCommandName() {
			return commandName;
		}
	}

	public Command processUser(ByteBuffer buffer) {
		buffer.flip();
		while (buffer.hasRemaining()) {
			byte b = buffer.get();
			if (b == '\n') {
				auxBuffer.flip();
				Command command = processCommand(auxBuffer.toString());
				auxBuffer.compact();
				buffer.compact();
				auxBuffer.clear();
				return command;
			}
			auxBuffer.put((char) b);
		}
		buffer.compact();
		return null;
	}

	private Command processCommand(String string) {
		if (State.AUTH.equals(state)) {
			String[] split = string.split(" ");
			return new Command(split[0], string);
		} else {
			String[] split = string.split(" ");
			return new Command(split[0], string);
		}
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	public void addBuffer(ByteBuffer buffer) {
		synchronized(bufferQueue) {
			bufferQueue.add(buffer);
		}
		SelectionKey ansKey = ((Session) key.attachment()).getKey();
		if ( ansKey.isValid() ) {
			ansKey.interestOps(ansKey.interestOps() | SelectionKey.OP_WRITE);
		}
	}

	public void clearBuffers() {
		bufferQueue.clear();

	}

	public void addToBuffer(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
		buffer.put(bytes);
		buffer.flip();
		synchronized(bufferQueue) {
			bufferQueue.add(buffer);
		}
		SelectionKey ansKey = ((Session) key.attachment()).getKey();
		if ( ansKey.isValid() ) {
			ansKey.interestOps(ansKey.interestOps() | SelectionKey.OP_WRITE);
		}
	}

	public void addToTransfromThread(ByteBuffer buffer) {
		thread.addBuffer(buffer);
	}

	public void transformUsing(String path) {
		thread = new TransformThread(path, key);
	}

	public void runTransformation(String string) {
		thread.setFirstMessage(string);
		thread.start();
	}

	public void finishTransformation() {
		thread.finished();
	}

	public void addToMailParsingThread(ByteBuffer buffer) {
		mailThread.addBuffer(buffer);
	}

	public void initializateMailParsing(List<Restriction> globalRestrictions,
			int mailToDelete) {
		mailThread = new MailParsingThread(globalRestrictions,
				user.getRestrictions(), key, mailToDelete);
		mailThread.start();
	}

	public void killThreads() {
		if (thread != null) {
			thread.interrupt();
		}
		if (mailThread != null) {
			mailThread.interrupt();
		}

	}

	public void finishMailParsing() {
		mailThread.finished();
	}

	public boolean transformThreadDead() {
		return !thread.isAlive();
	}

	public boolean mailThreadDead() {
		return !mailThread.isAlive();
	}
}
