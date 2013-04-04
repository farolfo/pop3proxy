package pop3;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import mime.MimeInfoSimplified;
import mime.MimeParser;
import pop3.restriction.Restriction;

public class MailParsingThread extends Thread{
	private MimeInfoSimplified mail;
	private List<Restriction> restrictions = new LinkedList<Restriction>();
	private SelectionKey key;
	private Queue<ByteBuffer> bufferQueue = new LinkedList<ByteBuffer>();
	private int mailToDelete;
	private boolean finished = false;
	
	public MailParsingThread(List<Restriction> globalRestrictions,
			List<Restriction> restrictions, SelectionKey key, int mailToDelete) {
		this.restrictions.addAll(globalRestrictions);
		this.restrictions.addAll(restrictions);
		this.key = key;
		this.mailToDelete = mailToDelete;
	}

	@Override
	public void run() {
		try {
			
			final MimeParser parser = new MimeParser();
			final PipedInputStream inputPipe = new PipedInputStream();
			PipedOutputStream outputPipe = new PipedOutputStream(inputPipe);
			Thread thread = new Thread() {
				@Override
				public void run() {
					try {
						mail = parser.parseSimplified(inputPipe);
					} catch (IOException e) {
						mail = null;
					}
				}
			};
			thread.start();
			while (!Thread.interrupted()) {
				try {
					if ( !thread.isAlive() ) {
						Session session = (Session) key.attachment();
						if ( mail == null ) {
							session.addToBuffer("-ERR Surgio un error al borrar el mensaje\n".getBytes());
						}
						for ( Restriction restriction : restrictions ) {
							if ( !restriction.validateRestriction(mail) ) {
								session.addToBuffer("-ERR No puede borrar ese mensaje\n".getBytes());
								return;
							}
						}
						Session ansSession = (Session) session.getKey().attachment();
						ansSession.addToBuffer(("DELE " + mailToDelete + "\n").getBytes());
						return;
					}
				} catch( IllegalThreadStateException e ) {
					
				}
				if ( finished && bufferQueue.isEmpty() ) {
                    outputPipe.close();
				}
				ByteBuffer b;
				synchronized(bufferQueue) {
					b = bufferQueue.poll();
				}
				if ( b != null ) {
					outputPipe.write(new String(b.array())
					.substring(b.position(),
							b.limit()).getBytes());
					outputPipe.flush();
				}
			}
		} catch (IOException e) {
			System.out.println("Error parsing mail!");
		}

	}
	
	public void addBuffer(ByteBuffer b) {
		synchronized(bufferQueue) {
			bufferQueue.add(b);
		}
	}

	public void finished() {
		finished = true;
	}

}
