package pop3;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.Queue;

public class TransformThread extends Thread {

	private Queue<ByteBuffer> bufferQueue = new LinkedList<ByteBuffer>();
	private String path;
	private SelectionKey key;
	private boolean finished;
	private boolean firstTime = true;
	private String firstMessage;

	public TransformThread(String path, SelectionKey key) {
		this.path = path;
		this.key = key;
	}

	@Override
	public void run() {
		Process p;
		try {
			ProcessBuilder builder = new ProcessBuilder();
			System.out.println("A ejecutar le programa externo en >" + path
					+ "<");
			builder.command(path.split(" "));
			p = builder.start();
			Reader programOutput = new InputStreamReader(p.getInputStream());
			Writer programInput = new OutputStreamWriter(p.getOutputStream());
			while (!Thread.interrupted()) {
				try {
					int value = p.exitValue();
					if (!programOutput.ready()) {
						programOutput.close();
						if ( value != 0 ) {
							if(firstTime){
								throw new Exception();
							}
							Session session = (Session) key.attachment();
							session.addToBuffer(new String(".\n").getBytes());
							System.out.println("El programa de transformacion fallo");
						}
						return;
					}
				} catch (IllegalThreadStateException e) {
				}
				while (programOutput.ready()) {
					Session session = (Session) key.attachment();
					if ( firstTime ) {
						firstTime = false;
						session.addToBuffer(firstMessage.getBytes());
					}
					char[] charBuffer = new char[Session.BUFFER_SIZE];
					programOutput.read(charBuffer);
					session.addToBuffer(new String(charBuffer).getBytes());
				}
				ByteBuffer b;
				if (bufferQueue.isEmpty()) {
					if (finished) {
						programInput.close();
					}
					continue;
				}
				b = null;
				synchronized (bufferQueue) {
					b = bufferQueue.poll();
				}
				if (b != null) {
					if (b.hasRemaining()) {
						programInput.write(new String(b.array()).substring(
								b.position(), b.limit()));
						programInput.flush();
					}
				}
			}
		} catch (Exception e) {
			((Session) key.attachment())
					.addToBuffer("-ERR Surgio un error al transformar\n"
							.getBytes());
			System.out.println("Error transforming!");
		}

	}

	public void addBuffer(ByteBuffer b) {
		synchronized (bufferQueue) {
			bufferQueue.add(b);
		}
	}
	
	public void setFirstMessage(String firstMessage) {
		this.firstMessage = firstMessage;
	}

	public void finished() {
		finished = true;
	}

}
