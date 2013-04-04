package pop3;

import java.nio.ByteBuffer;

public class ConfigSession {

	private enum State {
		AUTH, TRANSACTION, CLOSE;
	}

	private ByteBuffer buffer = ByteBuffer.allocate(Session.BUFFER_SIZE);
	private State state = State.AUTH;

	public ByteBuffer getBuffer() {
		return buffer;
	}

	public void setState(State state) {
		this.state = state;
	}

	public void authenticated() {
		this.state = State.TRANSACTION;
	}

	public boolean canExecute(String string) {
		if ( state.equals(State.AUTH) && string.equalsIgnoreCase("auth")) {
			return true;
		} 
		if ( state.equals(State.TRANSACTION) && !string.equalsIgnoreCase("auth")) {
			return true;
		}
		return false;
	}

	public boolean closing() {
		return state.equals(State.CLOSE);
	}

	public void closing(boolean b) {
		this.state = State.CLOSE;
		
	}


}
