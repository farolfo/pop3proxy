package mime;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MimeParser {

	// public static void main(String[] args) throws IOException {
	// BufferedReader bb = new BufferedReader(new StringReader("lalalalalal" +
	// '\n' + "caomidajdflkasd"));
	// String l = "";
	// while((l = bb.readLine())!=null){
	// System.out.println(l);
	// }
	// }
	private static final int EOF = -1;
	private static final String version = "MIME-Version: ";
	private static final String received = "Received: ";// puede repetirse
	private static final String date = "Date: ";
	private static final String messageID = "Message-ID: ";
	private static final String subject = "Subject: ";
	private static final String from = "From: ";
	private static final String to = "To: ";
	private static final String contentType = "Content-Type: ";
	private static final String contentTransferEncoding = "Content-Transfer-Encoding: ";
	private static final String contentDisposition = "Content-Disposition: ";
	private static final String attachmentId = "X-Attachment-Id: ";

	private boolean EOFflag = false;

	public MimeInfo parse(InputStream in) throws IOException {
		EOFflag = false;
		String line;
		String nextLine;

		HeaderInfo headerInfo = new HeaderInfo();
		List<MimeMultiPart> mimeParts = new LinkedList<MimeMultiPart>();
		MimeInfo resp = new MimeInfo(headerInfo, mimeParts);
		String aux[];

		// parsing the mime header
		line = getNextLine(in);
		while (line != null) {
			if (!line.isEmpty()) {
				while ((nextLine = getNextLine(in)) != null
						&& nextLine.length() >= 1 && nextLine.charAt(0) == ' ') {
					line = line + nextLine;
				}

				line += "\n";
				
				if (line.startsWith(version)) {
					headerInfo.version = line.split("MIME-Version: ")[1];
					resp.addHeader(line);
				} else if (line.startsWith(received)) {
					resp.addHeader(line);
				} else if (line.startsWith(date)) {
					headerInfo.date = line.split("Date: ")[1];
					resp.addHeader(line);
				} else if (line.startsWith(messageID)) {
					resp.addHeader(line);
				} else if (line.startsWith(subject)) {
					resp.addHeader(line);
				} else if (line.startsWith(from)) {
					resp.addHeader(line);
				} else if (line.startsWith(to)) {
					resp.addHeader(line);
				} else if (line.startsWith(contentType)) {
					aux = line.split("Content-Type: ");
					aux = aux[1].split(" boundary=");
					String type = aux[0].replaceAll(";", "");
					if (aux[0].startsWith("multipart")) {
						String boundary = aux[1];
						parseMultipart(mimeParts, type, boundary, in, line);
					} else {
						parseNotMultipart(mimeParts, type, in, line);
					}
				} else {
				}
				line = nextLine;
			} else {
				line = getNextLine(in);
			}
		}
		return resp;
	}

	private void parseNotMultipart(List<MimeMultiPart> mimeParts, String type,
			InputStream in, String header) throws IOException {
		String line;
		String nextLine;
		String aux[];
		MimeMultiPart multiPart = new MimeMultiPart();
		mimeParts.add(multiPart);
		multiPart.type = type;
		MimePart part = new MimePart();
		part.addHeader(header);
		line = getNextLine(in);
		while (line != null) {
			if (!line.isEmpty()) {

				while ((nextLine = getNextLine(in)) != null
						&& !nextLine.isEmpty() && nextLine.charAt(0) == ' ') {
					line = line + nextLine;
				}
				
				line += "\n";
				
				if (line.startsWith(contentType)) {
					
					aux = line.split(contentType);
					aux = aux[1].split(" boundary=");
					String typeR = aux[0].replaceAll(";", "");
					if (aux[0].startsWith("multipart")) {
						String boundaryR = aux[1];
						parseMultipart(mimeParts, typeR, boundaryR, in, line);
					} else {
						part.addHeader(line);
						part.type = typeR;
					}
				} else if (line.startsWith(contentTransferEncoding)) {
					part.addHeader(line);
					aux = line.split(contentTransferEncoding);
					part.transferEncoding = aux[1].replaceAll(";", "");
				} else if (line.startsWith(contentDisposition)) {
					part.addHeader(line);
					aux = line.split(contentDisposition);
					part.contentDisposition = aux[1].replaceAll(";", "");
				} else if (line.startsWith(attachmentId)) {
					part.addHeader(line);
					aux = line.split(attachmentId);
					part.contentDisposition = aux[1].replaceAll(";", "");
				} else {
					part.body += line;
				}
				line = nextLine;
			} else {
				line = getNextLine(in);
			}
		}
	}

	private void parseMultipart(List<MimeMultiPart> mimeParts, String type,
			String boundary, InputStream in, String header) throws IOException {
		// parsing the multipart if there are multiparts
		String line;
		String nextLine;
		String aux[];
		MimeMultiPart multiPart = new MimeMultiPart();
		mimeParts.add(multiPart);
		multiPart.type = type;
		multiPart.addHeader(header);
		multiPart.boundary = boundary;
		MimePart part = new MimePart();
		line = getNextLine(in);
		line+="\n";
		if (!line.equals("--" + boundary)) {
			throw new RuntimeException();
		}
		while (line != null) {
			if (!line.isEmpty()) {

				while ((nextLine = getNextLine(in)) != null
						&& !nextLine.isEmpty() && nextLine.charAt(0) == ' ') {
					line = line + nextLine;
				}
				
				line += "\n";
				
				if (line.equals("--" + boundary + "--\n")) {
					part.addFooter(line);
					return;
				} else if (line.equals("--" + boundary)) {
					part = new MimePart();
					multiPart.parts.add(part);
					part.addHeader(line);
				} else {
					if (line.startsWith(contentType)) {
						aux = line.split(contentType);
						aux = aux[1].split(" boundary=");
						String typeR = aux[0].replaceAll(";", "");
						if (aux[0].startsWith("multipart")) {
							String boundaryR = aux[1];
							parseMultipart(mimeParts, typeR, boundaryR, in, line);
						} else {
							part.addHeader(line);
							part.type = typeR;
						}
					} else if (line.startsWith(contentTransferEncoding)) {
						part.addHeader(line);
						aux = line.split(contentTransferEncoding);
						part.transferEncoding = aux[1].replaceAll(";", "");
					} else if (line.startsWith(contentDisposition)) {
						part.addHeader(line);
						aux = line.split(contentDisposition);
						part.contentDisposition = aux[1].replaceAll(";", "");
					} else if (line.startsWith(attachmentId)) {
						part.addHeader(line);
						aux = line.split(attachmentId);
						part.contentDisposition = aux[1].replaceAll(";", "");
					} else {
						part.body += line + '\n';
					}
				}
				line = nextLine;
			} else {
				line = getNextLine(in);
			}
		}
	}

	private String getNextLine(InputStream in) throws IOException {
		int c;
		String line = "";
		if (EOFflag) {
			return null;
		}
		while (((char) (c = in.read())) != '\n') {
			if (c == EOF) {
				EOFflag = true;
				if (line.isEmpty()) {
					return null;
				}
				return line;
			}
			line += ((char) c);
		}
		return line;
	}

	public MimeInfoSimplified parseSimplified(InputStream in)
			throws IOException {
		EOFflag = false;
		String line;
		String nextLine;

		MimeInfoSimplified resp = new MimeInfoSimplified();
		String contentTypeType = "";
		String aux[];

		// parsing the mime header
		line = getNextLine(in);
		while (line != null) {
			if (!line.isEmpty()) {
				while ((nextLine = getNextLine(in)) != null
						&& nextLine.length() >= 1 && nextLine.charAt(0) == ' ') {
					line = line + nextLine;
				}
				resp.incSize(line.length());
				if (line.startsWith(version)) {
					resp.addHeader(line);
				} else if (line.startsWith(received)) {
					resp.addHeader(line);
				} else if (line.startsWith(date)) {
					resp.addHeader(line);
					String dateP = line.split("Date: ")[1];
					SimpleDateFormat sdf = new SimpleDateFormat(
							"EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
					Calendar c = Calendar.getInstance();
					try {
						c.setTime(sdf.parse(dateP));
					} catch (ParseException e) {
						System.out
								.println("Couldn't parse date, the email will be considered to have been sent today");
					}
					resp.setDate(c);
				} else if (line.startsWith(messageID)) {
					resp.addHeader(line);
				} else if (line.startsWith(subject)) {
					resp.addHeader(line);
				} else if (line.startsWith(from)) {
					resp.addHeader(line);
					aux = line.split(from);
					resp.setFrom(aux[1]);
				} else if (line.startsWith(to)) {
					resp.addHeader(line);
				} else if (line.startsWith(contentType)) {
					resp.addHeader(line);
					aux = line.split("Content-Type: ");
					aux = aux[1].split(" boundary=");
					contentTypeType = aux[0];
					if (aux[0].startsWith("multipart")) {
						String boundary = aux[1];
						parseMultipartSimplified(resp, contentTypeType,
								boundary, in);
					} else {
						parseNotMultipartSimplified(resp, contentTypeType, in);
					}
				} else {
					resp.addHeader(line);
				}
				line = nextLine;
			} else {
				line = getNextLine(in);
			}
		}
		return resp;
	}

	private void parseNotMultipartSimplified(MimeInfoSimplified resp,
			String type, InputStream in) throws IOException {
		String line;
		String nextLine;
		String aux[];
		boolean attachFlag = false;
		AttachmentType attach = null;
		String contentTypeType = "";

		line = getNextLine(in);

		while (line != null) {
			if (!line.isEmpty()) {

				while ((nextLine = getNextLine(in)) != null
						&& !nextLine.isEmpty() && nextLine.charAt(0) == ' ') {
					line = line + nextLine;
				}
				resp.incSize(line.length());

				if (line.startsWith(contentType)) {
					resp.addHeader(line);
					aux = line.split(contentType);
					contentTypeType = aux[0];
					aux = aux[1].split(" boundary=");
					if (aux[0].startsWith("multipart")) {
						String boundaryR = aux[1];
						parseMultipartSimplified(resp, contentTypeType,
								boundaryR, in);
					}
				} else if (line.startsWith(contentTransferEncoding)) {
					resp.addHeader(line);
				} else if (line.startsWith(contentDisposition)) {
					resp.addHeader(line);
					aux = line.split(contentDisposition);
					String aux2 = aux[1].replaceAll(";", "");
					if (aux2.startsWith("attachment")) {
						resp.addHeader(line);
						attach = new AttachmentType(contentTypeType, aux2);
						resp.pushAttachment(attach);
						attachFlag = true;
					}
				} else if (line.startsWith(attachmentId)) {
					resp.addHeader(line);
				} else {
					if (attachFlag) {
						attach.incSize(line.length());
					}
				}
				line = nextLine;
			} else {
				line = getNextLine(in);
			}
		}

	}

	private void parseMultipartSimplified(MimeInfoSimplified resp, String type,
			String boundary, InputStream in) throws IOException {
		// parsing the multipart if there are multiparts
		String line;
		String nextLine;
		String aux[];
		String contentTypeType = "";
		boolean attachFlag = false;
		AttachmentType attach = null;

		line = getNextLine(in);
//		if (!line.equals("--" + boundary)) {
//			throw new RuntimeException();
//		}
		while (line != null) {
			if (!line.isEmpty()) {

				while ((nextLine = getNextLine(in)) != null
						&& !nextLine.isEmpty() && nextLine.charAt(0) == ' ') {
					line = line + nextLine;
				}
				resp.incSize(line.length());
				if (line.equals("--" + boundary + "--")) {
					attachFlag = false;
					return;
				} else if (line.equals("--" + boundary)) {
					attachFlag = false;
				} else {
					if (line.startsWith(contentType)) {
						resp.addHeader(line);
						aux = line.split(contentType);
						contentTypeType = aux[1];
						if (aux[1].startsWith("multipart")) {
							aux = aux[1].split(" boundary=");
							String boundaryR = aux[1];
							parseMultipartSimplified(resp, contentTypeType,
									boundaryR, in);
						} else {
							// parseNotMultipartSimplified(resp,
							// contentTypeType, in);
						}
					} else if (line.startsWith(contentTransferEncoding)) {
						resp.addHeader(line);

					} else if (line.startsWith(contentDisposition)) {
						aux = line.split(contentDisposition);
						String aux2 = aux[1].replaceAll(";", "");
						if (aux2.startsWith("attachment")) {
							attach = new AttachmentType(contentTypeType, aux2);
							resp.pushAttachment(attach);
							attachFlag = true;
						}
						resp.addHeader(line);
					} else if (line.startsWith(attachmentId)) {
						resp.addHeader(line);
					} else {
						if (attachFlag) {
							attach.incSize(line.length());
						}
					}
				}
				line = nextLine;
			} else {
				line = getNextLine(in);
			}
		}
	}
}
